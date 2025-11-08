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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.homo.superresolution.common.debug.imgui;

import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.flag.ImGuiConfigFlags;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.minecraft.MinecraftWindow;
import io.homo.superresolution.core.impl.Destroyable;
import org.lwjgl.glfw.GLFW;

public class ImguiMain implements Destroyable {
    public static final ImGuiImplGlfw imGuiGlfw = new ImGuiImplGlfw();
    public static final ImGuiImplGl3 imGuiGl3 = new ImGuiImplGl3();
    private static ImguiMain instance;
    public final ImGuiLayer imguiLayer = new ImGuiLayer();
    public boolean initDone = false;

    public ImguiMain() {
        instance = this;
        initImGui();
        initDone = true;
    }

    public static ImguiMain getInstance() {
        return instance;
    }

    private void initImGui() {
        ImGui.createContext();
        ImGuiIO io = ImGui.getIO();
        io.getFonts().setFreeTypeRenderer(true);
        io.addConfigFlags(ImGuiConfigFlags.ViewportsEnable);
        imGuiGlfw.init(MinecraftWindow.getWindowHandle(), true);
        imGuiGl3.init();
    }

    public void destroy() {
        if (!initDone) return;
        imGuiGl3.shutdown();
        imGuiGlfw.shutdown();
        ImGui.destroyContext();
    }

    public void render() {
        if (!initDone || !SuperResolutionConfig.isEnableImgui()) return;
        imGuiGlfw.newFrame();
        imGuiGl3.newFrame();
        ImGui.newFrame();
        imguiLayer.imgui();
        ImGui.render();
        imGuiGl3.renderDrawData(ImGui.getDrawData());
        if (ImGui.getIO().hasConfigFlags(ImGuiConfigFlags.ViewportsEnable)) {
            final long backupWindowPtr = org.lwjgl.glfw.GLFW.glfwGetCurrentContext();
            ImGui.updatePlatformWindows();
            ImGui.renderPlatformWindowsDefault();
            GLFW.glfwMakeContextCurrent(backupWindowPtr);
        }
    }
}
