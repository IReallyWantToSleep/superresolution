package io.homo.superresolution.common.upscale.utils;

import com.mojang.blaze3d.systems.RenderSystem;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.Config;
import io.homo.superresolution.common.impl.Destroyable;
import io.homo.superresolution.common.impl.Resizable;
import net.minecraft.client.Minecraft;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VK11;

import java.nio.IntBuffer;
import java.util.ArrayList;

import static io.homo.superresolution.common.render.gl.Gl.*;
import static io.homo.superresolution.common.render.gl.GlConst.GL_EXTENSIONS;
import static io.homo.superresolution.common.render.gl.GlConst.GL_NUM_EXTENSIONS;

public class AlgorithmHelper implements Resizable, Destroyable {
    public static final ArrayList<String> GLExtension = new ArrayList<>();
    public static int[] GLVersion;
    public static int[] VkVersion;

    static {
        GLVersion = getVersion();
        int l = glGetInteger(GL_NUM_EXTENSIONS);
        for (int i = 0; i < l; ++i) {
            GLExtension.add(glGetStringi(GL_EXTENSIONS, i));
        }
        IntBuffer vkVer = MemoryStack.stackCallocInt(1);
        VK11.vkEnumerateInstanceVersion(vkVer);

        VkVersion = new int[]{
                VK10.VK_API_VERSION_MAJOR(vkVer.get(0)),
                VK10.VK_API_VERSION_MINOR(vkVer.get(0)),
                VK10.VK_API_VERSION_PATCH(vkVer.get(0)),
        };
    }

    public AlgorithmHelper() {
        RenderSystem.assertOnRenderThread();
        this.resize(Minecraft.getInstance().getWindow().getScreenWidth(), Minecraft.getInstance().getWindow().getScreenHeight());

    }

    public static boolean hasGLExtension(String name) {
        return GLExtension.contains(name);
    }

    public void updateMotionVectors() {
        RenderSystem.assertOnRenderThread();
    }

    public void resize(int width, int height) {
        RenderSystem.assertOnRenderThread();
    }

    public ArrayList<String> getGLExtension() {
        return GLExtension;
    }

    public void destroy() {
    }

    public int getRenderHeight() {
        return (int) Math.max(getScreenHeight() * Config.getRenderScaleFactor(), 1);
    }

    public int getRenderWidth() {
        return (int) Math.max(getScreenWidth() * Config.getRenderScaleFactor(), 1);
    }

    public int getScreenHeight() {
        return SuperResolution.getMinecraftHeight();
    }

    public int getScreenWidth() {
        return SuperResolution.getMinecraftWidth();
    }
}
