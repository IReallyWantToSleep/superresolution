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
import io.homo.superresolution.api.SuperResolutionAPI;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.minecraft.handler.MinecraftRenderHandler;
import io.homo.superresolution.common.minecraft.handler.RenderHandlerManager;
import io.homo.superresolution.common.upscale.AlgorithmDescriptions;
import io.homo.superresolution.common.upscale.fsr2.FSR2;
import io.homo.superresolution.core.graphics.opengl.buffer.GlBuffer;
import io.homo.superresolution.core.graphics.impl.framebuffer.FrameBufferAttachmentType;
import io.homo.superresolution.common.upscale.AlgorithmManager;
import io.homo.superresolution.common.upscale.OpticalFlowMotionVectorsGenerator;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.shadercompat.ShaderCompatUpscaleDispatcher;
import io.homo.superresolution.thirdparty.fsr2.common.Fsr2Context;
import io.homo.superresolution.thirdparty.fsr2.common.Fsr2PipelineResourceType;
import io.homo.superresolution.thirdparty.fsr2.common.Fsr2PipelineResources;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;

import java.util.Map;

public class ImGuiLayer {

    private static final int PREVIEW_WIDTH = 500;

    public float[] blurPhi = new float[1];
    public float[] blurM = new float[1];

    public void imgui() {
        float previewHeight = ((float) PREVIEW_WIDTH / RenderHandlerManager.getScreenWidth()) * RenderHandlerManager.getScreenHeight();

        ImGui.begin("DEBUG");
        drawCaptureButtons();
        if (!SuperResolution.gameIsLoad || SuperResolution.currentAlgorithm == null || Minecraft.getInstance().level == null) {
            ImGui.end();
            return;
        }
        drawProjectionMatrix();
        drawDebugTextures(previewHeight);
        drawFsr2Resources(previewHeight);

        ImGui.end();
    }


    private void drawCaptureButtons() {
        if (ImGui.button("Capture")) SuperResolutionAPI.debugRenderdocCapture();
        if (ImGui.button("CaptureUpscale")) SuperResolutionAPI.debugRenderdocCaptureUpscale();
        if (ImGui.button("CaptureVulkan")) SuperResolutionAPI.debugRenderdocCaptureVulkan();
        if (ImGui.button("TriggerCapture")) SuperResolutionAPI.debugRenderdocTriggerCapture();
    }


    private void drawProjectionMatrix() {
        Matrix4f m = AlgorithmManager.param.currentProjectionMatrix;
        if (m == null) return;

        float[] values = new float[16];
        m.get(values);
        for (int row = 0; row < 4; row++) {
            ImGui.text(String.format("%f %f %f %f",
                    values[row * 4], values[row * 4 + 1],
                    values[row * 4 + 2], values[row * 4 + 3]));
        }
    }

    private void drawDebugTextures(float height) {
        drawImageIfExists("out", ShaderCompatUpscaleDispatcher.debugInfo.get("out"),
                RenderHandlerManager.getScreenWidth(), RenderHandlerManager.getScreenHeight(), height);

        drawImageIfExists("color", ShaderCompatUpscaleDispatcher.debugInfo.get("color"),
                RenderHandlerManager.getRenderWidth(), RenderHandlerManager.getRenderHeight(), height);

        drawImageIfExists("colorA", ShaderCompatUpscaleDispatcher.debugInfo.get("colora"),
                RenderHandlerManager.getRenderWidth(), RenderHandlerManager.getRenderHeight(), height);

        drawImage("outFramebuffer", SuperResolution.currentAlgorithm.getOutputTextureId(),
                RenderHandlerManager.getScreenWidth(), RenderHandlerManager.getScreenHeight(), height);

        if (RenderHandlerManager.getColorTexture() != null) {
            drawImage("colorTexture", (int) RenderHandlerManager.getColorTexture().handle(),
                    RenderHandlerManager.getRenderWidth(), RenderHandlerManager.getRenderHeight(), height);
        }
        if (RenderHandlerManager.getDepthTexture() != null) {
            drawImage("depthTexture", (int) RenderHandlerManager.getDepthTexture().handle(),
                    RenderHandlerManager.getRenderWidth(), RenderHandlerManager.getRenderHeight(), height);
        }

        drawImage("MainRenderTarget", (int) RenderHandlerManager.getOriginRenderTarget().handle(),
                RenderHandlerManager.getScreenWidth(), RenderHandlerManager.getScreenHeight(), height);

        drawImage("mv", AlgorithmManager.getMotionVectorsFrameBuffer().getTextureId(FrameBufferAttachmentType.Color),
                RenderHandlerManager.getRenderWidth(), RenderHandlerManager.getRenderHeight(), height);

        if (OpticalFlowMotionVectorsGenerator.gradFrameBuffer != null) {
            drawImage("mv grad", OpticalFlowMotionVectorsGenerator.gradFrameBuffer.getTextureId(FrameBufferAttachmentType.Color),
                    RenderHandlerManager.getRenderWidth(), RenderHandlerManager.getRenderHeight(), height);
        }
        if (OpticalFlowMotionVectorsGenerator.deltaFrameBuffer != null) {
            drawImage("mv delta", OpticalFlowMotionVectorsGenerator.deltaFrameBuffer.getTextureId(FrameBufferAttachmentType.Color),
                    RenderHandlerManager.getRenderWidth(), RenderHandlerManager.getRenderHeight(), height);
        }

        if (OpticalFlowMotionVectorsGenerator.currentFrameTexture != null)
            drawImage("mv c", (int) OpticalFlowMotionVectorsGenerator.currentFrameTexture.handle(),
                    RenderHandlerManager.getRenderWidth(), RenderHandlerManager.getRenderHeight(), height);
        if (OpticalFlowMotionVectorsGenerator.previousFrameTexture != null)
            drawImage("mv pt", (int) OpticalFlowMotionVectorsGenerator.previousFrameTexture.handle(),
                    RenderHandlerManager.getRenderWidth(), RenderHandlerManager.getRenderHeight(), height);
        if (OpticalFlowMotionVectorsGenerator.preprocessFrameBuffer != null)
            drawImage("mv pf", OpticalFlowMotionVectorsGenerator.preprocessFrameBuffer.getTextureId(FrameBufferAttachmentType.Color),
                    RenderHandlerManager.getRenderWidth(), RenderHandlerManager.getRenderHeight(), height);
    }

    private void drawFsr2Resources(float height) {
        if (SuperResolutionConfig.getUpscaleAlgorithm() != AlgorithmDescriptions.FSR2
                || !(SuperResolution.getCurrentAlgorithm() instanceof FSR2 fsr2)) return;

        Fsr2Context context = fsr2.fsr2Context;
        for (Map.Entry<Fsr2PipelineResourceType, Fsr2PipelineResources.Fsr2ResourceEntry> entry : context.resources.resources().entrySet()) {
            if (entry.getValue().getResource() == null || entry.getValue().getResource() instanceof GlBuffer) continue;

            ITexture texture = (ITexture) entry.getValue().getResource();
            drawImage(entry.getValue().getDescription().label, (int) texture.handle(),
                    texture.getWidth(), texture.getHeight(), height);
        }
    }


    private void drawImage(String label, int textureId, int texWidth, int texHeight, float previewHeight) {
        ImGui.text(label + " " + texWidth + " " + texHeight);
        ImGui.image(textureId, PREVIEW_WIDTH, previewHeight, 0, 1, 1, 0);
    }

    private void drawImageIfExists(String label, Object handle, int texWidth, int texHeight, float previewHeight) {
        if (handle == null) return;
        if (handle instanceof Integer id) {
            drawImage(label, id, texWidth, texHeight, previewHeight);
        } else if (handle instanceof Long id) {
            drawImage(label, (int) (long) id, texWidth, texHeight, previewHeight);
        }
    }
}
