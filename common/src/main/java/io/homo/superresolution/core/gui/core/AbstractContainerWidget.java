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

package io.homo.superresolution.core.gui.core;

import io.homo.superresolution.core.gui.core.backends.interfaces.IUIDrawContext;
import io.homo.superresolution.core.gui.core.backends.interfaces.Transform;
import io.homo.superresolution.core.gui.core.impl.Rectangle;
import io.homo.superresolution.core.gui.core.impl.Renderable;
import io.homo.superresolution.core.gui.core.layout.AbstractLayoutElement;
import io.homo.superresolution.core.gui.core.layout.ILayoutContainer;
import io.homo.superresolution.core.gui.core.layout.ILayoutElement;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class AbstractContainerWidget<T extends AbstractContainerWidget<T>> extends AbstractWidget<T> implements ILayoutContainer {
    protected final List<ILayoutElement> children = new ArrayList<>();

    @Override
    protected void init() {
        super.getLayoutNode().setDebugName("ContainerNode");
    }

    public void addChild(ILayoutElement element) {
        children.add(element);
        element.setParent(this);
        getLayoutNode().addChildAt(element.getLayoutNode(), getLayoutNode().getChildCount());
    }

    public void removeChild(ILayoutElement element) {
        children.remove(element);
        element.setParent(null);
        getLayoutNode().removeChild(element.getLayoutNode());
    }

    public List<ILayoutElement> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public void addChild(AbstractWidget<?> widget) {
        addChild((ILayoutElement) widget);
    }

    public void removeChild(AbstractWidget<?> widget) {
        removeChild((ILayoutElement) widget);
    }

    @Override
    public void mouseMove(float x, float y) {
        super.mouseMove(x, y);

        if (managesChildRendering()) {
            return;
        }

        if (isDisabled() || !isVisible()) return;
        for (ILayoutElement child : children) {
            if (child instanceof AbstractWidget<?> widget) {
                if (widget.isVisible() && !widget.isDisabled()) {
                    widget.mouseMove(x, y);
                }
            }
        }
    }

    @Override
    public void mousePress(float x, float y, int button) {
        super.mousePress(x, y, button);

        if (managesChildRendering()) {
            return;
        }

        if (isDisabled() || !isVisible()) return;

        for (ILayoutElement child : children) {
            if (child instanceof AbstractWidget<?> widget) {
                if (widget.isVisible() && !widget.isDisabled()) {
                    widget.mousePress(x, y, button);
                }
            }
        }
    }

    @Override
    public void mouseRelease(float x, float y, int button) {
        super.mouseRelease(x, y, button);
        if (isDisabled() || !isVisible()) return;

        for (ILayoutElement child : children) {
            if (child instanceof AbstractWidget<?> widget) {
                if (widget.isVisible() && !widget.isDisabled()) {
                    widget.mouseRelease(x, y, button);
                }
            }
        }
    }

    @Override
    public void mouseDrag(float mouseX, float mouseY, float dragX, float dragY, int button) {
        super.mouseDrag(mouseX, mouseY, dragX, dragY, button);
        if (isDisabled() || !isVisible()) return;
        for (ILayoutElement child : children) {
            if (child instanceof AbstractWidget<?> widget) {
                if (widget.isVisible() && !widget.isDisabled()) {
                    widget.mouseDrag(mouseX, mouseY, dragX, dragY, button);
                }
            }
        }
    }

    @Override
    public void mouseScroll(float x, float y, double scrollX) {
        super.mouseScroll(x, y, scrollX);

        if (managesChildRendering()) {
            return;
        }

        if (isDisabled() || !isVisible()) return;

        for (ILayoutElement child : children) {
            if (child instanceof AbstractWidget<?> widget) {
                if (widget.isVisible() && !widget.isDisabled()) {
                    widget.mouseScroll(x, y, scrollX);
                }
            }
        }
    }

    @Override
    public void keyPress(int keyCode, int scancode, int modifiers) {
        super.keyPress(keyCode, scancode, modifiers);
        if (isDisabled() || !isVisible()) return;

        for (ILayoutElement child : children) {
            if (child instanceof AbstractWidget<?> widget) {
                if (widget.isVisible() && !widget.isDisabled()) {
                    widget.keyPress(keyCode, scancode, modifiers);
                }
            }
        }
    }

    @Override
    public void keyRelease(int keyCode, int scancode, int modifiers) {
        super.keyRelease(keyCode, scancode, modifiers);
        if (isDisabled() || !isVisible()) return;
        for (ILayoutElement child : children) {
            if (child instanceof AbstractWidget<?> widget) {
                if (widget.isVisible() && !widget.isDisabled()) {
                    widget.keyRelease(keyCode, scancode, modifiers);
                }
            }
        }
    }

    @Override
    public void charTyped(char codePoint, int modifiers) {
        super.charTyped(codePoint, modifiers);
        if (isDisabled() || !isVisible()) return;
        for (ILayoutElement child : children) {
            if (child instanceof AbstractWidget<?> widget) {
                if (widget.isVisible() && !widget.isDisabled()) {
                    widget.charTyped(codePoint, modifiers);
                }
            }
        }
    }

    @Override
    public AbstractWidget<?> findInteractiveWidgetAt(Vector2f absPos) {
        if (!hitTest(absPos)) {
            return null;
        }

        for (int i = children.size() - 1; i >= 0; i--) {
            ILayoutElement child = children.get(i);
            if (child instanceof AbstractWidget<?> widget && widget.isVisible() && !widget.isDisabled()) {
                AbstractWidget<?> interactive = widget.findInteractiveWidgetAt(absPos);
                if (interactive != null) {
                    return interactive;
                }
            }
        }

        return isInteractive() ? this : null;
    }

    protected Rectangle getAbsoluteViewRect() {
        return new Rectangle(
                getAbsolutePosition().x,
                getAbsolutePosition().y,
                getBounds().width,
                getBounds().height);
    }

    private boolean isOutsideView(AbstractWidget<?> child) {
        Rectangle view = getAbsoluteViewRect();
        Rectangle childAbs = new Rectangle(
                child.getAbsolutePosition().x,
                child.getAbsolutePosition().y,
                child.getBounds().width,
                child.getBounds().height
        );
        return !view.intersect(childAbs);
    }

    private boolean isOutsideView(AbstractWidget<?> child, Vector2f position) {
        Rectangle view = getAbsoluteViewRect();

        Rectangle childAbs = new Rectangle(
                child.getAbsolutePosition().x,
                child.getAbsolutePosition().y,
                child.getBounds().width,
                child.getBounds().height
        );
        return !(view.x == 0 && view.y == 0 && view.width == 0 && view.height == 0) && (!view.intersect(childAbs) || !view.intersection(childAbs).in(position));
    }

    protected abstract Rectangle getViewRegion();


    protected void renderSelf(IUIDrawContext drawContext, UIInputState inputState) {

    }

    @Override
    public void render(IUIDrawContext drawContext, UIInputState inputState) {
        if (!isVisible()) return;
        renderSelf(drawContext, inputState);
    }

}
