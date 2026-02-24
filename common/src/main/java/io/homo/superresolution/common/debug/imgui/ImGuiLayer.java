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
import io.homo.superresolution.api.SuperResolutionAPI;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.minecraft.handler.RenderHandlerManager;
import io.homo.superresolution.common.upscale.AlgorithmDescriptions;
import io.homo.superresolution.common.upscale.DispatchResource;
import io.homo.superresolution.common.upscale.fsr2.FSR2;
import io.homo.superresolution.core.graphics.opengl.buffer.GlBuffer;
import io.homo.superresolution.core.graphics.impl.framebuffer.FrameBufferAttachmentType;
import io.homo.superresolution.common.upscale.AlgorithmManager;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.thirdparty.fsr2.common.Fsr2Context;
import io.homo.superresolution.thirdparty.fsr2.common.Fsr2PipelineResourceType;
import io.homo.superresolution.thirdparty.fsr2.common.Fsr2PipelineResources;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

public class ImGuiLayer {

    private static final int PREVIEW_WIDTH = 500;

    public float[] blurPhi = new float[1];
    public float[] blurM = new float[1];
    public static final float[] jitterScaleAlgo = new float[1];
    public static final float[] jitterScaleShader = new float[1];
    public static final int[] jitterOffsetFrameOffsetAlgo = new int[1];
    public static final int[] jitterOffsetFrameOffsetShader = new int[1];

    public static final float[] jitterOffset = new float[4];
    public void imgui() {
        float previewHeight = ((float) PREVIEW_WIDTH / RenderHandlerManager.getScreenWidth()) * RenderHandlerManager.getScreenHeight();

        ImGui.begin("DEBUG");
        drawCaptureButtons();
        ImGui.sliderFloat("jitterScaleAlgo",jitterScaleAlgo,-3,3);
        ImGui.sliderFloat("jitterScaleShader",jitterScaleShader,-3,3);
        ImGui.sliderInt("jitterOffsetFrameOffsetAlgo",jitterOffsetFrameOffsetAlgo,-3,3);
        ImGui.sliderInt("jitterOffsetFrameOffsetShader",jitterOffsetFrameOffsetShader,-3,3);

        ImGui.text("Shader Jitter: %.2f  %.2f".formatted(jitterOffset[0],jitterOffset[1]));
        ImGui.text("Algo Jitter: %.2f  %.2f".formatted(jitterOffset[2],jitterOffset[3]));

        ImGui.text("Jitter: %s  %s".formatted(
                Math.abs(jitterOffset[0] - jitterOffset[2]) < 1e-6,
                Math.abs(jitterOffset[1] - jitterOffset[3]) < 1e-6
                )
        );

        if (!SuperResolution.gameIsLoaded || SuperResolution.currentAlgorithm == null || Minecraft.getInstance().level == null) {
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

        if (RenderHandlerManager.getColorTexture() != null) {
            drawImage("Input Color Texture", (int) RenderHandlerManager.getColorTexture().handle(),
                    RenderHandlerManager.getRenderWidth(), RenderHandlerManager.getRenderHeight(), height);
        }
        if (RenderHandlerManager.getDepthTexture() != null) {
            drawImage("Input Depth Texture", (int) RenderHandlerManager.getDepthTexture().handle(),
                    RenderHandlerManager.getRenderWidth(), RenderHandlerManager.getRenderHeight(), height);
        }
        try {
            DispatchResource dispatchResource = (DispatchResource) Class.forName("io.homo.superresolution.shadercompat.IrisShaderCompatUpscaleDispatcher").getMethod("getDispatchResource", Class.forName("net.irisshaders.iris.pipeline.CompositeRenderer")).invoke(null, (Object) null);
            if (dispatchResource.resources().motionVectorsTexture() != null) {
                drawImage("Input Motion Vectors Texture", (int) dispatchResource.resources().motionVectorsTexture().handle(),
                        RenderHandlerManager.getRenderWidth(), RenderHandlerManager.getRenderHeight(), height);
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException |
                 ClassNotFoundException ignored) {
        }

        if (AlgorithmManager.getMotionVectorsFrameBuffer() != null)
            drawImage("Generated Motion Vectors", AlgorithmManager.getMotionVectorsFrameBuffer().getTextureId(FrameBufferAttachmentType.Color),
                    RenderHandlerManager.getRenderWidth(), RenderHandlerManager.getRenderHeight(), height);

        drawImage("Upscale Output", SuperResolution.currentAlgorithm.getOutputTextureId(),
                RenderHandlerManager.getScreenWidth(), RenderHandlerManager.getScreenHeight(), height);


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
