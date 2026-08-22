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

import io.homo.superresolution.common.config.SuperResolutionConfig;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.lwjgl.opengl.GL41;

import java.util.Arrays;

public class PerformanceTracker {
    public static final String VK_UPSCALE = "VK Upscale";
    public static final String VK_FRAME_GEN = "VK FrameGen";
    public static final String VK_PRESENT_BLIT = "VK Present Blit";
    public static final String GL_INTEROP_FLIP = "GL Interop Flip";
    public static final String GL_CAPTURE_FLIP = "GL Capture Flip";
    public static final String GL_INPUT_CONVERT = "GL Input Convert";

    private static final int MAX_RESULT = 256;
    private static final Object2ObjectOpenHashMap<String, TrackerContext> contextMap = new Object2ObjectOpenHashMap<>();

    static {
        addOperation("Frame");
        addOperation("Reflex Sleep");
        addOperation("Level Render");
        addOperation("Main Render");
        addOperation("Upscale");
        addOperation("GUI");
        // Regions timed with GPU-side queries owned elsewhere: the Vulkan ones come from
        // VulkanTimestampProfiler, the GL ones from the interop/capture passes. Registered
        // up front so submitExternalGpuSample only ever writes into an existing context.
        addExternalGpuOperation(VK_UPSCALE);
        addExternalGpuOperation(VK_FRAME_GEN);
        addExternalGpuOperation(VK_PRESENT_BLIT);
        addOperation(GL_INTEROP_FLIP);
        addOperation(GL_CAPTURE_FLIP);
        addOperation(GL_INPUT_CONVERT);
    }

    public static void addOperation(String operationName) {
        contextMap.computeIfAbsent(operationName, k -> new TrackerContext(false));
    }

    /**
     * Registers an operation whose GPU time arrives via {@link #submitExternalGpuSample}.
     * These never allocate GL query objects, because their timings come from a Vulkan
     * query pool on a different device.
     */
    public static void addExternalGpuOperation(String operationName) {
        contextMap.computeIfAbsent(operationName, k -> new TrackerContext(true));
    }

    public static void beginFrame() {
        TrackerContext ctx = contextMap.get("Frame");
        if (ctx == null) {
            addOperation("Frame");
            ctx = contextMap.get("Frame");
            if (ctx == null) {
                return;
            }
        }

        ctx.tempCpuStart = System.nanoTime();
        ctx.cpuStartPending = true;
    }

    public static void push(String operationName) {
        TrackerContext ctx = contextMap.get(operationName);
        if (ctx == null) {
            addOperation(operationName);
            ctx = contextMap.get(operationName);
            if (ctx == null) {
                return;
            }
        }

        if (!("Frame".equals(operationName) && ctx.cpuStartPending)) {
            ctx.tempCpuStart = System.nanoTime();
        }
        ctx.cpuStartPending = false;

        if (!SuperResolutionConfig.isEnableDetailedProfiling() || ctx.externalGpu) {
            return;
        }

        ctx.ensureQueriesInitialized();

        if (ctx.queryPending[ctx.cursor] && ctx.queryEnded[ctx.cursor]) {
            syncGpuResultAtIndex(ctx, ctx.cursor, true);
        }

        GL41.glQueryCounter(ctx.queryIdsStart[ctx.cursor], GL41.GL_TIMESTAMP);

        ctx.queryPending[ctx.cursor] = true;
        ctx.queryEnded[ctx.cursor] = false;
    }

    public static void pop(String operationName) {
        TrackerContext ctx = contextMap.get(operationName);
        if (ctx == null) {
            return;
        }

        if (SuperResolutionConfig.isEnableDetailedProfiling() && !ctx.externalGpu) {
            ctx.ensureQueriesInitialized();
            GL41.glQueryCounter(ctx.queryIdsEnd[ctx.cursor], GL41.GL_TIMESTAMP);
            ctx.queryEnded[ctx.cursor] = true;
        }

        long end = System.nanoTime();
        ctx.cpuTimes[ctx.cursor] = end - ctx.tempCpuStart;
        ctx.cpuStartPending = false;

        if (SuperResolutionConfig.isEnableDetailedProfiling() && !ctx.externalGpu) {
            tryCleanPendingResults(ctx);
        }

        ctx.cursor = (ctx.cursor + 1) % MAX_RESULT;
    }

    /**
     * Records a GPU duration measured by something other than this class's GL queries -
     * currently {@code VulkanTimestampProfiler}, whose timestamps come back several
     * frames after the work was recorded. Such a context keeps its own ring cursor
     * because there is no {@code push}/{@code pop} pair driving it.
     */
    public static void submitExternalGpuSample(String operationName, long nanos) {
        TrackerContext ctx = contextMap.get(operationName);
        if (ctx == null) {
            return;
        }
        ctx.gpuTimes[ctx.externalCursor] = nanos;
        ctx.externalCursor = (ctx.externalCursor + 1) % MAX_RESULT;
        ctx.cursor = ctx.externalCursor;
    }

    public static void clear(String operationName) {
        TrackerContext ctx = contextMap.remove(operationName);
        if (ctx != null) {
            ctx.cleanup();
        }
    }

    public static void clearAll() {
        for (TrackerContext ctx : contextMap.values()) {
            ctx.cleanup();
        }
        contextMap.clear();
    }

    public static long[] getAllResultsCPU(String operationName) {
        TrackerContext ctx = contextMap.get(operationName);
        if (ctx == null) {
            return new long[0];
        }

        long[] result = new long[MAX_RESULT];
        int head = ctx.cursor;
        int len1 = MAX_RESULT - head;

        System.arraycopy(ctx.cpuTimes, head, result, 0, len1);
        if (head > 0) {
            System.arraycopy(ctx.cpuTimes, 0, result, len1, head);
        }
        return result;
    }

    public static long[] getAllResultsGPU(String operationName) {
        if (!SuperResolutionConfig.isEnableDetailedProfiling()) {
            return new long[0];
        }

        TrackerContext ctx = contextMap.get(operationName);
        if (ctx == null) {
            return new long[0];
        }

        if (ctx.externalGpu) {
            return rotatedCopy(ctx.gpuTimes, ctx.cursor);
        }

        ctx.ensureQueriesInitialized();

        for (int i = 0; i < MAX_RESULT; i++) {
            if (ctx.queryPending[i] && ctx.queryEnded[i]) {
                syncGpuResultAtIndex(ctx, i, true);
            }
        }

        long[] result = new long[MAX_RESULT];
        int head = ctx.cursor;
        int len1 = MAX_RESULT - head;

        System.arraycopy(ctx.gpuTimes, head, result, 0, len1);
        if (head > 0) {
            System.arraycopy(ctx.gpuTimes, 0, result, len1, head);
        }
        return result;
    }

    public static long getLastResultCPU(String operationName) {
        TrackerContext ctx = contextMap.get(operationName);
        if (ctx == null) {
            return 0;
        }
        int lastIdx = (ctx.cursor - 1 + MAX_RESULT) % MAX_RESULT;
        return ctx.cpuTimes[lastIdx];
    }

    public static long getLastResultGPU(String operationName) {
        if (!SuperResolutionConfig.isEnableDetailedProfiling()) {
            return 0;
        }

        TrackerContext ctx = contextMap.get(operationName);
        if (ctx == null) {
            return 0;
        }

        int lastIdx = (ctx.cursor - 1 + MAX_RESULT) % MAX_RESULT;

        if (ctx.externalGpu) {
            return ctx.gpuTimes[lastIdx];
        }

        ctx.ensureQueriesInitialized();

        if (ctx.queryPending[lastIdx] && ctx.queryEnded[lastIdx]) {
            syncGpuResultAtIndex(ctx, lastIdx, true);
        }

        return ctx.gpuTimes[lastIdx];
    }

    private static long[] rotatedCopy(long[] source, int head) {
        long[] result = new long[MAX_RESULT];
        int len1 = MAX_RESULT - head;
        System.arraycopy(source, head, result, 0, len1);
        if (head > 0) {
            System.arraycopy(source, 0, result, len1, head);
        }
        return result;
    }

    private static void tryCleanPendingResults(TrackerContext ctx) {
        int checks = 0;
        int idx = (ctx.cursor - 1 + MAX_RESULT) % MAX_RESULT;
        while (checks < 5) {
            if (ctx.queryPending[idx] && ctx.queryEnded[idx]) {
                if (!syncGpuResultAtIndex(ctx, idx, false)) {
                    break;
                }
            }
            idx = (idx - 1 + MAX_RESULT) % MAX_RESULT;
            checks++;
        }
    }

    private static boolean syncGpuResultAtIndex(TrackerContext ctx, int index, boolean forceWait) {
        if (!ctx.queryPending[index] || !ctx.queryEnded[index]) {
            return true;
        }

        int startId = ctx.queryIdsStart[index];
        int endId = ctx.queryIdsEnd[index];

        if (!forceWait) {
            int available = GL41.glGetQueryObjecti(endId, GL41.GL_QUERY_RESULT_AVAILABLE);
            if (available == 0) {
                return false;
            }
        }

        long startTime = GL41.glGetQueryObjectui64(startId, GL41.GL_QUERY_RESULT);
        long endTime = GL41.glGetQueryObjectui64(endId, GL41.GL_QUERY_RESULT);

        ctx.gpuTimes[index] = Math.max(0L, endTime - startTime);
        ctx.queryPending[index] = false;
        return true;
    }

    private static class TrackerContext {
        final int[] queryIdsStart = new int[MAX_RESULT];
        final int[] queryIdsEnd = new int[MAX_RESULT];

        final long[] cpuTimes = new long[MAX_RESULT];
        final long[] gpuTimes = new long[MAX_RESULT];

        final boolean[] queryPending = new boolean[MAX_RESULT];
        final boolean[] queryEnded = new boolean[MAX_RESULT]; // 防止 pop 意外没有调用导致的卡死

        int cursor = 0;
        int externalCursor = 0;
        boolean externalGpu = false;
        long tempCpuStart = 0;
        boolean cpuStartPending = false;
        boolean queriesInitialized = false;

        TrackerContext(boolean external) {
            this.externalGpu = external;
            if (!external && SuperResolutionConfig.isEnableDetailedProfiling()) {
                initQueries();
            }
        }

        private void initQueries() {
            GL41.glGenQueries(queryIdsStart);
            GL41.glGenQueries(queryIdsEnd);
            queriesInitialized = true;
        }

        void ensureQueriesInitialized() {
            if (!externalGpu && !queriesInitialized && SuperResolutionConfig.isEnableDetailedProfiling()) {
                initQueries();
            }
        }

        void cleanup() {
            if (queriesInitialized) {
                GL41.glDeleteQueries(queryIdsStart);
                GL41.glDeleteQueries(queryIdsEnd);
                queriesInitialized = false;
            }
            Arrays.fill(queryPending, false);
            Arrays.fill(queryEnded, false);
            Arrays.fill(cpuTimes, 0);
            Arrays.fill(gpuTimes, 0);
            cursor = 0;
            cpuStartPending = false;
        }
    }
}
