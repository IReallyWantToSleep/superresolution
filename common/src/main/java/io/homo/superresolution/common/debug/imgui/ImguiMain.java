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

package io.homo.superresolution.common.debug.imgui;

import imgui.ImGui;
import imgui.ImFont;
import imgui.ImFontAtlas;
import imgui.ImFontConfig;
import imgui.ImGuiIO;
import imgui.flag.ImGuiConfigFlags;
#if MC_VER < MC_26_1_2
import imgui.flag.ImGuiFreeTypeBuilderFlags;
#endif
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.minecraft.MinecraftWindow;
import io.homo.superresolution.core.impl.Destroyable;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.io.InputStream;

public class ImguiMain implements Destroyable {
    public static final ImGuiImplGlfw imGuiGlfw = new ImGuiImplGlfw();
    public static final ImGuiImplGl3 imGuiGl3 = new ImGuiImplGl3();
    private static final String IMGUI_FONT_PATH = "/assets/super_resolution/font/Font.ttf";
    private static final float UI_FONT_SIZE = 20.0f;
    private static ImguiMain instance;
    public final ImGuiLayer imguiLayer = new ImGuiLayer();
    public boolean initDone = false;
    private ImFont uiFont;
    private boolean fontAtlasUploaded = false;

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
        //io.addConfigFlags(ImGuiConfigFlags.ViewportsEnable);
        loadCustomFont(io);
        imGuiGlfw.init(MinecraftWindow.getWindowHandle(), true);
        imGuiGl3.init();
        rebuildFontAtlas(io);
    }

    private void loadCustomFont(ImGuiIO io) {
        ImFontAtlas fonts = io.getFonts();
        try (InputStream fontStream = ImguiMain.class.getResourceAsStream(IMGUI_FONT_PATH)) {
            if (fontStream == null) {
                uiFont = fonts.addFontDefault();
                io.setFontDefault(uiFont);
                return;
            }
            byte[] fontData = fontStream.readAllBytes();
            ImFontConfig fontConfig = new ImFontConfig();
            try {
                fontConfig.setName("Font Bold");
                #if MC_VER < MC_26_1_2
                fontConfig.setFontBuilderFlags(ImGuiFreeTypeBuilderFlags.Bold);
                #endif
                fontConfig.setOversampleH(2);
                fontConfig.setOversampleV(2);
                uiFont = fonts.addFontFromMemoryTTF(fontData, UI_FONT_SIZE, fontConfig, fonts.getGlyphRangesChineseFull());
            } finally {
                fontConfig.destroy();
            }
            if (uiFont == null) {
                uiFont = fonts.addFontDefault();
            }
            io.setFontDefault(uiFont);
        } catch (IOException e) {
            uiFont = fonts.addFontDefault();
            io.setFontDefault(uiFont);
        }
    }

    private void rebuildFontAtlas(ImGuiIO io) {
        ImFontAtlas fonts = io.getFonts();
        if (!fonts.isBuilt() && !fonts.build()) {
            throw new IllegalStateException("Failed to build ImGui font atlas");
        }
        imGuiGl3.destroyFontsTexture();
        if (!imGuiGl3.createFontsTexture()) {
            throw new IllegalStateException("Failed to upload ImGui font atlas texture");
        }
        fontAtlasUploaded = true;
    }

    public void destroy() {
        if (!initDone) {
            return;
        }
        imGuiGl3.shutdown();
        imGuiGlfw.shutdown();
        ImGui.destroyContext();
    }

    public void render() {
        if (!initDone || !SuperResolutionConfig.isEnableImgui()) {
            return;
        }
        if (!fontAtlasUploaded || !ImGui.getIO().getFonts().isBuilt()) {
            rebuildFontAtlas(ImGui.getIO());
        }
        imGuiGlfw.newFrame();
        imGuiGl3.newFrame();
        ImGui.newFrame();
        if (uiFont != null) {
            #if MC_VER >= MC_26_1_2
            ImGui.pushFont(uiFont, UI_FONT_SIZE);
            #else
            ImGui.pushFont(uiFont);
            #endif
        }
        imguiLayer.imgui();
        if (uiFont != null) {
            ImGui.popFont();
        }
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
