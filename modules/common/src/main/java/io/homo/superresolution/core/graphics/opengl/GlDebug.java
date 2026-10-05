/*
 * Super Resolution
 * Copyright (c) 2025-2026. 187J3X1-114514
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

package io.homo.superresolution.core.graphics.opengl;

import io.homo.superresolution.common.config.SuperResolutionConfig;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.KHRDebug;

import java.util.concurrent.atomic.AtomicInteger;

public class GlDebug {
    public static final int DEBUG_GROUP_COMMANDBUFFER_ID_BEGIN = 0xF0000;
    public static final int DEBUG_GROUP_DRAW_ID_BEGIN = DEBUG_GROUP_COMMANDBUFFER_ID_BEGIN + 0xF0000;
    public static final int DEBUG_GROUP_COMPUTE_ID_BEGIN = DEBUG_GROUP_DRAW_ID_BEGIN + 0xF0000;
    public static final int DEBUG_GROUP_COPY_ID_BEGIN = DEBUG_GROUP_COMPUTE_ID_BEGIN + 0xF0000;
    public static final int DEBUG_GROUP_CLEAR_ID_BEGIN = DEBUG_GROUP_COPY_ID_BEGIN + 0xF0000;
    public static final int DEBUG_GROUP_STATE_ID_BEGIN = DEBUG_GROUP_CLEAR_ID_BEGIN + 0xF0000;
    private static final GlDebugBackend DEBUG_IMPL = new DebugImpl();
    private static final GlDebugBackend NO_OP_IMPL = new NoOpImpl();
    private static volatile GlDebugBackend backend = SuperResolutionConfig.isEnableDebug() ? DEBUG_IMPL : NO_OP_IMPL;

    public static void popGroup() {
        backend.popGroup();
    }

    public static void pushGroup(int id, String name) {
        backend.pushGroup(id, name);
    }

    public static void objectLabel(int type, int id, String label) {
        backend.objectLabel(type, id, label);
    }

    public static void useDebugImpl() {
        setEnabled(true);
    }

    public static void useNoOpImpl() {
        setEnabled(false);
    }

    public static void refreshFromConfig() {
        setEnabled(SuperResolutionConfig.isEnableDebug());
    }

    public static boolean isEnabled() {
        return backend == DEBUG_IMPL;
    }

    public static void setEnabled(boolean enabled) {
        backend = enabled ? DEBUG_IMPL : NO_OP_IMPL;
    }

    public static int nextCommandBufferId() {
        return backend.nextCommandBufferId();
    }


    public static int nextDrawId() {
        return backend.nextDrawId();
    }


    public static int nextComputeId() {
        return backend.nextComputeId();
    }


    public static int nextClearId() {
        return backend.nextClearId();
    }


    public static int nextCopyId() {
        return backend.nextCopyId();
    }

    public static int nextStateId() {
        return backend.nextStateId();
    }

    private interface GlDebugBackend {
        void popGroup();

        void pushGroup(int id, String name);

        void objectLabel(int type, int id, String label);

        int nextCommandBufferId();

        int nextDrawId();

        int nextComputeId();

        int nextClearId();

        int nextCopyId();

        int nextStateId();
    }

    private static final class NoOpImpl implements GlDebugBackend {
        @Override
        public void popGroup() {
        }

        @Override
        public void pushGroup(int id, String name) {
        }

        @Override
        public void objectLabel(int type, int id, String label) {
        }

        @Override
        public int nextCommandBufferId() {
            return 0;
        }

        @Override
        public int nextDrawId() {
            return 0;
        }

        @Override
        public int nextComputeId() {
            return 0;
        }

        @Override
        public int nextClearId() {
            return 0;
        }

        @Override
        public int nextCopyId() {
            return 0;
        }

        @Override
        public int nextStateId() {
            return 0;
        }
    }

    private static final class DebugImpl implements GlDebugBackend {
        private final AtomicInteger stateIdCounter = new AtomicInteger(DEBUG_GROUP_STATE_ID_BEGIN);
        private final AtomicInteger clearIdCounter = new AtomicInteger(DEBUG_GROUP_CLEAR_ID_BEGIN);
        private final AtomicInteger copyIdCounter = new AtomicInteger(DEBUG_GROUP_COPY_ID_BEGIN);
        private final AtomicInteger computeIdCounter = new AtomicInteger(DEBUG_GROUP_COMPUTE_ID_BEGIN);
        private final AtomicInteger drawIdCounter = new AtomicInteger(DEBUG_GROUP_DRAW_ID_BEGIN);
        private final AtomicInteger commandBufferIdCounter = new AtomicInteger(DEBUG_GROUP_COMMANDBUFFER_ID_BEGIN);

        @Override
        public void popGroup() {
            if (GL.getCapabilities().GL_KHR_debug) {
                KHRDebug.glPopDebugGroup();
            }
        }

        @Override
        public void pushGroup(int id, String name) {
            if (GL.getCapabilities().GL_KHR_debug) {
                KHRDebug.glPushDebugGroup(
                        KHRDebug.GL_DEBUG_SOURCE_APPLICATION,
                        id,
                        StringUtils.abbreviate(name, 255)
                );
            }
        }

        @Override
        public void objectLabel(int type, int id, String label) {
            if (GL.getCapabilities().GL_KHR_debug) {
                KHRDebug.glObjectLabel(type, id, StringUtils.abbreviate(label, 255));
            }
        }

        @Override
        public int nextCommandBufferId() {
            return commandBufferIdCounter.getAndIncrement();
        }

        @Override
        public int nextDrawId() {
            return drawIdCounter.getAndIncrement();
        }

        @Override
        public int nextComputeId() {
            return computeIdCounter.getAndIncrement();
        }

        @Override
        public int nextClearId() {
            return clearIdCounter.getAndIncrement();
        }

        @Override
        public int nextCopyId() {
            return copyIdCounter.getAndIncrement();
        }

        @Override
        public int nextStateId() {
            return stateIdCounter.getAndIncrement();
        }
    }
}