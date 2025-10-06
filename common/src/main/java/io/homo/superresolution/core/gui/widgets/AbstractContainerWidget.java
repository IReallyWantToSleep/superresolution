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

import io.homo.superresolution.core.gui.core.AbstractWidget;
import io.homo.superresolution.core.gui.core.UIDrawContext;
import io.homo.superresolution.core.gui.core.UIInputState;
import io.homo.superresolution.core.gui.core.WidgetStyle;
import io.homo.superresolution.core.gui.core.animator.AnimationSet;
import io.homo.superresolution.core.gui.core.event.EventListener;
import io.homo.superresolution.core.gui.core.impl.Rectangle;
import io.homo.superresolution.core.gui.core.impl.Renderable;
import io.homo.superresolution.core.gui.core.layout.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class AbstractContainerWidget<
        T extends AbstractWidget<?, ?, ?>,
        STYLE extends WidgetStyle<?>,
        ANIM extends AnimationSet
        > extends AbstractWidget<T, STYLE, ANIM> implements ILayoutContainer {

    protected final List<ILayoutElement> children = new ArrayList<>();
    protected ILayout layout;

    @Override
    protected void init() {
        if (this.layout == null) {
            this.layout = new AbsoluteLayout();
        }
    }

    @Override
    public void addChild(ILayoutElement element) {
        children.add(element);
        element.setParent(this);
        if (layout != null) {
            layout.layout(this);
        }
    }

    @Override
    public void removeChild(ILayoutElement element) {
        children.remove(element);
        element.setParent(null);
        if (layout != null) {
            layout.layout(this);
        }
    }

    @Override
    public List<ILayoutElement> getChildren() {
        return Collections.unmodifiableList(children);
    }

    @Override
    public ILayout getLayout() {
        return layout;
    }

    @Override
    public void setLayout(ILayout layout) {
        this.layout = layout;
        if (layout != null) {
            layout.layout(this);
        }
    }

    public void addChild(AbstractWidget<?, ?, ?> widget) {
        addChild((ILayoutElement) widget);
    }

    public void removeChild(AbstractWidget<?, ?, ?> widget) {
        removeChild((ILayoutElement) widget);
    }

    @Override
    public void mouseMove(float x, float y) {
        super.mouseMove(x, y);

        if (isDisabled()) return;

        // 将事件传播给子控件
        for (ILayoutElement child : children) {
            if (child instanceof AbstractWidget) {
                AbstractWidget<?, ?, ?> widget = (AbstractWidget<?, ?, ?>) child;
                if (widget.isVisible() && !widget.isDisabled()) {
                    widget.mouseMove(x, y);
                }
            }
        }
    }

    @Override
    public void mousePress(float x, float y, int button) {
        super.mousePress(x, y, button);

        if (isDisabled()) return;

        for (ILayoutElement child : children) {
            if (child instanceof AbstractWidget) {
                AbstractWidget<?, ?, ?> widget = (AbstractWidget<?, ?, ?>) child;
                if (widget.isVisible() && !widget.isDisabled()) {
                    widget.mousePress(x, y, button);
                }
            }
        }
    }

    @Override
    public void mouseRelease(float x, float y, int button) {
        super.mouseRelease(x, y, button);

        if (isDisabled()) return;

        for (ILayoutElement child : children) {
            if (child instanceof AbstractWidget) {
                AbstractWidget<?, ?, ?> widget = (AbstractWidget<?, ?, ?>) child;
                if (widget.isVisible() && !widget.isDisabled()) {
                    widget.mouseRelease(x, y, button);
                }
            }
        }
    }

    @Override
    public void mouseDrag(float mouseX, float mouseY, float dragX, float dragY, int button) {
        super.mouseDrag(mouseX, mouseY, dragX, dragY, button);
        if (isDisabled()) return;

        for (ILayoutElement child : children) {
            if (child instanceof AbstractWidget) {
                AbstractWidget<?, ?, ?> widget = (AbstractWidget<?, ?, ?>) child;
                if (widget.isVisible() && !widget.isDisabled()) {
                    widget.mouseDrag(mouseX, mouseY, dragX, dragY, button);
                }
            }
        }
    }

    @Override
    public void mouseScroll(float x, float y, double scrollX) {
        super.mouseScroll(x, y, scrollX);
        if (isDisabled()) return;

        for (ILayoutElement child : children) {
            if (child instanceof AbstractWidget) {
                AbstractWidget<?, ?, ?> widget = (AbstractWidget<?, ?, ?>) child;
                if (widget.isVisible() && !widget.isDisabled()) {
                    widget.mouseScroll(x, y, scrollX);
                }
            }
        }
    }

    @Override
    public void keyPress(int keyCode, int scancode, int modifiers) {
        super.keyPress(keyCode, scancode, modifiers);
        if (isDisabled()) return;

        for (ILayoutElement child : children) {
            if (child instanceof AbstractWidget) {
                AbstractWidget<?, ?, ?> widget = (AbstractWidget<?, ?, ?>) child;
                if (widget.isVisible() && !widget.isDisabled()) {
                    widget.keyPress(keyCode, scancode, modifiers);
                }
            }
        }
    }

    @Override
    public void keyRelease(int keyCode, int scancode, int modifiers) {
        super.keyRelease(keyCode, scancode, modifiers);
        if (isDisabled()) return;
        for (ILayoutElement child : children) {
            if (child instanceof AbstractWidget) {
                AbstractWidget<?, ?, ?> widget = (AbstractWidget<?, ?, ?>) child;
                if (widget.isVisible() && !widget.isDisabled()) {
                    widget.keyRelease(keyCode, scancode, modifiers);
                }
            }
        }
    }

    @Override
    public void charTyped(char codePoint, int modifiers) {
        super.charTyped(codePoint, modifiers);
        if (isDisabled()) return;
        for (ILayoutElement child : children) {
            if (child instanceof AbstractWidget) {
                AbstractWidget<?, ?, ?> widget = (AbstractWidget<?, ?, ?>) child;
                if (widget.isVisible() && !widget.isDisabled()) {
                    widget.charTyped(codePoint, modifiers);
                }
            }
        }
    }

    @Override
    public void render(UIDrawContext drawContext, UIInputState inputState) {
        if (!isVisible()) return;

        if (layout != null) {
            layout.layout(this);
        }

        renderSelf(drawContext, inputState);

        for (ILayoutElement child : children) {
            if (child instanceof Renderable renderableChild) {
                renderableChild.render(drawContext, inputState);
            }
        }
    }

    protected void renderSelf(UIDrawContext drawContext, UIInputState inputState) {
    }
}
