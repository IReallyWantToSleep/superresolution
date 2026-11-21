#!/usr/bin/env bash
# Unattended performance run of the 26.3 Vulkan dev client.
#
# Launches this checkout's Fabric dev client (runs/fabric-iris-vulkan) straight into a world with
# SR_AUTO_TRACE, so the game warms up, records a frame-pacing trace, exports it and quits by itself.
# The trace is copied to LOG_DIR/<label>.csv and summarized with analyze_frame_pacing.py.
# Prepare runs/fabric-iris-vulkan first (world, options.txt, configs, extra mods, shaderpack).
#
# Usage: script/vk_autotest.sh [--label NAME] [--warmup SECONDS] [--duration SECONDS] [--world NAME]
#                              [--size WIDTHxHEIGHT]
# Environment: TIMEOUT (seconds for the whole game run, default 900), LOG_DIR, GRADLE_OPTS_EXTRA.
set -euo pipefail

SR_DIR=$(cd "$(dirname "$0")/.." && pwd)
RUN_DIR="$SR_DIR/runs/fabric-iris-vulkan"
TIMEOUT=${TIMEOUT:-900}
LOG_DIR=${LOG_DIR:-/tmp/sr-vk-autotest}
LABEL=run
WORLD="New World"
WARMUP=40
DURATION=30
SIZE=2560x1380

while [ $# -gt 0 ]; do
    case "$1" in
        --label) LABEL=$2; shift ;;
        --warmup) WARMUP=$2; shift ;;
        --duration) DURATION=$2; shift ;;
        --world) WORLD=$2; shift ;;
        --size) SIZE=$2; shift ;;
        *) echo "unknown argument: $1" >&2; exit 2 ;;
    esac
    shift
done

REPORTS="$RUN_DIR/config/super_resolution/reports"
# Loom passes the client's JVM arguments through this file, so it identifies a dev-run game.
CLIENT_PATTERN="$SR_DIR/modules/fabric/build/loom-cache/argFiles/runClient"
mkdir -p "$LOG_DIR"

if pgrep -f "$CLIENT_PATTERN" > /dev/null; then
    echo "[vk-autotest] a dev client from this checkout is already running; close it first" >&2
    exit 1
fi
if [ ! -d "$RUN_DIR/saves/$WORLD" ]; then
    echo "[vk-autotest] world '$WORLD' is missing from $RUN_DIR/saves" >&2
    exit 1
fi

marker="$LOG_DIR/.run-start"
touch "$marker"
echo "[vk-autotest] $LABEL: launching '$WORLD' at $SIZE (warmup ${WARMUP}s, record ${DURATION}s)"
status=0
# shellcheck disable=SC2086
SR_AUTO_TRACE="warmup=$WARMUP,duration=$DURATION,exit=true" \
    timeout --kill-after=30 "$TIMEOUT" \
    "$SR_DIR/gradlew" -p "$SR_DIR" :fabric:runClient -x :native:build ${GRADLE_OPTS_EXTRA:-} \
    --args="--quickPlaySingleplayer \"$WORLD\" --width ${SIZE%x*} --height ${SIZE#*x}" \
    > "$LOG_DIR/$LABEL.run.log" 2>&1 || status=$?

# The game JVM is a child of the Gradle process, so a timed-out client can leave it running.
if [ "$status" = 124 ] || [ "$status" = 137 ]; then
    pkill -f "$CLIENT_PATTERN" || true
    echo "[vk-autotest] run timed out after ${TIMEOUT}s" >&2
fi

grep -E "Auto frame-pacing trace|BUILD FAILED|error:|Exception in|FATAL" "$LOG_DIR/$LABEL.run.log" | tail -20 || true

trace=$(find "$REPORTS" -name 'frame_pacing_*.csv' -newer "$marker" 2>/dev/null | sort | tail -1)
if [ -z "$trace" ]; then
    echo "[vk-autotest] no trace was produced (exit status $status), run log: $LOG_DIR/$LABEL.run.log" >&2
    exit 1
fi
cp -p "$trace" "$LOG_DIR/$LABEL.csv"
echo "[vk-autotest] trace: $LOG_DIR/$LABEL.csv"
python3 -I "$SR_DIR/script/analyze_frame_pacing.py" "$LOG_DIR/$LABEL.csv" || true
