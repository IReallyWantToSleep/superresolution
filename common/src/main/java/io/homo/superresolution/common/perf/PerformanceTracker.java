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
    private static final int MAX_RESULT = 128;
    private static final Object2ObjectOpenHashMap<String, TrackerContext> contextMap = new Object2ObjectOpenHashMap<>();

    static {
        addOperation("Frame");
        addOperation("Level Render");
        addOperation("Main Render");
        addOperation("Upscale");
    }

    public static void addOperation(String operationName) {
        contextMap.computeIfAbsent(operationName, k -> new TrackerContext());
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

        ctx.tempCpuStart = System.nanoTime();

        if (!SuperResolutionConfig.isEnableDetailedProfiling()) {
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

        if (SuperResolutionConfig.isEnableDetailedProfiling()) {
            ctx.ensureQueriesInitialized();
            GL41.glQueryCounter(ctx.queryIdsEnd[ctx.cursor], GL41.GL_TIMESTAMP);
            ctx.queryEnded[ctx.cursor] = true;
        }

        long end = System.nanoTime();
        ctx.cpuTimes[ctx.cursor] = end - ctx.tempCpuStart;

        if (SuperResolutionConfig.isEnableDetailedProfiling()) {
            tryCleanPendingResults(ctx);
        }

        ctx.cursor = (ctx.cursor + 1) % MAX_RESULT;
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

        ctx.ensureQueriesInitialized();

        int lastIdx = (ctx.cursor - 1 + MAX_RESULT) % MAX_RESULT;

        if (ctx.queryPending[lastIdx] && ctx.queryEnded[lastIdx]) {
            syncGpuResultAtIndex(ctx, lastIdx, true);
        }

        return ctx.gpuTimes[lastIdx];
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
        long tempCpuStart = 0;
        boolean queriesInitialized = false;

        TrackerContext() {
            if (SuperResolutionConfig.isEnableDetailedProfiling()) {
                initQueries();
            }
        }

        private void initQueries() {
            GL41.glGenQueries(queryIdsStart);
            GL41.glGenQueries(queryIdsEnd);
            queriesInitialized = true;
        }

        void ensureQueriesInitialized() {
            if (!queriesInitialized && SuperResolutionConfig.isEnableDetailedProfiling()) {
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
        }
    }
}