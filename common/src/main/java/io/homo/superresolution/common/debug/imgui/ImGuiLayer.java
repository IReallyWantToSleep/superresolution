package io.homo.superresolution.common.debug.imgui;

import com.mojang.blaze3d.pipeline.RenderTarget;
import imgui.ImGui;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.platform.Platform;
import io.homo.superresolution.common.render.MinecraftRenderingStates;
import io.homo.superresolution.common.upscale.AlgorithmManager;
import net.minecraft.client.Minecraft;

public class ImGuiLayer {
    public void imgui() {

        int width = 500;
        float height = (((float) width) / SuperResolution.getMinecraftWidth()) * SuperResolution.getMinecraftHeight();
        ImGui.begin("DEBUG");
        if (ImGui.button("iris")){
            System.out.println(Platform.currentPlatform.iris().isShaderPackInUse()?"true":"false");
        }

        if (!SuperResolution.gameIsLoad || SuperResolution.currentAlgorithm == null || Minecraft.getInstance().level == null) {
            ImGui.end();
            return;
        }

        if (AlgorithmManager.param.currentProjectionMatrix != null) {
            ImGui.text("%s %s %s %s".formatted(
                    String.valueOf(AlgorithmManager.param.currentProjectionMatrix.m00()),
                    String.valueOf(AlgorithmManager.param.currentProjectionMatrix.m01()),
                    String.valueOf(AlgorithmManager.param.currentProjectionMatrix.m02()),
                    String.valueOf(AlgorithmManager.param.currentProjectionMatrix.m03())
            ));
            ImGui.text("%s %s %s %s".formatted(
                    String.valueOf(AlgorithmManager.param.currentProjectionMatrix.m10()),
                    String.valueOf(AlgorithmManager.param.currentProjectionMatrix.m11()),
                    String.valueOf(AlgorithmManager.param.currentProjectionMatrix.m12()),
                    String.valueOf(AlgorithmManager.param.currentProjectionMatrix.m13())
            ));
            ImGui.text("%s %s %s %s".formatted(
                    String.valueOf(AlgorithmManager.param.currentProjectionMatrix.m20()),
                    String.valueOf(AlgorithmManager.param.currentProjectionMatrix.m21()),
                    String.valueOf(AlgorithmManager.param.currentProjectionMatrix.m22()),
                    String.valueOf(AlgorithmManager.param.currentProjectionMatrix.m23())
            ));
            ImGui.text("%s %s %s %s".formatted(
                    String.valueOf(AlgorithmManager.param.currentProjectionMatrix.m30()),
                    String.valueOf(AlgorithmManager.param.currentProjectionMatrix.m31()),
                    String.valueOf(AlgorithmManager.param.currentProjectionMatrix.m32()),
                    String.valueOf(AlgorithmManager.param.currentProjectionMatrix.m33())
            ));
        }
        if (AlgorithmManager.param.currentModelViewMatrix != null) {
            ImGui.text("%s %s %s %s".formatted(
                    String.valueOf(AlgorithmManager.param.currentModelViewMatrix.m00()),
                    String.valueOf(AlgorithmManager.param.currentModelViewMatrix.m01()),
                    String.valueOf(AlgorithmManager.param.currentModelViewMatrix.m02()),
                    String.valueOf(AlgorithmManager.param.currentModelViewMatrix.m03())
            ));
            ImGui.text("%s %s %s %s".formatted(
                    String.valueOf(AlgorithmManager.param.currentModelViewMatrix.m10()),
                    String.valueOf(AlgorithmManager.param.currentModelViewMatrix.m11()),
                    String.valueOf(AlgorithmManager.param.currentModelViewMatrix.m12()),
                    String.valueOf(AlgorithmManager.param.currentModelViewMatrix.m13())
            ));
            ImGui.text("%s %s %s %s".formatted(
                    String.valueOf(AlgorithmManager.param.currentModelViewMatrix.m20()),
                    String.valueOf(AlgorithmManager.param.currentModelViewMatrix.m21()),
                    String.valueOf(AlgorithmManager.param.currentModelViewMatrix.m22()),
                    String.valueOf(AlgorithmManager.param.currentModelViewMatrix.m23())
            ));
            ImGui.text("%s %s %s %s".formatted(
                    String.valueOf(AlgorithmManager.param.currentModelViewMatrix.m30()),
                    String.valueOf(AlgorithmManager.param.currentModelViewMatrix.m31()),
                    String.valueOf(AlgorithmManager.param.currentModelViewMatrix.m32()),
                    String.valueOf(AlgorithmManager.param.currentModelViewMatrix.m33())
            ));
        }
        ImGui.text("outFramebuffer " + AlgorithmManager.helper.getScreenWidth() + " " + AlgorithmManager.helper.getScreenHeight());
        ImGui.image(SuperResolution.currentAlgorithm.getOutputTextureId(),
                width,
                height,
                0, 1, 1, 0);

        ImGui.text("inFramebuffer " + AlgorithmManager.helper.getRenderWidth() + " " + AlgorithmManager.helper.getRenderHeight());
        ImGui.image(SuperResolution.currentAlgorithm.getInputTextureId(),
                width,
                height,
                0, 1, 1, 0);
        ImGui.text("inFramebufferD " + AlgorithmManager.helper.getRenderWidth() + " " + AlgorithmManager.helper.getRenderHeight());
        ImGui.image(SuperResolution.currentAlgorithm.getInputFrameBuffer().getDepthTextureId(),
                width,
                height,
                0, 1, 1, 0);
        ImGui.text("MainRenderTarget " + AlgorithmManager.helper.getScreenWidth() + " " + AlgorithmManager.helper.getScreenHeight());
        ImGui.image(Minecraft.getInstance().getMainRenderTarget().getColorTextureId(),
                width,
                height,
                0, 1, 1, 0);
        MinecraftRenderingStates.minecraftRenderTargetMap.forEach((String key, RenderTarget renderTarget) -> {
            ImGui.text(key + " " + renderTarget.width + " " + renderTarget.height);
            ImGui.image(renderTarget.getColorTextureId(),
                    width,
                    height,
                    0, 1, 1, 0);
        });
        ImGui.end();
    }
}