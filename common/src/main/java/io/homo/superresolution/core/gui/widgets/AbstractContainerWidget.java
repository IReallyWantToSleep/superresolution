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

package io.homo.superresolution.core.gui.widgets;

import io.homo.superresolution.core.gui.core.UIDrawContext;
import io.homo.superresolution.core.gui.core.UIInputState;
import io.homo.superresolution.core.gui.core.WidgetStyle;
import io.homo.superresolution.core.gui.core.animator.AnimationSet;
import io.homo.superresolution.core.gui.core.impl.Rectangle;
import io.homo.superresolution.core.gui.core.layout.ILayout;

import java.util.ArrayList;

public abstract class AbstractContainerWidget
        <T extends AbstractWidget<?, ?, ?>,
                STYLE extends WidgetStyle<?>,
                ANIM extends AnimationSet
                > extends AbstractWidget<T, STYLE, ANIM> {
    protected ILayout layout;
    protected ArrayList<AbstractWidget<?, ?, ?>> children = new ArrayList<>();
    protected STYLE style;

    public void addChildren(AbstractWidget<?, ?, ?> widget) {
        children.add(widget);
        layout.addElement(widget);
        widget.setParent(this);
    }

    public void removeChildren(AbstractWidget<?, ?, ?> widget) {
        children.remove(widget);
        layout.addElement(widget);
        widget.setParent(null);
    }

    public boolean hasChildren(AbstractWidget<?, ?, ?> widget) {
        return children.contains(widget);
    }

    public <L extends ILayout> L layout() {
        return (L) layout;
    }

    public AbstractContainerWidget<T, STYLE, ANIM> layout(ILayout layout) {
        this.layout = layout;
        return this;
    }

    @Override
    public void mouseMove(float x, float y) {
        super.mouseMove(x, y);
        children.forEach((child) -> child.mouseMove(x, y));
    }

    @Override
    public void mousePress(float x, float y, int button) {
        super.mousePress(x, y, button);
        children.forEach((child) -> child.mousePress(x, y, button));
    }

    @Override
    public void mouseRelease(float x, float y, int button) {
        super.mouseRelease(x, y, button);
        children.forEach((child) -> child.mouseRelease(x, y, button));

    }

    @Override
    public void mouseDrag(float mouseX, float mouseY, float dragX, float dragY, int button) {
        super.mouseDrag(mouseX, mouseY, dragX, dragY, button);
        children.forEach((child) -> child.mouseDrag(mouseX, mouseY, dragX, dragY, button));
    }

    @Override
    public void mouseScroll(float x, float y, double scrollX) {
        super.mouseScroll(x, y, scrollX);
        children.forEach((child) -> child.mouseScroll(x, y, scrollX));

    }

    @Override
    public void keyPress(int keyCode, int scancode, int modifiers) {
        super.keyPress(keyCode, scancode, modifiers);
        children.forEach((child) -> child.keyPress(keyCode, scancode, modifiers));

    }

    @Override
    public void keyRelease(int keyCode, int scancode, int modifiers) {
        super.keyRelease(keyCode, scancode, modifiers);
        children.forEach((child) -> child.keyRelease(keyCode, scancode, modifiers));
    }

    @Override
    public void charTyped(char codePoint, int modifiers) {
        super.charTyped(codePoint, modifiers);
        children.forEach((child) -> child.charTyped(codePoint, modifiers));
    }

    @Override
    public void render(UIDrawContext drawContext, UIInputState inputState) {
        layout.update();
        children.forEach((child) -> child.render(drawContext, inputState));
    }

    @Override
    public Rectangle getRectangle() {
        return layout.getRectangle();
    }
}
