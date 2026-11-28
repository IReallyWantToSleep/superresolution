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

package io.homo.superresolution.common.debug;

import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.perf.FramePacingTrace;
import net.minecraft.client.Minecraft;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * Unattended frame-pacing capture for automated testing, enabled by the {@code SR_AUTO_TRACE}
 * environment variable, e.g. {@code SR_AUTO_TRACE=warmup=20,duration=10,exit=true}.
 *
 * <p>Warmup counts from the first frame with a loaded level. After recording for
 * {@code duration} seconds the CSV is exported like the ImGui "Stop & Export" button does,
 * and with {@code exit=true} the game quits once the export has finished.</p>
 *
 * <p>A still camera never leaves the state it settles in, so two keys disturb the run the way
 * play does: {@code turn=DEGREES} turns the player by that much over 0.3 s every
 * {@code every=SECONDS} (default 3), which changes the GPU load at once, and {@code hitch=MS}
 * stalls the render thread for that long at the same moments.</p>
 */
public final class AutoFramePacingTrace {
    private static final Settings SETTINGS = Settings.parse(System.getenv("SR_AUTO_TRACE"));
    private static State state = State.WAITING_FOR_LEVEL;
    private static long phaseStartedAtNanos;
    private static CompletableFuture<Path> export;
    private static Object framerateLimitTracker;
    private static Method reportInput;
    private static boolean inputReportUnavailable;
    private static long levelEnteredAtNanos;
    private static long lastFrameNanos;
    private static long disturbances;

    private AutoFramePacingTrace() {
    }

    /** Called from the render thread once per frame. */
    public static void onFrameEnd() {
        if (SETTINGS == null || state == State.DONE) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        keepOutOfAfk(minecraft);
        disturb(minecraft);
        long elapsedNanos = System.nanoTime() - phaseStartedAtNanos;
        switch (state) {
            case WAITING_FOR_LEVEL -> {
                if (minecraft.level == null || minecraft.player == null) {
                    return;
                }
                keepRenderingWhenUnfocused(minecraft);
                levelEnteredAtNanos = System.nanoTime();
                enterPhase(State.WARMUP);
                SuperResolution.LOGGER.info("Auto frame-pacing trace: level loaded, warming up for {}s",
                        SETTINGS.warmupSeconds());
            }
            case WARMUP -> {
                if (elapsedNanos < SETTINGS.warmupSeconds() * 1_000_000_000L) {
                    return;
                }
                if (!FramePacingTrace.INSTANCE.start()) {
                    SuperResolution.LOGGER.warn("Auto frame-pacing trace: a collection is already running");
                    finish(minecraft);
                    return;
                }
                enterPhase(State.RECORDING);
                SuperResolution.LOGGER.info("Auto frame-pacing trace: recording for {}s",
                        SETTINGS.durationSeconds());
            }
            case RECORDING -> {
                if (elapsedNanos < SETTINGS.durationSeconds() * 1_000_000_000L) {
                    return;
                }
                FramePacingTrace.Snapshot snapshot = FramePacingTrace.INSTANCE.stop();
                if (snapshot == null) {
                    SuperResolution.LOGGER.warn("Auto frame-pacing trace: the collection was stopped elsewhere");
                    finish(minecraft);
                    return;
                }
                export = FramePacingTrace.INSTANCE.exportAsync(snapshot);
                enterPhase(State.EXPORTING);
            }
            case EXPORTING -> {
                if (!export.isDone()) {
                    return;
                }
                try {
                    SuperResolution.LOGGER.info("Auto frame-pacing trace exported: {}", export.join());
                } catch (Throwable throwable) {
                    SuperResolution.LOGGER.warn("Auto frame-pacing trace export failed", throwable);
                }
                finish(minecraft);
            }
            default -> {
            }
        }
    }

    private static void enterPhase(State next) {
        state = next;
        phaseStartedAtNanos = System.nanoTime();
    }

    private static void finish(Minecraft minecraft) {
        state = State.DONE;
        if (SETTINGS.exit()) {
            SuperResolution.LOGGER.info("Auto frame-pacing trace: quitting the game");
            minecraft.stop();
        }
    }

    // A launched test window rarely has focus; without this the pause menu replaces the
    // scene being measured. Reflective because the field is not guaranteed on every version.
    private static void keepRenderingWhenUnfocused(Minecraft minecraft) {
        try {
            minecraft.options.getClass().getField("pauseOnLostFocus").setBoolean(minecraft.options, false);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            SuperResolution.LOGGER.info("Auto frame-pacing trace: cannot disable pause-on-lost-focus: {}",
                    String.valueOf(exception));
        }
    }

    // Minecraft 1.21.2+ drops to 30 fps after 60 s without input, which an unattended run
    // never sends. Reflective because the tracker does not exist on older versions.
    private static void keepOutOfAfk(Minecraft minecraft) {
        if (inputReportUnavailable) {
            return;
        }
        try {
            if (reportInput == null) {
                framerateLimitTracker = minecraft.getClass().getMethod("getFramerateLimitTracker").invoke(minecraft);
                reportInput = framerateLimitTracker.getClass().getMethod("onInputReceived");
            }
            reportInput.invoke(framerateLimitTracker);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            inputReportUnavailable = true;
        }
    }

    private static final long TURN_NANOS = 300_000_000L;

    // Turns the player and stalls the render thread on a fixed schedule from the level's first
    // frame, so that runs compare.
    private static void disturb(Minecraft minecraft) {
        if (levelEnteredAtNanos == 0L || minecraft.player == null
                || (SETTINGS.turnDegrees() == 0.0 && SETTINGS.hitchMillis() == 0L)) {
            return;
        }
        long now = System.nanoTime();
        long previous = lastFrameNanos;
        lastFrameNanos = now;
        long periodNanos = SETTINGS.everySeconds() * 1_000_000_000L;
        long sinceLevel = now - levelEnteredAtNanos;
        long phase = sinceLevel % periodNanos;
        if (SETTINGS.turnDegrees() != 0.0 && previous != 0L && phase < TURN_NANOS) {
            double share = Math.min(now - previous, TURN_NANOS) / (double) TURN_NANOS;
            // Entity.turn scales its arguments by 0.15.
            minecraft.player.turn(SETTINGS.turnDegrees() * share / 0.15, 0.0);
        }
        long index = sinceLevel / periodNanos;
        if (SETTINGS.hitchMillis() > 0L && index > disturbances) {
            disturbances = index;
            try {
                Thread.sleep(SETTINGS.hitchMillis());
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private enum State {
        WAITING_FOR_LEVEL,
        WARMUP,
        RECORDING,
        EXPORTING,
        DONE
    }

    private record Settings(
            long warmupSeconds, long durationSeconds, boolean exit,
            double turnDegrees, long hitchMillis, long everySeconds
    ) {
        static Settings parse(String value) {
            if (value == null || value.isBlank()) {
                return null;
            }
            long warmup = 20L;
            long duration = 10L;
            boolean exit = false;
            double turn = 0.0;
            long hitch = 0L;
            long every = 3L;
            for (String entry : value.split(",")) {
                String[] pair = entry.trim().split("=", 2);
                String key = pair[0].trim();
                String setting = pair.length > 1 ? pair[1].trim() : "true";
                switch (key) {
                    case "warmup" -> warmup = Long.parseLong(setting);
                    case "duration" -> duration = Long.parseLong(setting);
                    case "exit" -> exit = Boolean.parseBoolean(setting);
                    case "turn" -> turn = Double.parseDouble(setting);
                    case "hitch" -> hitch = Long.parseLong(setting);
                    case "every" -> every = Math.max(1L, Long.parseLong(setting));
                    default -> SuperResolution.LOGGER.warn("Ignoring unknown SR_AUTO_TRACE key '{}'", key);
                }
            }
            SuperResolution.LOGGER.info(
                    "Auto frame-pacing trace enabled: warmup={}s duration={}s exit={} turn={} hitch={}ms every={}s",
                    warmup, duration, exit, turn, hitch, every);
            return new Settings(warmup, duration, exit, turn, hitch, every);
        }
    }
}
