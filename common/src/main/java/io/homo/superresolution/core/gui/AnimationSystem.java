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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class AnimationSystem {
    private static volatile Thread animationThread;
    private static volatile boolean isInitialized = false;

    public static synchronized void initialize() {
        if (isInitialized) {
            return;
        }
        long frameTime = (long) ((1000L / 120f) * 1000L);
        animationThread = new Thread(() -> {
            try {
                while (true) {
                    long currentTimeNanos = System.nanoTime();
                    AnimationHandler.getInstance().doAnimationFrame(Core.timeMillis());
                    Thread.sleep(Duration.ofNanos(Math.max(
                            frameTime - (System.nanoTime() - currentTimeNanos),
                            0
                    )));
                }
            } catch (Exception e) {
                e.printStackTrace();
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


        Looper frameLooper = Looper.getFrameLooper();
        if (frameLooper != null) {
            frameLooper.quitSafely();
        }

        if (animationThread != null && animationThread.isAlive()) {
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