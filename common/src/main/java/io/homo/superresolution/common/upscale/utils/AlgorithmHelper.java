package io.homo.superresolution.common.upscale.utils;

import com.mojang.blaze3d.systems.RenderSystem;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.Config;
import io.homo.superresolution.common.impl.Destroyable;
import io.homo.superresolution.common.impl.Resizable;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;

import static io.homo.superresolution.common.render.gl.Gl.*;
import static io.homo.superresolution.common.render.gl.GlConst.GL_EXTENSIONS;
import static io.homo.superresolution.common.render.gl.GlConst.GL_NUM_EXTENSIONS;

public class AlgorithmHelper implements Resizable, Destroyable {
    private static final ArrayList<String> GLExtension = new ArrayList<>();
    public static int[] GLVersion;

    static {
        GLVersion = getVersion();
        int l = glGetInteger(GL_NUM_EXTENSIONS);
        for (int i = 0; i < l; ++i) {
            GLExtension.add(glGetStringi(GL_EXTENSIONS, i));
        }
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
