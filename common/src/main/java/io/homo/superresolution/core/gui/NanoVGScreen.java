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

import io.homo.superresolution.common.minecraft.MinecraftUtils;
import io.homo.superresolution.common.minecraft.MinecraftWindow;
import io.homo.superresolution.common.minecraft.handler.RenderHandlerManager;
import io.homo.superresolution.common.perf.PerformanceTracker;
import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.opengl.GlStates;
import io.homo.superresolution.core.gui.core.backends.nanovg.NanoVGBackend;
import io.homo.superresolution.core.gui.core.backends.nanovg.NanoVGContextWrapper;
import io.homo.superresolution.core.gui.core.backends.nanovg.NanoVGFont;
import io.homo.superresolution.core.gui.b3d.B3DGuiFrameInput;
import io.homo.superresolution.core.gui.b3d.B3DGuiVulkanTarget;
import io.homo.superresolution.core.gui.b3d.B3DGuiVulkanUiRenderer;
import io.homo.superresolution.core.utils.Color;
import io.homo.superresolution.thirdparty.nanovg.NanoVGRhiBridge;
import io.homo.superresolution.core.gui.core.UIInputState;
import io.homo.superresolution.core.gui.core.backends.nanovg.NanoVGRenderContext;
import io.homo.superresolution.core.gui.core.backends.render.GuiScaleManager;
import io.homo.superresolution.core.gui.core.backends.render.RenderContext;
import org.joml.Vector2f;
import io.homo.superresolution.core.utils.MouseCursor;
import net.minecraft.client.Minecraft;
#if MC_VER < MC_26_1
import net.minecraft.client.gui.GuiGraphics;
#endif

#if MC_VER > MC_1_21_6 && false
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
#endif
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.opengl.GL41;
import org.lwjgl.opengl.GL42;

import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_ONE;
import static org.lwjgl.opengl.GL11.GL_ZERO;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL30.*;

public abstract class NanoVGScreen<T> extends WidgetEventScreen<T> {
    protected final NanoVGContextWrapper nvg;
    protected final GuiScaleManager scaleManager;

    protected NanoVGScreen(Component title) {
        super(title);
        nvg = NanoVGBackend.context;
        scaleManager = GuiScaleManager.getInstance();
        buildWidgets();
        
    }

    public NanoVGScreen setTransparent(boolean transparent) {
        return this;
    }

    protected abstract void buildWidgets();

    @Override
    public void onClose() {
        super.onClose();
        MouseCursor.ARROW.use();
    }

    #if MC_VER > MC_1_21_11
    @Override
    public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor guiGraphics, int mouseX_, int mouseY_, float partialTick)
    #else
    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX_, int mouseY_, float partialTick)
    #endif
    {
        float mouseX = (float) transformPos(mouseX_);
        float mouseY = (float) transformPos(mouseY_);
        drawBefore(guiGraphics, (int) mouseX, (int) mouseY, partialTick);
        scaleManager.update();
        #if MC_VER >= MC_26_2
        if (B3DGuiVulkanUiRenderer.isAvailable()) {
            Vector2f screenSize = MinecraftWindow.getWindowSize();
            B3DGuiFrameInput input = new B3DGuiFrameInput(
                    this,
                    mouseX,
                    mouseY,
                    partialTick,
                    scaleManager.guiScale(),
                    scaleManager.dpiScale(),
                    (int) screenSize.x,
                    (int) screenSize.y
            );
            B3DGuiVulkanUiRenderer.register(input);
            GuiRenderInjector.submitB3DVulkanBlit(guiGraphics, input);
            drawAfter(guiGraphics, (int) mouseX, (int) mouseY, partialTick);
            return;
        }
        #endif
        Runnable renderAction = () -> {
            GL41.glBindSampler(0, 0);
            boolean useRhi = NanoVGBackend.USE_RHI;
            boolean useVulkan = false;
            if (useVulkan) {
                NanoVGRhiBridge.beginBatch(RenderSystems.vulkan().device());
            } else if (useRhi) {
                NanoVGRhiBridge.beginBatch(RenderSystems.current().device());
            }

            boolean frameBegun = false;
            try {
                nvg.begin(true);
                if (useRhi) {
                    NanoVGRhiBridge.setTargetFramebuffer(nvg.frameBuffer);
                }


                frameBegun = true;
                nvg.resetGlobalTransform();
                nvg.resetTransform();

                nvg.globalScale(scaleManager.guiScale());
                nvg.globalAlpha(1.0f);
                NanoVGRenderContext ctx = new NanoVGRenderContext(nvg);
                ctx.setGuiScale(scaleManager.guiScale());
                ctx.setDpiScale(scaleManager.dpiScale());
                Vector2f screenSize = MinecraftWindow.getWindowSize();
                ctx.setViewportSize(screenSize.x / scaleManager.guiScale(), screenSize.y / scaleManager.guiScale());
                PerformanceTracker.push("GUI");
                draw(ctx, new UIInputState(
                        new Vector2f(mouseX, mouseY),
                        PerformanceTracker.getLastResultCPU("Frame") / 1_000_000f
                ));
                ctx.flush();
                PerformanceTracker.pop("GUI");
            } finally {
                try {
                    if (frameBegun && !useRhi) {
                        nvg.end();
                    }
                } finally {
                    if (useRhi) {
                        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, (int) nvg.frameBuffer.handle());
                        nvg.rawContext.endFrame();
                        NanoVGRhiBridge.endBatch();
                        glBindFramebuffer(GL_READ_FRAMEBUFFER, (int) nvg.frameBuffer.handle());
                        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, (int) RenderHandlerManager.getOriginRenderTarget().handle());
                        glEnable(GL_BLEND);
                        GL42.glBlendFuncSeparate(GL_ONE, GL_ZERO, GL_ZERO, GL_ONE);

                        glBlitFramebuffer(
                                0,
                                0,
                                nvg.frameBuffer.getWidth(),
                                nvg.frameBuffer.getHeight(),
                                0,
                                0,
                                MinecraftUtils.getMainRenderTarget().width,
                                MinecraftUtils.getMainRenderTarget().height,
                                GL_COLOR_BUFFER_BIT,
                                GL_LINEAR
                        );

                        GlStates.pop("nanovg-frame").restore();
                    }
                }
            }
        };
        GuiRenderInjector.submit(guiGraphics, renderAction);

        drawAfter(guiGraphics, (int) mouseX, (int) mouseY, partialTick);
    }

    #if MC_VER >= MC_26_2
    public void preRender(B3DGuiVulkanTarget target, B3DGuiFrameInput input) {
        scaleManager.update();
        boolean frameBegun = false;
        try {
            nvg.beginB3DVulkan(target);
            frameBegun = true;
            nvg.resetGlobalTransform();
            nvg.resetTransform();

            nvg.globalScale(scaleManager.guiScale());
            nvg.globalAlpha(1.0f);
            NanoVGRenderContext ctx = new NanoVGRenderContext(nvg);
            ctx.setGuiScale(scaleManager.guiScale());
            ctx.setDpiScale(scaleManager.dpiScale());
            Vector2f screenSize = MinecraftWindow.getWindowSize();
            ctx.setViewportSize(screenSize.x / scaleManager.guiScale(), screenSize.y / scaleManager.guiScale());
            PerformanceTracker.push("GUI");
            draw(ctx, new UIInputState(
                    new Vector2f(input.mouseX(), input.mouseY()),
                    PerformanceTracker.getLastResultCPU("Frame") / 1_000_000f
            ));
            ctx.flush();
            PerformanceTracker.pop("GUI");
        } finally {
            if (frameBegun) {
                nvg.rawContext.endFrame();
            }
        }
    }
    #endif

    @Override
    protected void init() {
        super.init();
    }

    #if MC_VER > MC_1_21_10
    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        scaleManager.update();
    }

    #else
    @Override
    public void resize(@NotNull Minecraft minecraft, int width, int height) {
        super.resize(minecraft, width, height);
        scaleManager.update();
    }
    #endif

    public void draw(RenderContext ctx, UIInputState inputState) {
        drawBefore(ctx, inputState);
        drawWidgets(ctx, inputState);
        drawTooltips(ctx, inputState);
        drawAfter(ctx, inputState);
    }

    public void drawTooltips(RenderContext ctx, UIInputState inputState) {
    }

    public void drawAfter(RenderContext ctx, UIInputState inputState) {

    }

    public void drawBefore(RenderContext ctx, UIInputState inputState) {

    }

    #if MC_VER > MC_1_21_11
    public void drawAfter(net.minecraft.client.gui.GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {

    }

    public void drawBefore(net.minecraft.client.gui.GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {

    }
    #else
    public void drawAfter(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {

    }

    public void drawBefore(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        //super.renderBackground(guiGraphics);
    }
    #endif

    public void drawWidgets(RenderContext ctx, UIInputState inputState) {
        Vector2f screenSize = MinecraftWindow.getWindowSize();
        view.setViewport(screenSize.x / scaleManager.guiScale(), screenSize.y / scaleManager.guiScale());
        view.render(ctx, inputState);
    }
    @Override
    protected double transformPos(double pos) {
        return (Minecraft.getInstance().getWindow().getGuiScale() * pos) / nvg.globalScale();
    }
}
