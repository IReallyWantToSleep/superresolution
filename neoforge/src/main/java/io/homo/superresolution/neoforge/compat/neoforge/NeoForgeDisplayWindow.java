package io.homo.superresolution.neoforge.compat.neoforge;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL.createCapabilities;
import static org.lwjgl.opengl.GL32C.*;

import io.homo.superresolution.core.GraphicsCapabilities;
import io.homo.superresolution.neoforge.mixin.core.NeoForgeDisplayWindowAccessor;
import net.neoforged.fml.earlydisplay.DisplayWindow;
import net.neoforged.fml.earlydisplay.STBHelper;
import net.neoforged.fml.loading.FMLConfig;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.fml.loading.FMLServiceProvider;
import net.neoforged.neoforgespi.earlywindow.ImmediateWindowProvider;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.stb.STBImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class NeoForgeDisplayWindow implements ImmediateWindowProvider {
    protected final Logger LOGGER = LoggerFactory.getLogger("SuperResolution-EarlyDisplayWindow");
    protected long window = -1;

    @Override
    public String name() {
        return "fmlearlywindow";
    }

    @Override
    public Runnable initialize(String[] arguments) {
        return () -> {
            LOGGER.info("initialize");
        };
    }

    @Override
    public void updateModuleReads(ModuleLayer layer) {

    }

    @Override
    public void crash(String message) {
        LOGGER.error(message);
    }

    @Override
    public long takeOverGlfwWindow() {
        initWindow(FMLLoader.versionInfo().mcVersion());
        return window;
    }

    @Override
    public void periodicTick() {

    }

    @Override
    public void updateProgress(String label) {
        LOGGER.info(label);

    }

    @Override
    public void completeProgress() {
        LOGGER.info("completeProgress");
    }

    /*
     * Copyright (c) Forge Development LLC and contributors
     * SPDX-License-Identifier: LGPL-2.1-only
     */

    public void initWindow(@Nullable String mcVersion) {
        glfwInit();
        GraphicsCapabilities.detectSupportedVersions();
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CLIENT_API, GLFW_OPENGL_API);
        glfwWindowHint(GLFW_CONTEXT_CREATION_API, GLFW_NATIVE_CONTEXT_API);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, GraphicsCapabilities.getHighestOpenGLVersion().left());
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, GraphicsCapabilities.getHighestOpenGLVersion().right());
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE);
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        String vanillaWindowTitle = "Minecraft* " + mcVersion;
        glfwWindowHintString(GLFW_X11_CLASS_NAME, vanillaWindowTitle);
        glfwWindowHintString(GLFW_X11_INSTANCE_NAME, vanillaWindowTitle);
        if (FMLConfig.getBoolConfigValue(FMLConfig.ConfigValue.DEBUG_OPENGL)) {
            glfwWindowHint(GLFW_OPENGL_DEBUG_CONTEXT, GL_TRUE);
        }

        long primaryMonitor = glfwGetPrimaryMonitor();
        if (primaryMonitor == 0) {
            throw new IllegalStateException("Can't find a primary monitor");
        }
        GLFWVidMode vidmode = glfwGetVideoMode(primaryMonitor);
        if (vidmode == null) {
            throw new IllegalStateException("Can't get a resolution");
        }

        var successfulWindow = new AtomicBoolean(false);

        window = glfwCreateWindow(854, 480, "Minecraft: NeoForge Loading...", 0L, 0L);
        if (window <= 0L) {
            throw new IllegalStateException("Failed to create a window");
        }

        successfulWindow.set(true);
        int[] x = new int[1];
        int[] y = new int[1];
        glfwGetMonitorPos(primaryMonitor, x, y);
        int monitorX = x[0];
        int monitorY = y[0];

        glfwGetWindowSize(window, x, y);
        glfwSetWindowPos(window, (vidmode.width() - x[0]) / 2 + monitorX, (vidmode.height() - y[0]) / 2 + monitorY);

        int[] channels = new int[1];
        try (var glfwImgBuffer = GLFWImage.malloc(1)) {
            final ByteBuffer imgBuffer;
            try (GLFWImage glfwImages = GLFWImage.malloc()) {
                imgBuffer = STBHelper.loadImageFromClasspath("neoforged_icon.png", 20000, x, y, channels);
                glfwImgBuffer.put(glfwImages.set(x[0], y[0], imgBuffer));
                glfwImgBuffer.flip();
                glfwSetWindowIcon(window, glfwImgBuffer);
                STBImage.stbi_image_free(imgBuffer);
            }
        } catch (NullPointerException e) {
            LOGGER.error("Failed to load NeoForged icon");
        }

        glfwShowWindow(window);
        glfwPollEvents();
    }
}
