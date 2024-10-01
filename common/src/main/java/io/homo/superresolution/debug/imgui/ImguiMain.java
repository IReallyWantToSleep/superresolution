package io.homo.superresolution.debug.imgui;

import imgui.ImFont;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.flag.ImGuiConfigFlags;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import io.homo.superresolution.debug.imgui.mixin.WindowAccessor;
import io.homo.superresolution.impl.CanDestroy;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public class ImguiMain implements CanDestroy {
    public static final ImGuiImplGlfw imGuiGlfw = new ImGuiImplGlfw();
    public static final ImGuiImplGl3 imGuiGl3 = new ImGuiImplGl3();
    public final ImGuiLayer imguiLayer = new ImGuiLayer();
    public boolean initDone = false;
    private static ImguiMain instance;
    public ImguiMain(){
        instance = this;
        initImGui();
        initDone = true;
    }

    private void initImGui() {
        ImGui.createContext();
        ImGuiIO io = ImGui.getIO();
        io.getFonts().addFontFromFileTTF("C:\\Users\\yyyyy\\AppData\\Local\\Microsoft\\Windows\\Fonts\\HarmonyOS_Sans_SC_Medium.ttf",16);
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

    public void render(){
        if (!initDone) return;
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
    public static ImguiMain getInstance() {
        return instance;
    }
}
