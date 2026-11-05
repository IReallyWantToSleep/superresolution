#!/usr/bin/env python3
"""Summarize a Super Resolution frame-pacing trace CSV (application-managed FG presentation).

Usage: analyze_frame_pacing.py <trace.csv | reports-dir>
A directory argument picks the newest frame_pacing_*.csv inside it.

Present cadence is measured at the moment vkQueuePresentKHR returned (present_call end),
or at first-pixel-visible when the trace carries VK_EXT_present_timing results.
"""
import bisect
import csv
import statistics
import sys
from collections import Counter, defaultdict
from pathlib import Path


def pick_trace(arg):
    path = Path(arg)
    if path.is_dir():
        traces = sorted(path.glob("frame_pacing_*.csv"), key=lambda p: p.stat().st_mtime)
        if not traces:
            sys.exit(f"no frame_pacing_*.csv in {path}")
        return traces[-1]
    return path


def ms(nanos):
    return nanos / 1e6


def describe(values):
    if not values:
        return "n=0"
    ordered = sorted(values)

    def pct(p):
        return ordered[min(len(ordered) - 1, int(p / 100 * (len(ordered) - 1)))]

    return (f"p1={pct(1):6.2f} p50={pct(50):6.2f} p90={pct(90):6.2f} p99={pct(99):6.2f} "
            f"max={ordered[-1]:6.2f} mean={statistics.mean(ordered):6.2f} "
            f"sd={statistics.pstdev(ordered):5.2f} n={len(ordered)}")


def main():
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    trace = pick_trace(sys.argv[1])
    rows = list(csv.DictReader(open(trace, newline="")))
    by_event = defaultdict(list)
    for row in rows:
        by_event[row["event"]].append(row)

    def durations(event, kind=None):
        return [ms(int(r["duration_nanos"])) for r in by_event[event]
                if kind is None or r["frame_kind"] == kind]

    print(f"trace: {trace}")
    calls = sorted(by_event["present_call"], key=lambda r: int(r["display_index"] or -1))
    if not calls:
        sys.exit("no present_call events: was application-managed presentation active?")
    starts = [int(r["start_nanos"]) for r in calls]
    ends = [int(r["end_nanos"]) for r in calls]
    kinds = [r["frame_kind"] for r in calls]
    renders = sorted(by_event["real_frame_rendering"], key=lambda r: int(r["start_nanos"]))
    seconds = (max(ends) - min(starts)) / 1e9
    batches = Counter(r["batch_id"] for r in calls)
    multiplier = Counter(batches.values()).most_common(1)[0][0]
    print(f"capture {seconds:.2f}s  presents/s {len(calls) / seconds:.1f}  "
          f"real fps {len(renders) / seconds:.1f}  dominant images/batch {multiplier}")

    statuses = Counter((r["event"], r["status"]) for r in rows if r["status"] not in ("complete", ""))
    if statuses:
        print("non-complete statuses:", dict(statuses))

    print("\n-- present thread (ms) --")
    for event in ("present_gpu_ready_wait", "present_pacing_wait", "present_call"):
        for kind in ("GENERATED", "REAL"):
            values = durations(event, kind)
            if values:
                print(f"{event:24s} {kind:9s} {describe(values)}")

    visible = {}
    for r in by_event["present_image_first_pixel_visible"] + by_event["present_image_first_pixel_out"]:
        if r["display_index"]:
            visible[int(r["display_index"])] = int(r["start_nanos"])
    if len(visible) > len(calls) // 2:
        source = "first-pixel timestamps"
        stamps = [visible.get(int(r["display_index"])) for r in calls]
        pairs = [(s, k) for s, k in zip(stamps, kinds) if s is not None]
        stamps, kinds = [p[0] for p in pairs], [p[1] for p in pairs]
    else:
        source = "present_call return"
        stamps = ends

    gaps = [ms(stamps[i + 1] - stamps[i]) for i in range(len(stamps) - 1)]
    print(f"\n-- output cadence from {source} (ms) --")
    print(f"all gaps                 {describe(gaps)}")
    transitions = defaultdict(list)
    for i in range(len(stamps) - 1):
        transitions[f"{kinds[i][:3]}->{kinds[i + 1][:3]}"].append(gaps[i])
    for name, values in sorted(transitions.items()):
        print(f"{name:24s} {describe(values)}")
    if gaps:
        ideal = statistics.mean(gaps)
        bunched = sum(1 for g in gaps if g < 0.5 * ideal)
        print(f"mean interval {ideal:.2f} ms; gaps < 50% of mean: {bunched}/{len(gaps)} "
              f"({100 * bunched / len(gaps):.0f}%); cv {statistics.pstdev(gaps) / ideal:.2f}")

    print("\n-- render thread (ms) --")
    render_ends = [int(r["end_nanos"]) for r in renders]
    print(f"real period (render end) {describe([ms(b - a) for a, b in zip(render_ends, render_ends[1:])])}")
    print(f"real_frame_rendering     {describe(durations('real_frame_rendering'))}")
    print(f"Reflex Sleep             {describe(durations('Reflex Sleep'))}")
    windows = []
    periods = [ms(b - a) for a, b in zip(render_ends, render_ends[1:])]
    for start in range(0, len(periods) - 39, 40):
        window = sorted(periods[start:start + 40])
        windows.append(f"{statistics.mean(window):.1f}/{window[36]:.1f}")
    print("real period per 40 frames (mean/p90):", " ".join(windows))

    # Render-thread stalls (Frame start -> Main Render, last capture flip -> enqueue) that
    # end within 0.2 ms of a present returning: the signature of render/present lock coupling.
    present_returns = sorted(ends)
    render_rows = [r for r in rows if r["thread_name"] == "Render thread"]
    first = {}
    last_flip = {}
    for r in render_rows:
        frame = r["logical_frame"]
        start, end = int(r["start_nanos"]), int(r["end_nanos"])
        if r["event"] in ("Frame", "Main Render", "presentation_enqueue"):
            first[(frame, r["event"])] = start
        if r["event"] == "GL Capture Flip":
            last_flip[frame] = max(last_flip.get(frame, 0), end)

    def ends_at_present(t):
        i = bisect.bisect_right(present_returns, t) - 1
        return i >= 0 and t - present_returns[i] < 200_000

    coupled = 0
    coupled_nanos = 0
    stalls = 0
    for (frame, event), start in first.items():
        if event == "Main Render" and (frame, "Frame") in first:
            begin = first[(frame, "Frame")]
        elif event == "presentation_enqueue" and frame in last_flip:
            begin = last_flip[frame]
        else:
            continue
        if start - begin > 2_000_000:
            stalls += 1
            if ends_at_present(start):
                coupled += 1
                coupled_nanos += start - begin
    span = max(int(r["end_nanos"]) for r in render_rows) - min(int(r["start_nanos"]) for r in render_rows)
    print(f"render stalls >2ms: {stalls}, released by a present return: {coupled} "
          f"({100 * coupled_nanos / span:.0f}% of render-thread time)")

    reflex_section(rows, by_event)


def reflex_section(rows, by_event):
    """Driver-side view from vkGetLatencyTimingsNV, when the trace carries it."""
    reports = {}
    for r in by_event["reflex_latency_report"]:
        fields = dict(item.split("=", 1) for item in r["details"].split(",") if "=" in item)
        reports[int(r["present_id"])] = {k: int(v) for k, v in fields.items()}
    if not reports:
        return
    ordered = [reports[k] for k in sorted(reports)]
    print(f"\n-- Reflex driver reports (ms), {len(ordered)} frames --")
    zero_gpu = sum(1 for f in ordered if f["gpu_start"] == 0 or f["gpu_end"] == 0)
    print(f"frames without GPU render timing: {zero_gpu}/{len(ordered)}")

    def spans(a, b):
        return [(f[b] - f[a]) / 1000 for f in ordered if f[a] and f[b]]

    for label, a, b in (("sim (sim_start->sim_end)", "sim_start", "sim_end"),
                        ("sim_start->submit_end", "sim_start", "submit_end"),
                        ("submit_end->present_start", "submit_end", "present_start"),
                        ("present (start->end)", "present_start", "present_end"),
                        ("driver (start->end)", "driver_start", "driver_end"),
                        ("os queue (start->end)", "os_queue_start", "os_queue_end"),
                        ("gpu render (start->end)", "gpu_start", "gpu_end"),
                        ("submit_end->gpu_end", "submit_end", "gpu_end"),
                        ("sim_start->gpu_end", "sim_start", "gpu_end")):
        values = spans(a, b)
        if values:
            print(f"{label:26s} {describe(values)}")
    starts = [f["sim_start"] for f in ordered if f["sim_start"]]
    print(f"{'sim_start period':26s} {describe([(b - a) / 1000 for a, b in zip(starts, starts[1:])])}")

    # Clock check: the driver's sim_start should sit a fixed offset from the end of our
    # Reflex Sleep span for the same frame (present id -> logical frame via the enqueue rows).
    frame_of_present = {int(r["present_id"]): r["logical_frame"]
                        for r in by_event["frame_generation_enqueue_wait"] if r["present_id"] not in ("", "-1")}
    sleep_end = {r["logical_frame"]: int(r["end_nanos"]) for r in by_event["Reflex Sleep"]}
    offsets = []
    for present_id, frame in frame_of_present.items():
        report = reports.get(present_id)
        if report and report["sim_start"] and frame in sleep_end:
            offsets.append(report["sim_start"] * 1000 - sleep_end[frame])
    if len(offsets) > 10:
        offsets.sort()
        spread = (offsets[int(0.9 * len(offsets))] - offsets[int(0.1 * len(offsets))]) / 1e6
        print(f"driver clock vs trace: sim_start-minus-sleep-end p10..p90 spread {spread:.2f} ms over "
              f"{len(offsets)} frames ({'aligned' if spread < 1 else 'NOT aligned'})")


if __name__ == "__main__":
    main()
