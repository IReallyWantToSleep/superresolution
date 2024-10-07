package io.homo.superresolution.debug.imgui;

import imgui.ImGui;
import io.homo.superresolution.SuperResolution;
import io.homo.superresolution.upscale.AlgorithmManager;
import io.homo.superresolution.upscale.fsr2.FSR2;
import net.minecraft.client.Minecraft;

public class ImGuiLayer {
    public void imgui() {
        int width = 500;
        float height = (((float) width) /SuperResolution.getMinecraftWidth())*SuperResolution.getMinecraftHeight();
        ImGui.begin("DEBUG");
        if (SuperResolution.algorithmType == AlgorithmManager.AlgorithmType.FSR2){
            ImGui.text("mvFramebuffer "+ FSR2.helper.screenWidth+" "+FSR2.helper.screenHeight);
            ImGui.image(FSR2.helper.getMotionVectorsTex(),
                    width,
                    height,
                    0,1,1,0);
        }

        ImGui.text("outFramebuffer "+ AlgorithmManager.helper.screenWidth+" "+AlgorithmManager.helper.screenHeight);
        ImGui.image(SuperResolution.currentAlgorithm.getOutputTexId(),
                width,
                height,
                0,1,1,0);


        ImGui.text("inFramebuffer "+AlgorithmManager.helper.renderWidth+" "+AlgorithmManager.helper.renderHeight);
        ImGui.image(SuperResolution.currentAlgorithm.getInputTexId(),
                width,
                height,
                0,1,1,0);
        ImGui.text("inFramebufferD "+AlgorithmManager.helper.renderWidth+" "+AlgorithmManager.helper.renderHeight);
        ImGui.image(SuperResolution.currentAlgorithm.getInput().getDepthTextureId(),
                width,
                height,
                0,1,1,0);

        ImGui.text("MainRenderTarget "+AlgorithmManager.helper.screenWidth+" "+AlgorithmManager.helper.screenHeight);
        ImGui.image(Minecraft.getInstance().getMainRenderTarget().getColorTextureId(),
                width,
                height,
                0,1,1,0);
        ImGui.text("MainRenderTargetD "+AlgorithmManager.helper.screenWidth+" "+AlgorithmManager.helper.screenHeight);
        ImGui.image(Minecraft.getInstance().getMainRenderTarget().getDepthTextureId(),
                width,
                height,
                0,1,1,0);
        ImGui.end();
    }
}