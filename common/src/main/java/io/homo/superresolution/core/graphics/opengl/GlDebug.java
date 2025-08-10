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

    private static final AtomicInteger commandBufferIdCounter = new AtomicInteger(DEBUG_GROUP_COMMANDBUFFER_ID_BEGIN);
    private static final AtomicInteger drawIdCounter = new AtomicInteger(DEBUG_GROUP_DRAW_ID_BEGIN);
    private static final AtomicInteger computeIdCounter = new AtomicInteger(DEBUG_GROUP_COMPUTE_ID_BEGIN);
    private static final AtomicInteger copyIdCounter = new AtomicInteger(DEBUG_GROUP_COPY_ID_BEGIN);
    private static final AtomicInteger clearIdCounter = new AtomicInteger(DEBUG_GROUP_CLEAR_ID_BEGIN);
    private static final AtomicInteger stateIdCounter = new AtomicInteger(DEBUG_GROUP_STATE_ID_BEGIN);


    public static void popGroup() {
        if (!SuperResolutionConfig.isEnableDebug()) return;
        if (GL.getCapabilities().GL_KHR_debug) KHRDebug.glPopDebugGroup();
    }

    public static void pushGroup(int id, String name) {
        if (!SuperResolutionConfig.isEnableDebug()) return;

        if (GL.getCapabilities().GL_KHR_debug) KHRDebug.glPushDebugGroup(
                KHRDebug.GL_DEBUG_SOURCE_APPLICATION,
                id,
                StringUtils.abbreviate(name, 255)
        );
    }

    public static void objectLabel(int type, int id, String label) {
        if (!SuperResolutionConfig.isEnableDebug()) return;

        if (GL.getCapabilities().GL_KHR_debug) {
            KHRDebug.glObjectLabel(type, id, StringUtils.abbreviate(label, 255));
        }
    }


    public static int nextCommandBufferId() {
        if (!SuperResolutionConfig.isEnableDebug()) return 0;
        return commandBufferIdCounter.getAndIncrement();
    }


    public static int nextDrawId() {
        if (!SuperResolutionConfig.isEnableDebug()) return 0;
        return drawIdCounter.getAndIncrement();
    }


    public static int nextComputeId() {
        if (!SuperResolutionConfig.isEnableDebug()) return 0;
        return computeIdCounter.getAndIncrement();
    }


    public static int nextClearId() {
        if (!SuperResolutionConfig.isEnableDebug()) return 0;
        return clearIdCounter.getAndIncrement();
    }


    public static int nextCopyId() {
        if (!SuperResolutionConfig.isEnableDebug()) return 0;
        return copyIdCounter.getAndIncrement();
    }

    public static int nextStateId() {
        if (!SuperResolutionConfig.isEnableDebug()) return 0;
        return stateIdCounter.getAndIncrement();
    }
}