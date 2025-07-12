package io.homo.superresolution.common.debug.imgui;

import imgui.ImGui;
import io.homo.superresolution.api.SuperResolutionAPI;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.minecraft.MinecraftRenderHandle;
import io.homo.superresolution.common.upscale.AlgorithmDescriptions;
import io.homo.superresolution.common.upscale.fsr2.FSR2;
import io.homo.superresolution.core.graphics.opengl.buffer.GlBuffer;
import io.homo.superresolution.core.graphics.impl.framebuffer.FrameBufferAttachmentType;
import io.homo.superresolution.common.upscale.AlgorithmManager;
import io.homo.superresolution.common.upscale.MotionVectorsGenerator;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.fsr2.v221.Fsr2Context;
import io.homo.superresolution.fsr2.v221.Fsr2PipelineResourceType;
import io.homo.superresolution.fsr2.v221.Fsr2PipelineResources;
import net.minecraft.client.Minecraft;

import java.util.Map;

public class ImGuiLayer {
    public float[] blurPhi = new float[1];
    public float[] blurM = new float[1];


    public void imgui() {
        int width = 500;
        float height = (((float) width) / MinecraftRenderHandle.getScreenWidth()) * MinecraftRenderHandle.getScreenHeight();
        ImGui.begin("DEBUG");
        if (ImGui.button("Capture")) {
            SuperResolutionAPI.debugRenderdocCapture();
        }
        if (ImGui.button("CaptureUpscale")) {
            SuperResolutionAPI.debugRenderdocCaptureUpscale();

        }
        if (ImGui.button("CaptureVulkan")) {
            SuperResolutionAPI.debugRenderdocCaptureVulkan();

        }
        if (ImGui.button("TriggerCapture")) {
            SuperResolutionAPI.debugRenderdocTriggerCapture();
        }
        ImGui.sliderFloat("blurPhi", blurPhi, 0, 16);
        ImGui.sliderFloat("blurM", blurM, 0, 1);
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
        if (AlgorithmManager.param.currentModelViewProjectionMatrix != null) {
            ImGui.text("%s %s %s %s".formatted(
                    String.valueOf(AlgorithmManager.param.currentModelViewProjectionMatrix.m00()),
                    String.valueOf(AlgorithmManager.param.currentModelViewProjectionMatrix.m01()),
                    String.valueOf(AlgorithmManager.param.currentModelViewProjectionMatrix.m02()),
                    String.valueOf(AlgorithmManager.param.currentModelViewProjectionMatrix.m03())
            ));
            ImGui.text("%s %s %s %s".formatted(
                    String.valueOf(AlgorithmManager.param.currentModelViewProjectionMatrix.m10()),
                    String.valueOf(AlgorithmManager.param.currentModelViewProjectionMatrix.m11()),
                    String.valueOf(AlgorithmManager.param.currentModelViewProjectionMatrix.m12()),
                    String.valueOf(AlgorithmManager.param.currentModelViewProjectionMatrix.m13())
            ));
            ImGui.text("%s %s %s %s".formatted(
                    String.valueOf(AlgorithmManager.param.currentModelViewProjectionMatrix.m20()),
                    String.valueOf(AlgorithmManager.param.currentModelViewProjectionMatrix.m21()),
                    String.valueOf(AlgorithmManager.param.currentModelViewProjectionMatrix.m22()),
                    String.valueOf(AlgorithmManager.param.currentModelViewProjectionMatrix.m23())
            ));
            ImGui.text("%s %s %s %s".formatted(
                    String.valueOf(AlgorithmManager.param.currentModelViewProjectionMatrix.m30()),
                    String.valueOf(AlgorithmManager.param.currentModelViewProjectionMatrix.m31()),
                    String.valueOf(AlgorithmManager.param.currentModelViewProjectionMatrix.m32()),
                    String.valueOf(AlgorithmManager.param.currentModelViewProjectionMatrix.m33())
            ));
        }
        ImGui.text("outFramebuffer " + MinecraftRenderHandle.getScreenWidth() + " " + MinecraftRenderHandle.getScreenHeight());
        ImGui.image(SuperResolution.currentAlgorithm.getOutputTextureId(),
                width,
                height,
                0, 1, 1, 0);


        if (SuperResolution.currentAlgorithm.getInputFrameBuffer() != null) {
            ImGui.text("inFramebuffer " + MinecraftRenderHandle.getRenderWidth() + " " + MinecraftRenderHandle.getRenderHeight());
            ImGui.image(SuperResolution.currentAlgorithm.getInputTextureId(),
                    width,
                    height,
                    0, 1, 1, 0);
            ImGui.text("inFramebufferD " + MinecraftRenderHandle.getRenderWidth() + " " + MinecraftRenderHandle.getRenderHeight());
            ImGui.image(SuperResolution.currentAlgorithm.getInputFrameBuffer().getTextureId(FrameBufferAttachmentType.Depth),
                    width,
                    height,
                    0, 1, 1, 0);
        }
        ImGui.text("MainRenderTarget " + MinecraftRenderHandle.getScreenWidth() + " " + MinecraftRenderHandle.getScreenHeight());
        ImGui.image(MinecraftRenderHandle.getOriginRenderTarget().handle(),
                width,
                height,
                0, 1, 1, 0);
        ImGui.text("mv " + MinecraftRenderHandle.getRenderWidth() + " " + MinecraftRenderHandle.getRenderHeight());
        ImGui.image(AlgorithmManager.getMotionVectorsFrameBuffer().getTextureId(FrameBufferAttachmentType.Color),
                width,
                height,
                0, 1, 1, 0);
        ImGui.text("mv grad " + MinecraftRenderHandle.getRenderWidth() + " " + MinecraftRenderHandle.getRenderHeight());
        ImGui.image(MotionVectorsGenerator.gradFrameBuffer.getTextureId(FrameBufferAttachmentType.Color),
                width,
                height,
                0, 1, 1, 0);
        ImGui.text("mv delta " + MinecraftRenderHandle.getRenderWidth() + " " + MinecraftRenderHandle.getRenderHeight());
        ImGui.image(MotionVectorsGenerator.deltaFrameBuffer.getTextureId(FrameBufferAttachmentType.Color),
                width,
                height,
                0, 1, 1, 0);
        ImGui.text("mv c " + MinecraftRenderHandle.getRenderWidth() + " " + MinecraftRenderHandle.getRenderHeight());
        ImGui.image(MotionVectorsGenerator.currentFrameTexture.handle(),
                width,
                height,
                0, 1, 1, 0);
        ImGui.text("mv pt " + MinecraftRenderHandle.getRenderWidth() + " " + MinecraftRenderHandle.getRenderHeight());
        ImGui.image(MotionVectorsGenerator.previousFrameTexture.handle(),
                width,
                height,
                0, 1, 1, 0);
        ImGui.text("mv pf " + MinecraftRenderHandle.getRenderWidth() + " " + MinecraftRenderHandle.getRenderHeight());
        ImGui.image(MotionVectorsGenerator.preprocessFrameBuffer.getTextureId(FrameBufferAttachmentType.Color),
                width,
                height,
                0, 1, 1, 0);


        if (SuperResolutionConfig.getUpscaleAlgorithm() == AlgorithmDescriptions.FSR2 && SuperResolution.getCurrentAlgorithm() instanceof FSR2) {
            Fsr2Context context = ((FSR2) SuperResolution.getCurrentAlgorithm()).fsr2Context;
            for (Map.Entry<Fsr2PipelineResourceType, Fsr2PipelineResources.Fsr2ResourceEntry> entry : context.resources.resources().entrySet()) {
                if (entry.getValue().getResource() == null || (entry.getValue().getResource() instanceof GlBuffer))
                    continue;
                ITexture texture = (ITexture) entry.getValue().getResource();
                ImGui.text(entry.getValue().getDescription().label + " " + texture.getWidth() + " " + texture.getHeight());
                ImGui.image(texture.handle(),
                        width,
                        height,
                        0, 1, 1, 0);
            }
        }
        ImGui.end();
    }
}