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

import io.homo.superresolution.core.gui.core.AbstractWidget;
import io.homo.superresolution.core.gui.core.frame.Frame;
import io.homo.superresolution.core.gui.core.view.View;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaNode;
#if MC_VER < MC_26_1
import net.minecraft.client.gui.GuiGraphics;
#endif
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public abstract class WidgetEventScreen<T> extends Screen {
    protected View view = new View();
    protected Frame defaultFrame = new Frame();
    protected YogaNode defaultFrameLayout;

    protected WidgetEventScreen(Component title) {
        super(title);
        defaultFrameLayout = view.addFrame(defaultFrame);
        defaultFrameLayout.setWidthPercent(100);
        defaultFrameLayout.setHeightPercent(100);
    }

    protected void setRoot(AbstractWidget<?> root) {
        defaultFrame.setRoot(root);
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

    protected abstract double transformPos(double pos);

    #if MC_VER > MC_1_21_8
    @Override
    public boolean charTyped(net.minecraft.client.input.CharacterEvent event) {
        #if MC_VER > MC_1_21_11
        dispatchCharTypedToFrame(((char) event.codepoint()), 0);
        #else
        dispatchCharTypedToFrame(((char) event.codepoint()), event.modifiers());
        #endif
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

    #if MC_VER <= MC_1_20_1
    @Override
    public void renderBackground(GuiGraphics guiGraphics) {
        return;
    }

    #else

    #if MC_VER > MC_1_21_11
    @Override
    public void extractMenuBackground(net.minecraft.client.gui.GuiGraphicsExtractor guiGraphics)
    {
        return;
    }

    @Override
    protected void extractBlurredBackground(net.minecraft.client.gui.GuiGraphicsExtractor graphics) {
        return;
    }
    #else
    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick)
    {
        return;
    }
    #endif
    #endif
}