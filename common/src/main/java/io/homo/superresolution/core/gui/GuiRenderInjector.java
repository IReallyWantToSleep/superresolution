/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
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

package io.homo.superresolution.core.gui;

import com.mojang.blaze3d.vertex.VertexConsumer;
import io.homo.superresolution.common.gui.CustomActionRenderPipeline;
import io.homo.superresolution.common.mixin.gui.GuiGraphicsAccessor;
import io.homo.superresolution.core.gui.b3d.B3DGuiFrameInput;
import io.homo.superresolution.core.gui.b3d.B3DGuiVulkanUiRenderer;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;
#if MC_VER < MC_26_1
import net.minecraft.client.gui.GuiGraphics;
#endif

public final class GuiRenderInjector {
    private GuiRenderInjector() {
    }

    #if MC_VER > MC_1_21_11
    public static void submit(net.minecraft.client.gui.GuiGraphicsExtractor guiGraphics, Runnable renderAction)
    #else
    public static void submit(GuiGraphics guiGraphics, Runnable renderAction)
    #endif
    {
        #if MC_VER > MC_1_21_5
        ((GuiGraphicsAccessor) guiGraphics)
                #if MC_VER > MC_1_21_11
                .getGuiRenderState().addGuiElement
                #else
                .getGuiRenderState().submitGuiElement
                #endif
                        (
                                #if MC_VER > MC_1_21_11
                                new net.minecraft.client.renderer.state.gui.GuiElementRenderState()
                                #else
                                new net.minecraft.client.gui.render.state.GuiElementRenderState()
                                #endif
                                {
                                    @Override
                                    public @Nullable net.minecraft.client.gui.navigation.ScreenRectangle bounds() {
                                        return new net.minecraft.client.gui.navigation.ScreenRectangle(
                                                0,
                                                0,
                                                10000,
                                                10000
                                        );
                                    }

                                    public void buildVertices(VertexConsumer vertexConsumer, float v) {
                                        vertexConsumer.addVertex(0, 0, 0).setUv(0, 0).setColor(0, 0, 0, 0);
                                        vertexConsumer.addVertex(0, 0, 0).setUv(0, 0).setColor(0, 0, 0, 0);
                                        vertexConsumer.addVertex(0, 0, 0).setUv(0, 0).setColor(0, 0, 0, 0);
                                        vertexConsumer.addVertex(0, 0, 0).setUv(0, 0).setColor(0, 0, 0, 0);
                                    }

                                    // For 1.21.11+
                                    public void buildVertices(VertexConsumer vertexConsumer) {
                                        buildVertices(vertexConsumer, 0);
                                    }

                                    @Override
                                    public com.mojang.blaze3d.pipeline.RenderPipeline pipeline() {
                                        return new CustomActionRenderPipeline(renderAction);
                                    }

                                    @Override
                                    public net.minecraft.client.gui.render.TextureSetup textureSetup() {
                                        return net.minecraft.client.gui.render.TextureSetup.noTexture();
                                    }

                                    @Override
                                    public @Nullable net.minecraft.client.gui.navigation.ScreenRectangle scissorArea() {
                                        return new net.minecraft.client.gui.navigation.ScreenRectangle(
                                                0,
                                                0,
                                                10000,
                                                10000
                                        );
                                    }
                                }
                        );
        #else
        renderAction.run();
        #endif
    }

    #if MC_VER >= MC_26_2
    public static void submitB3DVulkanBlit(net.minecraft.client.gui.GuiGraphicsExtractor guiGraphics, B3DGuiFrameInput input) {
        B3DGuiVulkanUiRenderer.ensureTarget(input.framebufferWidth(), input.framebufferHeight());
        io.homo.superresolution.core.gui.b3d.B3DGuiVulkanTarget target = B3DGuiVulkanUiRenderer.target();
        if (target == null) {
            return;
        }
        int guiWidth = Math.max(1, Minecraft.getInstance().getWindow().getGuiScaledWidth());
        int guiHeight = Math.max(1, Minecraft.getInstance().getWindow().getGuiScaledHeight());
        ((GuiGraphicsAccessor) guiGraphics).getGuiRenderState().addBlitToCurrentLayer(
                new net.minecraft.client.renderer.state.gui.BlitRenderState(
                        net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA,
                        net.minecraft.client.gui.render.TextureSetup.singleTexture(target.textureView(), target.sampler()),
                        new org.joml.Matrix3x2f(),
                        0,
                        0,
                        guiWidth,
                        guiHeight,
                        0.0f,
                        1.0f,
                        1.0f,
                        0.0f,
                        -1,
                        null,
                        new net.minecraft.client.gui.navigation.ScreenRectangle(
                                0,
                                0,
                                guiWidth,
                                guiHeight
                        )
                )
        );
    }
    #endif
}
