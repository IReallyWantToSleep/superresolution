package io.homo.superresolution.debug.imgui;

import imgui.ImGui;
import io.homo.superresolution.SuperResolution;
import io.homo.superresolution.upscale.AlgorithmManager;
import io.homo.superresolution.upscale.AlgorithmType;
import io.homo.superresolution.upscale.fsr2.FSR2;
import net.minecraft.client.Minecraft;

public class ImGuiLayer {
    public void imgui() {
        if (!SuperResolution.gameIsLoad) return;
        int width = 500;
        float height = (((float) width) / SuperResolution.getMinecraftWidth()) * SuperResolution.getMinecraftHeight();
        ImGui.begin("DEBUG");
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


        if (SuperResolution.algorithmType == AlgorithmType.FSR2) {
            ImGui.text("mvFramebuffer " + AlgorithmManager.helper.getScreenWidth() + " " + AlgorithmManager.helper.getScreenHeight());
            ImGui.image(FSR2.helper.getMotionVectorsTex(),
                    width,
                    height,
                    0, 1, 1, 0);
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
        if (Minecraft.getInstance().level != null) {
            ImGui.text("entityTarget " + AlgorithmManager.helper.getScreenWidth() + " " + AlgorithmManager.helper.getScreenHeight());
            if (Minecraft.getInstance().levelRenderer.entityTarget() != null)
                ImGui.image(Minecraft.getInstance().levelRenderer.entityTarget().getColorTextureId(),
                        width,
                        height,
                        0, 1, 1, 0);

            ImGui.text("ParticlesTarget " + AlgorithmManager.helper.getScreenWidth() + " " + AlgorithmManager.helper.getScreenHeight());
            if (Minecraft.getInstance().levelRenderer.getParticlesTarget() != null)
                ImGui.image(Minecraft.getInstance().levelRenderer.getParticlesTarget().getColorTextureId(),
                        width,
                        height,
                        0, 1, 1, 0);
            ImGui.text("ItemEntityTarget " + AlgorithmManager.helper.getScreenWidth() + " " + AlgorithmManager.helper.getScreenHeight());
            if (Minecraft.getInstance().levelRenderer.getItemEntityTarget() != null)
                ImGui.image(Minecraft.getInstance().levelRenderer.getItemEntityTarget().getColorTextureId(),
                        width,
                        height,
                        0, 1, 1, 0);
            ImGui.text("TranslucentTarget " + AlgorithmManager.helper.getScreenWidth() + " " + AlgorithmManager.helper.getScreenHeight());
            if (Minecraft.getInstance().levelRenderer.getTranslucentTarget() != null)
                ImGui.image(Minecraft.getInstance().levelRenderer.getTranslucentTarget().getColorTextureId(),
                        width,
                        height,
                        0, 1, 1, 0);
            ImGui.text("CloudsTarget " + AlgorithmManager.helper.getScreenWidth() + " " + AlgorithmManager.helper.getScreenHeight());
            if (Minecraft.getInstance().levelRenderer.getCloudsTarget() != null)
                ImGui.image(Minecraft.getInstance().levelRenderer.getCloudsTarget().getColorTextureId(),
                        width,
                        height,
                        0, 1, 1, 0);
            ImGui.text("WeatherTarget " + AlgorithmManager.helper.getScreenWidth() + " " + AlgorithmManager.helper.getScreenHeight());
            if (Minecraft.getInstance().levelRenderer.getWeatherTarget() != null)
                ImGui.image(Minecraft.getInstance().levelRenderer.getWeatherTarget().getColorTextureId(),
                        width,
                        height,
                        0, 1, 1, 0);
        }

        ImGui.end();
    }
}