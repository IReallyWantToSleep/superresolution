package io.homo.superresolution.common.debug.imgui;

import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.flag.ImGuiConfigFlags;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.core.impl.Destroyable;
import net.minecraft.client.Minecraft;
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
        imGuiGlfw.init(Minecraft.getInstance().getWindow().getWindow(), true);
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
