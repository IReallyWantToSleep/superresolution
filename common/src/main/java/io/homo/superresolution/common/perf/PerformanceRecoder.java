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
 * along with this program.  If not, see <https:
 */

package io.homo.superresolution.common.perf;

import io.homo.superresolution.common.config.SuperResolutionConfig;
import org.lwjgl.opengl.GL41;

public class PerformanceRecoder {
    private static int[] timeQueryIds = new int[3];
    private static float frameTime;
    private static boolean queriesInitialized = false;

    private static long worldTimeNs = 0;
    private static long upscaleTimeNs = 0;
    private static long frameTimeNs = 0;

    private static long cpuWorldTimeNs = 0;
    private static long cpuUpscaleTimeNs = 0;
    private static long cpuFrameTimeNs = 0;
    private static long cpuWorldStartTimeNs = 0;
    private static long cpuUpscaleStartTimeNs = 0;
    private static long cpuFrameStartTimeNs = 0;

    public static void initialize() {
        if (!queriesInitialized) {
            GL41.glGenQueries(timeQueryIds);
            queriesInitialized = true;
        }
    }

    public static void beginFrame() {
        cpuFrameStartTimeNs = System.nanoTime();
    }

    public static void endFrame() {
        frameTimeNs = cpuFrameTimeNs = System.nanoTime() - cpuFrameStartTimeNs;
        frameTime = cpuFrameTimeNs / 1_000_000.0f;
    }

    public static void beginUpscale() {
        cpuUpscaleStartTimeNs = System.nanoTime();
        if (SuperResolutionConfig.isEnableDetailedProfiling()) {
            GL41.glBeginQuery(GL41.GL_TIME_ELAPSED, timeQueryIds[1]);
        }
    }

    public static void endUpscale() {
        cpuUpscaleTimeNs = System.nanoTime() - cpuUpscaleStartTimeNs;

        if (SuperResolutionConfig.isEnableDetailedProfiling()) {
            GL41.glEndQuery(GL41.GL_TIME_ELAPSED);
            long[] upscaleTime = {0};
            GL41.glGetQueryObjectui64v(timeQueryIds[1], GL41.GL_QUERY_RESULT, upscaleTime);
            upscaleTimeNs = upscaleTime[0];
        }
    }

    public static void beginWorld() {
        cpuWorldStartTimeNs = System.nanoTime();
        if (SuperResolutionConfig.isEnableDetailedProfiling()) {
            GL41.glBeginQuery(GL41.GL_TIME_ELAPSED, timeQueryIds[0]);
        }
    }

    public static void endWorld() {
        cpuWorldTimeNs = System.nanoTime() - cpuWorldStartTimeNs;
        if (SuperResolutionConfig.isEnableDetailedProfiling()) {
            GL41.glEndQuery(GL41.GL_TIME_ELAPSED);
            long[] worldTime = {0};
            GL41.glGetQueryObjectui64v(timeQueryIds[0], GL41.GL_QUERY_RESULT, worldTime);
            worldTimeNs = worldTime[0];
        }
    }


    public static long getWorldTimeNs() {
        return worldTimeNs;
    }

    public static float getWorldTimeMs() {
        return worldTimeNs / 1_000_000.0f;
    }

    public static long getUpscaleTimeNs() {
        return upscaleTimeNs;
    }

    public static float getUpscaleTimeMs() {
        return upscaleTimeNs / 1_000_000.0f;
    }

    public static long getFrameTimeNs() {
        return frameTimeNs;
    }

    public static float getFrameTimeMs() {
        return frameTimeNs / 1_000_000.0f;
    }

    public static long getCpuWorldTimeNs() {
        return cpuWorldTimeNs;
    }

    public static float getCpuWorldTimeMs() {
        return cpuWorldTimeNs / 1_000_000.0f;
    }

    public static long getCpuUpscaleTimeNs() {
        return cpuUpscaleTimeNs;
    }

    public static float getCpuUpscaleTimeMs() {
        return cpuUpscaleTimeNs / 1_000_000.0f;
    }

    public static long getCpuFrameTimeNs() {
        return cpuFrameTimeNs;
    }

    public static float getCpuFrameTimeMs() {
        return cpuFrameTimeNs / 1_000_000.0f;
    }
}