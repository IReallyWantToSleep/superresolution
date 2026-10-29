/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.homo.superresolution.common.perf;

import io.homo.superresolution.core.SuperResolutionConstants;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.LongSupplier;

public final class FramePacingTrace {
    public static final FramePacingTrace INSTANCE =
            new FramePacingTrace(System::nanoTime, System::currentTimeMillis);

    private static final Span NO_OP_SPAN = new Span(null, null, -1L);

    private final Object stateMonitor = new Object();
    private final LongSupplier clock;
    private final LongSupplier wallClockMillis;
    private volatile Recording recording;
    private volatile CompletableFuture<Path> exportFuture;
    private volatile Path lastExportPath;
    private volatile String lastExportError;
    private volatile long lastSnapshotFrameCount;
    private volatile long lastSnapshotEventCount;

    FramePacingTrace(LongSupplier clock, LongSupplier wallClockMillis) {
        if (clock == null || wallClockMillis == null) {
            throw new IllegalArgumentException("clock and wallClockMillis cannot be null");
        }
        this.clock = clock;
        this.wallClockMillis = wallClockMillis;
    }

    public boolean start() {
        refreshExportState();
        synchronized (stateMonitor) {
            if (recording != null || isExportingLocked()) {
                return false;
            }

            Recording next = new Recording(clock.getAsLong(), wallClockMillis.getAsLong());
            recording = next;
            next.addInstantLocked(
                    "trace_start",
                    Context.empty(),
                    "complete",
                    "capture_start_epoch_millis=" + next.captureStartEpochMillis
            );
            return true;
        }
    }

    /**
     * Stops the active recording and returns an immutable snapshot.
     *
     * <p>Open spans are closed at the stop timestamp with {@code incomplete} status.
     * Spans that race with stop after the recording has been frozen are ignored.</p>
     */
    public Snapshot stop() {
        Recording current = recording;
        if (current == null) {
            return null;
        }

        synchronized (stateMonitor) {
            if (recording != current) {
                return null;
            }
            synchronized (current.monitor) {
                long stopNanos = clock.getAsLong();
                current.stopping = true;
                recording = null;
                current.closeOpenSpansLocked(stopNanos);
                current.addInstantLocked("trace_stop", Context.empty(), "complete", "");
                Snapshot snapshot = current.snapshotLocked(stopNanos);
                lastSnapshotFrameCount = snapshot.frameCount();
                lastSnapshotEventCount = snapshot.events().size();
                return snapshot;
            }
        }
    }

    public CompletableFuture<Path> exportAsync(Snapshot snapshot) {
        if (snapshot == null) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("snapshot cannot be null")
            );
        }

        refreshExportState();
        synchronized (stateMonitor) {
            if (isExportingLocked()) {
                return exportFuture;
            }
            lastExportError = null;
            CompletableFuture<Path> future = CompletableFuture.supplyAsync(() ->
                    FramePacingCsvWriter.write(
                            snapshot,
                            SuperResolutionConstants.DATA_DIR.getPath().resolve("reports")
                    )
            );
            exportFuture = future;
            return future;
        }
    }

    public Span begin(String event, Context context) {
        if (event == null || event.isBlank()) {
            return NO_OP_SPAN;
        }
        Recording current = recording;
        if (current == null) {
            return NO_OP_SPAN;
        }
        return current.begin(
                event,
                context == null ? Context.empty() : context,
                clock.getAsLong()
        );
    }

    /**
     * Primitive-field overload used by hot lifecycle paths. It avoids constructing a
     * Context object when collection is inactive.
     */
    public Span begin(
            String event,
            int logicalFrame,
            long realFrame,
            long batchId,
            long displayIndex,
            long presentId,
            String frameKind,
            String provider
    ) {
        if (event == null || event.isBlank() || recording == null) {
            return NO_OP_SPAN;
        }
        return begin(
                event,
                new Context(
                        logicalFrame,
                        realFrame,
                        batchId,
                        displayIndex,
                        presentId,
                        frameKind,
                        provider
                )
        );
    }

    public long nowNanos() {
        return clock.getAsLong();
    }

    public void instant(String event, Context context, String status, String details) {
        instantAt(event, context, clock.getAsLong(), status, details);
    }

    public void instantAt(
            String event,
            Context context,
            long timestampNanos,
            String status,
            String details
    ) {
        if (event == null || event.isBlank()) {
            return;
        }
        Recording current = recording;
        if (current == null) {
            return;
        }
        current.addInstant(
                event,
                context == null ? Context.empty() : context,
                timestampNanos,
                status,
                details
        );
    }

    public Status status() {
        refreshExportState();
        Recording current = recording;
        if (current != null) {
            RecordingStats stats = current.stats();
            return new Status(
                    State.RECORDING,
                    stats.frameCount(),
                    stats.eventCount(),
                    lastExportPath,
                    lastExportError
            );
        }

        CompletableFuture<Path> future = exportFuture;
        if (future != null && !future.isDone()) {
            return new Status(
                    State.EXPORTING,
                    lastSnapshotFrameCount,
                    lastSnapshotEventCount,
                    lastExportPath,
                    lastExportError
            );
        }
        return new Status(
                State.IDLE,
                lastSnapshotFrameCount,
                lastSnapshotEventCount,
                lastExportPath,
                lastExportError
        );
    }

    private void refreshExportState() {
        CompletableFuture<Path> future = exportFuture;
        if (future == null || !future.isDone()) {
            return;
        }

        synchronized (stateMonitor) {
            if (exportFuture != future || !future.isDone()) {
                return;
            }
            try {
                lastExportPath = future.join();
                lastExportError = null;
            } catch (CompletionException exception) {
                Throwable cause = exception.getCause() == null
                        ? exception
                        : exception.getCause();
                lastExportError = cause.getMessage() == null
                        ? cause.getClass().getSimpleName()
                        : cause.getMessage();
            } catch (RuntimeException exception) {
                lastExportError = exception.getMessage() == null
                        ? exception.getClass().getSimpleName()
                        : exception.getMessage();
            } finally {
                exportFuture = null;
            }
        }
    }

    private boolean isExportingLocked() {
        return exportFuture != null && !exportFuture.isDone();
    }

    public enum State {
        IDLE,
        RECORDING,
        EXPORTING
    }

    public record Status(
            State state,
            long frameCount,
            long eventCount,
            Path lastExportPath,
            String lastExportError
    ) {
    }

    public record Context(
            int logicalFrame,
            long realFrame,
            long batchId,
            long displayIndex,
            long presentId,
            String frameKind,
            String provider
    ) {
        public Context {
            frameKind = frameKind == null ? "" : frameKind;
            provider = provider == null ? "" : provider;
        }

        public static Context empty() {
            return new Context(-1, -1L, -1L, -1L, -1L, "", "");
        }

        public static Context logicalFrame(int logicalFrame) {
            return new Context(logicalFrame, -1L, -1L, -1L, -1L, "", "");
        }
    }

    public record Event(
            long sequence,
            int logicalFrame,
            long realFrame,
            long batchId,
            long displayIndex,
            long presentId,
            String frameKind,
            String provider,
            String threadName,
            FramePacingEvent eventId,
            String event,
            long startNanos,
            long endNanos,
            long durationNanos,
            String status,
            String details
    ) {
        public Event {
            eventId = eventId == null ? FramePacingEvent.CUSTOM : eventId;
            event = event == null ? "" : event;
        }

        /**
         * Keeps the pre-event-id constructor usable for programmatic snapshots.
         */
        public Event(
                long sequence,
                int logicalFrame,
                long realFrame,
                long batchId,
                long displayIndex,
                long presentId,
                String frameKind,
                String provider,
                String threadName,
                String event,
                long startNanos,
                long endNanos,
                long durationNanos,
                String status,
                String details
        ) {
            this(
                    sequence,
                    logicalFrame,
                    realFrame,
                    batchId,
                    displayIndex,
                    presentId,
                    frameKind,
                    provider,
                    threadName,
                    FramePacingEvent.fromEventName(event),
                    event,
                    startNanos,
                    endNanos,
                    durationNanos,
                    status,
                    details
            );
        }
    }

    public record Snapshot(
            long captureStartEpochMillis,
            long durationNanos,
            long frameCount,
            List<Event> events
    ) {
        public Snapshot {
            events = List.copyOf(events);
        }
    }

    public static final class Span implements AutoCloseable {
        private final FramePacingTrace owner;
        private final Recording recording;
        private final long spanId;
        private final AtomicBoolean closed = new AtomicBoolean();

        private Span(FramePacingTrace owner, Recording recording, long spanId) {
            this.owner = owner;
            this.recording = recording;
            this.spanId = spanId;
        }

        public void complete(String status, String details) {
            if (owner == null || recording == null || !closed.compareAndSet(false, true)) {
                return;
            }
            owner.end(recording, spanId, status, details);
        }

        @Override
        public void close() {
            complete("complete", "");
        }
    }

    private void end(Recording target, long spanId, String status, String details) {
        if (recording != target) {
            return;
        }
        target.end(
                spanId,
                clock.getAsLong(),
                normalizeStatus(status),
                normalizeDetails(details)
        );
    }

    private static String normalizeStatus(String status) {
        return status == null || status.isBlank() ? "complete" : status;
    }

    private static String normalizeDetails(String details) {
        return details == null ? "" : details;
    }

    private final class Recording {
        private final Object monitor = new Object();
        private final long originNanos;
        private final long captureStartEpochMillis;
        private final List<Event> events = new ArrayList<>();
        private final Map<Long, OpenSpan> openSpans = new HashMap<>();
        private final Set<Integer> logicalFrames = new HashSet<>();
        private long nextSequence;
        private long nextSpanId;
        private boolean stopping;

        private Recording(long originNanos, long captureStartEpochMillis) {
            this.originNanos = originNanos;
            this.captureStartEpochMillis = captureStartEpochMillis;
        }

        private Span begin(String event, Context context, long startNanos) {
            synchronized (monitor) {
                if (stopping) {
                    return NO_OP_SPAN;
                }
                long sequence = ++nextSequence;
                long spanId = ++nextSpanId;
                OpenSpan span = new OpenSpan(
                        spanId,
                        sequence,
                        context,
                        Thread.currentThread().getName(),
                        FramePacingEvent.fromEventName(event),
                        event,
                        relativeNanos(startNanos)
                );
                openSpans.put(spanId, span);
                addLogicalFrameLocked(context);
                return new Span(FramePacingTrace.this, this, spanId);
            }
        }

        private void addInstant(
                String event,
                Context context,
                long timestampNanos,
                String status,
                String details
        ) {
            synchronized (monitor) {
                if (stopping) {
                    return;
                }
                addInstantLocked(event, context, status, details, timestampNanos);
            }
        }

        private void addInstantLocked(
                String event,
                Context context,
                String status,
                String details
        ) {
            addInstantLocked(event, context, status, details, originNanos);
        }

        private void addInstantLocked(
                String event,
                Context context,
                String status,
                String details,
                long timestampNanos
        ) {
            long timestamp = relativeNanos(timestampNanos);
            events.add(new Event(
                    ++nextSequence,
                    context.logicalFrame(),
                    context.realFrame(),
                    context.batchId(),
                    context.displayIndex(),
                    context.presentId(),
                    context.frameKind(),
                    context.provider(),
                    Thread.currentThread().getName(),
                    FramePacingEvent.fromEventName(event),
                    event,
                    timestamp,
                    timestamp,
                    0L,
                    normalizeStatus(status),
                    normalizeDetails(details)
            ));
            addLogicalFrameLocked(context);
        }

        private void end(long spanId, long endNanos, String status, String details) {
            synchronized (monitor) {
                OpenSpan span = openSpans.remove(spanId);
                if (span == null || stopping) {
                    return;
                }
                addEventLocked(span, relativeNanos(endNanos), status, details);
            }
        }

        private void closeOpenSpansLocked(long stopNanos) {
            long relativeStop = relativeNanos(stopNanos);
            for (OpenSpan span : openSpans.values()) {
                addEventLocked(
                        span,
                        relativeStop,
                        "incomplete",
                        "stopped_before_end"
                );
            }
            openSpans.clear();
        }

        private void addEventLocked(
                OpenSpan span,
                long endNanos,
                String status,
                String details
        ) {
            long duration = Math.max(0L, endNanos - span.startNanos);
            events.add(new Event(
                    span.sequence,
                    span.context.logicalFrame(),
                    span.context.realFrame(),
                    span.context.batchId(),
                    span.context.displayIndex(),
                    span.context.presentId(),
                    span.context.frameKind(),
                    span.context.provider(),
                    span.threadName,
                    span.eventId,
                    span.event,
                    span.startNanos,
                    endNanos,
                    duration,
                    normalizeStatus(status),
                    normalizeDetails(details)
            ));
        }

        private Snapshot snapshotLocked(long stopNanos) {
            List<Event> sorted = new ArrayList<>(events);
            sorted.sort(Comparator
                    .comparingLong(Event::startNanos)
                    .thenComparingLong(Event::sequence));
            return new Snapshot(
                    captureStartEpochMillis,
                    relativeNanos(stopNanos),
                    logicalFrames.size(),
                    sorted
            );
        }

        private RecordingStats stats() {
            synchronized (monitor) {
                return new RecordingStats(
                        logicalFrames.size(),
                        events.size() + openSpans.size()
                );
            }
        }

        private void addLogicalFrameLocked(Context context) {
            if (context.logicalFrame() >= 0) {
                logicalFrames.add(context.logicalFrame());
            }
        }

        private long relativeNanos(long timestampNanos) {
            return Math.max(0L, timestampNanos - originNanos);
        }
    }

    private record RecordingStats(long frameCount, long eventCount) {
    }

    private static final class OpenSpan {
        private final long spanId;
        private final long sequence;
        private final Context context;
        private final String threadName;
        private final FramePacingEvent eventId;
        private final String event;
        private final long startNanos;

        private OpenSpan(
                long spanId,
                long sequence,
                Context context,
                String threadName,
                FramePacingEvent eventId,
                String event,
                long startNanos
        ) {
            this.spanId = spanId;
            this.sequence = sequence;
            this.context = context;
            this.threadName = threadName;
            this.eventId = eventId;
            this.event = event;
            this.startNanos = startNanos;
        }
    }
}
