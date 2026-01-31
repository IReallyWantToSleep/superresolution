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

import io.homo.superresolution.common.minecraft.MinecraftWindow;
import io.homo.superresolution.core.gui.core.backends.nanovg.NanoVG;
import io.homo.superresolution.core.gui.core.backends.nanovg.NanoVGContextWrapper;
import io.homo.superresolution.core.gui.core.UIInputState;
import io.homo.superresolution.core.gui.core.backends.nanovg.NanoVGRenderContext;
import io.homo.superresolution.core.gui.core.backends.render.GuiScaleManager;
import io.homo.superresolution.core.gui.core.backends.render.RenderContext;
import io.homo.superresolution.core.gui.core.frame.Frame;
import io.homo.superresolution.core.gui.core.view.View;
import io.homo.superresolution.core.gui.core.AbstractWidget;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaNode;
import org.joml.Vector2f;
import io.homo.superresolution.core.utils.MouseCursor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
#if MC_VER >MC_1_21_6 && false
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
#endif
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public abstract class NanoVGScreen<T> extends Screen {
    protected final NanoVGContextWrapper nvg;
    protected final GuiScaleManager scaleManager;
    protected View view = new View();
    protected Frame defaultFrame = new Frame();
    protected YogaNode defaultFrameLayout;

    protected NanoVGScreen(Component title) {
        super(title);
        nvg = NanoVG.context;
        scaleManager = GuiScaleManager.getInstance();
        defaultFrameLayout = view.addFrame(defaultFrame);
        defaultFrameLayout.setWidthPercent(100);
        defaultFrameLayout.setHeightPercent(100);
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

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX_, int mouseY_, float partialTick) {
        float mouseX = (float) transformPos(mouseX_);
        float mouseY = (float) transformPos(mouseY_);
        drawBefore(guiGraphics, (int) mouseX, (int) mouseY, partialTick);
        scaleManager.update();

        nvg.begin(true);
        nvg.resetGlobalTransform();
        nvg.resetTransform();
        nvg.globalScale(scaleManager.guiScale());
        nvg.globalAlpha(1.0f);

        NanoVGRenderContext ctx = new NanoVGRenderContext(nvg);
        ctx.setGuiScale(scaleManager.guiScale());
        ctx.setDpiScale(scaleManager.dpiScale());

        draw(ctx, new UIInputState(
                new Vector2f(mouseX, mouseY),
                partialTick
        ));

        ctx.flush();
        nvg.end();
        drawAfter(guiGraphics, (int) mouseX, (int) mouseY, partialTick);
    }

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

    public void drawAfter(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {

    }

    public void drawBefore(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        //super.renderBackground(guiGraphics, mouseX, mouseY, delta);
    }

    public void drawWidgets(RenderContext ctx, UIInputState inputState) {
        Vector2f screenSize = MinecraftWindow.getWindowSize();
        view.setViewport(screenSize.x / scaleManager.guiScale(), screenSize.y / scaleManager.guiScale());

        defaultFrame.updateHitTestDebug(inputState.mousePosition());

        view.render(ctx, inputState);
    }

    protected void setRoot(AbstractWidget<?> root) {
        this.defaultFrame.setRoot(root);
    }

    protected void enableDebugRender(boolean enabled) {
        view.setDebugRenderEnabled(enabled);
    }

    protected void setDebugBoundsVisible(boolean layout, boolean render, boolean hitTest) {
        view.setDebugBoundsVisible(layout, render, hitTest);
    }

    protected View getView() {
        return view;
    }

    protected Frame getDefaultFrame() {
        return defaultFrame;
    }

    protected void dispatchMouseMoveToFrame(float x, float y) {
        view.dispatchMouseMove(x, y);
    }

    protected void dispatchMousePressToFrame(float x, float y, int button) {
        view.dispatchMousePress(x, y, button);
    }

    protected void dispatchMouseReleaseToFrame(float x, float y, int button) {
        view.dispatchMouseRelease(x, y, button);
    }

    protected void dispatchMouseScrollToFrame(float x, float y, double scroll) {
        view.dispatchMouseScroll(x, y, scroll);
    }

    protected void dispatchMouseDragToFrame(float mouseX, float mouseY, float dragX, float dragY, int button) {
        view.dispatchMouseDrag(mouseX, mouseY, dragX, dragY, button);
    }

    protected void dispatchKeyPressToFrame(int keyCode, int scancode, int modifiers) {
        view.dispatchKeyPress(keyCode, scancode, modifiers);
    }

    protected void dispatchKeyReleaseToFrame(int keyCode, int scancode, int modifiers) {
        view.dispatchKeyRelease(keyCode, scancode, modifiers);
    }

    protected void dispatchCharTypedToFrame(char codePoint, int modifiers) {
        view.dispatchCharTyped(codePoint, modifiers);
    }

    #if MC_VER > MC_1_21_8
    @Override
    public boolean charTyped(net.minecraft.client.input.CharacterEvent event) {
        dispatchCharTypedToFrame(((char) event.codepoint()), event.modifiers());
        return true;
    }

    @Override
    public boolean keyReleased(net.minecraft.client.input.KeyEvent event) {
        dispatchKeyReleaseToFrame(event.key(), event.scancode(), event.modifiers());
        super.keyPressed(event);
        return true;
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        dispatchKeyPressToFrame(event.key(), event.scancode(), event.modifiers());
        super.keyPressed(event);
        return true;
    }

    @Override
    public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event, double dragX, double dragY) {
        dispatchMouseDragToFrame((float) transformPos(event.x()), (float) transformPos(event.y()), (float) transformPos(dragX), (float) transformPos(dragY), event.button());
        super.mouseDragged(event, dragX, dragY);
        return true;
    }

    @Override
    public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        dispatchMouseReleaseToFrame((float) transformPos(event.x()), (float) transformPos(event.y()), event.button());
        super.mouseReleased(event);
        return true;
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean idk) {
        dispatchMousePressToFrame((float) transformPos(event.x()), (float) transformPos(event.y()), event.button());
        super.mouseClicked(event, idk);
        return true;
    }

    #else
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        dispatchMousePressToFrame((float) transformPos(mouseX), (float) transformPos(mouseY), button);
        super.mouseClicked(mouseX, mouseY, button);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dispatchMouseReleaseToFrame((float) transformPos(mouseX), (float) transformPos(mouseY), button);
        super.mouseReleased(mouseX, mouseY, button);
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        dispatchMouseDragToFrame((float) transformPos(mouseX), (float) transformPos(mouseY), (float) transformPos(dragX), (float) transformPos(dragY), button);
        super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        return true;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        dispatchKeyReleaseToFrame(keyCode, scanCode, modifiers);
        super.keyReleased(keyCode, scanCode, modifiers);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        dispatchKeyPressToFrame(keyCode, scanCode, modifiers);
        super.keyPressed(keyCode, scanCode, modifiers);
        return true;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        dispatchCharTypedToFrame(codePoint, modifiers);
        return true;
    }
    #endif

    #if MC_VER > MC_1_20_1
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        dispatchMouseScrollToFrame((float) transformPos(mouseX), (float) transformPos(mouseY), transformPos(scrollY));
        super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        return true;
    }

    #else
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX) {
        dispatchMouseScrollToFrame((float) transformPos(mouseX), (float) transformPos(mouseY), transformPos(scrollX));
        super.mouseScrolled(mouseX, mouseY, scrollX);
        return true;
    }
    #endif


    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        dispatchMouseMoveToFrame((float) transformPos(mouseX), (float) transformPos(mouseY));
        super.mouseMoved(mouseX, mouseY);
    }


    protected double transformPos(double pos) {
        return (Minecraft.getInstance().getWindow().getGuiScale() * pos) / nvg.globalScale();
    }

    #if MC_VER <= MC_1_20_1
    @Override
    public void renderBackground(GuiGraphics guiGraphics) {
        return;
    }

    #else
    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        return;
    }
    #endif

}
