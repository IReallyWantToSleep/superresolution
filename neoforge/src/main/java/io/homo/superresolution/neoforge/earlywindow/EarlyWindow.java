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

package io.homo.superresolution.neoforge.earlywindow;

import io.homo.superresolution.core.graphics.GraphicsCapabilities;
import net.neoforged.fml.ModLoadingIssue;
import net.neoforged.fml.loading.FMLConfig;
#if MC_VER > MC_1_21_10
import net.neoforged.fml.loading.ProgramArgs;
#endif
import net.neoforged.neoforgespi.earlywindow.ImmediateWindowProvider;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.*;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_CREATION_API;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MAJOR;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MINOR;
import static org.lwjgl.glfw.GLFW.GLFW_FALSE;
import static org.lwjgl.glfw.GLFW.GLFW_NATIVE_CONTEXT_API;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_API;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_CORE_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_FORWARD_COMPAT;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_RESIZABLE;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;
import static org.lwjgl.glfw.GLFW.GLFW_VISIBLE;
import static org.lwjgl.opengl.GL11C.GL_TRUE;

public class EarlyWindow implements ImmediateWindowProvider {
    public long window;

    @Override
    public String name() {
        return "sr-early-window";
    }

    #if MC_VER > MC_1_21_10
    @Override
    public void crash(String message) {

    }

    @Override
    public void periodicTick() {

    }

    @Override
    public void initialize(ProgramArgs args) {
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }
        GraphicsCapabilities.detectSupportedVersions();
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CLIENT_API, GLFW_OPENGL_API);
        glfwWindowHint(GLFW_CONTEXT_CREATION_API, GLFW_NATIVE_CONTEXT_API);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE);
        glfwWindowHint(GLFW_VISIBLE, GLFW_TRUE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, GraphicsCapabilities.getHighestOpenGLVersion().left());
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, GraphicsCapabilities.getHighestOpenGLVersion().right());
        this.window = glfwCreateWindow(
                FMLConfig.getIntConfigValue(FMLConfig.ConfigValue.EARLY_WINDOW_WIDTH),
                FMLConfig.getIntConfigValue(FMLConfig.ConfigValue.EARLY_WINDOW_HEIGHT),
                "Minecraft Loading...",
                0L,
                0L
        );
    }

    @Override
    public void setMinecraftVersion(String version) {

    }

    @Override
    public void setNeoForgeVersion(String version) {

    }

    @Override
    public void crash(String message) {

    }

    @Override
    public void periodicTick() {

    }

    @Override
    public String getGLVersion() {
        return "";
    }

    @Override
    public void displayFatalErrorAndExit(List<ModLoadingIssue> issues, @Nullable Path modsFolder, @Nullable Path logFile, @Nullable Path crashReportFile) {

    }

    @Override
    public long takeOverGlfwWindow() {
        glfwSetWindowAttrib(this.window, GLFW_RESIZABLE, GLFW_TRUE);
        glfwShowWindow(this.window);
        return window;
    }


    @Override
    public void updateProgress(String label) {

    }

    @Override
    public void completeProgress() {

    }

    #else
    @Override
    public Runnable initialize(String[] arguments) {
        NeoOpenGLVersionOverride.override();
        return () -> {
        };
    }

    @Override
    public void updateFramebufferSize(IntConsumer width, IntConsumer height) {

    }

    @Override
    public long setupMinecraftWindow(IntSupplier width, IntSupplier height, Supplier<String> title, LongSupplier monitor) {
        return 0;
    }

    @Override
    public boolean positionWindow(Optional<Object> monitor, IntConsumer widthSetter, IntConsumer heightSetter, IntConsumer xSetter, IntConsumer ySetter) {
        return false;
    }

    @Override
    public <T> Supplier<T> loadingOverlay(Supplier<?> mc, Supplier<?> ri, Consumer<Optional<Throwable>> ex, boolean fade) {
        return null;
    }

    @Override
    public void updateModuleReads(ModuleLayer layer) {

    }

    @Override
    public void crash(String message) {

    }

    @Override
    public void periodicTick() {

    }

    @Override
    public String getGLVersion() {
        return "";
    }
    #endif
}
