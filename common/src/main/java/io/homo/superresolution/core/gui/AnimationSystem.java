/*
 * Super Resolution
 * Copyright (c) 2025. 187J3X1-114514
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

package io.homo.superresolution.core.gui;


import io.homo.superresolution.thirdparty.icyllis.modernui.animation.AnimationHandler;
import io.homo.superresolution.thirdparty.icyllis.modernui.core.Core;
import io.homo.superresolution.thirdparty.icyllis.modernui.core.Looper;

import java.util.concurrent.locks.LockSupport;

public class AnimationSystem {
    private static volatile Thread animationThread;
    private static volatile boolean isInitialized = false;
    private static volatile boolean shouldStop = false;

    public static synchronized void initialize() {
        if (isInitialized) {
            return;
        }

        shouldStop = false;
        long frameTimeNanos = (long) ((1_000_000_000L / 120.0));

        animationThread = new Thread(() -> {
            try {
                while (!shouldStop && !Thread.currentThread().isInterrupted()) {
                    long startTimeNanos = System.nanoTime();

                    AnimationHandler.getInstance().doAnimationFrame(Core.timeMillis());

                    long elapsedNanos = System.nanoTime() - startTimeNanos;
                    long sleepNanos = frameTimeNanos - elapsedNanos;

                    if (sleepNanos > 0) {
                        LockSupport.parkNanos(sleepNanos);
                    }
                }
            } catch (Exception e) {
                if (!shouldStop) {
                    e.printStackTrace();
                }
            }
        }, "SR-MUI-AnimationThread");

        animationThread.setDaemon(true);
        animationThread.start();
        isInitialized = true;
    }

    public static synchronized void shutdown() {
        if (!isInitialized) {
            return;
        }

        shouldStop = true;

        Looper frameLooper = Looper.getFrameLooper();
        if (frameLooper != null) {
            frameLooper.quitSafely();
        }

        if (animationThread != null && animationThread.isAlive()) {
            animationThread.interrupt();
            try {
                animationThread.join(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        isInitialized = false;
        animationThread = null;
    }

    public static boolean isAnimationThread() {
        return Thread.currentThread() == animationThread;
    }

    public static boolean isInitialized() {
        return isInitialized;
    }

    public static Thread getAnimationThread() {
        return animationThread;
    }
}