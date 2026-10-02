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

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

final class FramePacingCsvWriter {
    private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter
            .ofPattern("yyyyMMdd_HHmmss_SSS", Locale.ROOT)
            .withZone(ZoneOffset.UTC);
    private static final AtomicInteger FILE_COUNTER = new AtomicInteger();

    private FramePacingCsvWriter() {
    }

    static Path write(FramePacingTrace.Snapshot snapshot, Path reportsDirectory) {
        if (snapshot == null) {
            throw new IllegalArgumentException("snapshot cannot be null");
        }
        if (reportsDirectory == null) {
            throw new IllegalArgumentException("reportsDirectory cannot be null");
        }

        try {
            Files.createDirectories(reportsDirectory);
            for (int attempt = 0; attempt < 1000; attempt++) {
                int counter = FILE_COUNTER.getAndIncrement();
                Path output = reportsDirectory.resolve(
                        "frame_pacing_"
                                + FILE_TIMESTAMP.format(Instant.ofEpochMilli(snapshot.captureStartEpochMillis()))
                                + "_"
                                + counter
                                + ".csv"
                );
                try {
                    writeNewFile(snapshot, output);
                    return output;
                } catch (FileAlreadyExistsException ignored) {
                    // Another export selected the same timestamp/counter pair.
                }
            }
            throw new IOException("Unable to allocate a unique frame-pacing report filename");
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Failed to write frame-pacing report to " + reportsDirectory,
                    exception
            );
        }
    }

    private static void writeNewFile(FramePacingTrace.Snapshot snapshot, Path output)
            throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(
                output,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE
        )) {
            writeRow(
                    writer,
                    "sequence",
                    "logical_frame",
                    "real_frame",
                    "batch_id",
                    "display_index",
                    "present_id",
                    "frame_kind",
                    "provider",
                    "thread_name",
                    "event_id",
                    "event",
                    "start_nanos",
                    "end_nanos",
                    "duration_nanos",
                    "status",
                    "details"
            );
            for (FramePacingTrace.Event event : snapshot.events()) {
                writeRow(
                        writer,
                        Long.toString(event.sequence()),
                        blankIfNegative(event.logicalFrame()),
                        blankIfNegative(event.realFrame()),
                        blankIfNegative(event.batchId()),
                        blankIfNegative(event.displayIndex()),
                        blankIfNegative(event.presentId()),
                        event.frameKind(),
                        event.provider(),
                        event.threadName(),
                        event.eventId().name(),
                        event.event(),
                        Long.toString(event.startNanos()),
                        Long.toString(event.endNanos()),
                        Long.toString(event.durationNanos()),
                        event.status(),
                        event.details()
                );
            }
        }
    }

    private static String blankIfNegative(long value) {
        return value < 0L ? "" : Long.toString(value);
    }

    private static void writeRow(BufferedWriter writer, String... values) throws IOException {
        for (int index = 0; index < values.length; index++) {
            if (index > 0) {
                writer.write(',');
            }
            writer.write(escape(values[index]));
        }
        writer.newLine();
    }

    private static String escape(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        String normalized = value.replace('\r', ' ').replace('\n', ' ');
        if (normalized.indexOf(',') < 0
                && normalized.indexOf('"') < 0
                && normalized.indexOf(' ') < 0) {
            return normalized;
        }
        return '"' + normalized.replace("\"", "\"\"") + '"';
    }
}
