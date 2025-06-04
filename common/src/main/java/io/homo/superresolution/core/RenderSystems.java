package io.homo.superresolution.core;

import io.homo.superresolution.common.config.Config;
import io.homo.superresolution.core.graphics.opengl.GlRenderSystem;
import io.homo.superresolution.core.graphics.vulkan.VkRenderSystem;

public class RenderSystems {
    private static VkRenderSystem vulkan;
    private static GlRenderSystem opengl;

    public static void init() {
        opengl = new GlRenderSystem();
        if (!Config.isSkipInitVulkan()) {
            vulkan = new VkRenderSystem();
            //vulkan.initRenderSystem();
        }
        opengl.initRenderSystem();
    }

    public static GlRenderSystem opengl() {
        return opengl;
    }

    public static VkRenderSystem vulkan() {
        return vulkan;
    }

    public static GlRenderSystem current() {
        return opengl;
    }
}
