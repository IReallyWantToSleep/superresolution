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

import io.homo.superresolution.core.gui.core.MouseButton;
import io.homo.superresolution.core.gui.core.WidgetStyle;
import io.homo.superresolution.core.gui.core.animator.AnimationSet;
import io.homo.superresolution.core.gui.core.event.EventHandle;
import io.homo.superresolution.core.gui.core.event.EventHandler;
import io.homo.superresolution.core.gui.core.event.EventListener;
import io.homo.superresolution.core.gui.core.event.events.FocusEvent;
import io.homo.superresolution.core.gui.core.event.events.MouseHoverEvent;
import io.homo.superresolution.core.gui.core.event.events.MousePressEvent;
import io.homo.superresolution.core.gui.core.event.events.MouseReleaseEvent;
import io.homo.superresolution.core.gui.core.impl.*;
import io.homo.superresolution.core.gui.core.layout.ILayoutElement;
import io.homo.superresolution.core.math.Vector2f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public abstract class AbstractWidget<
        T extends AbstractWidget<?, ?, ?>,
        STYLE extends WidgetStyle<?>,
        ANIM extends AnimationSet
        > extends TooltipHolder implements
        EventListener,
        Renderable,
        EventHandle<T>,
        ILayoutElement {
    protected boolean visible = true;
    protected boolean hovered;
    protected boolean pressed;
    protected boolean focused;
    protected EventHandler<T> eventHandler;
    protected STYLE style;
    protected ANIM animationSet;
    protected AbstractContainerWidget<?, ?, ?> parent;

    public AbstractWidget() {
        this.eventHandler = new EventHandler<>((T) this);

        eventHandler.registerEventType("focus");
        eventHandler.registerEventType("mousePress", Vector2f.class);
        eventHandler.registerEventType("mouseRelease", Vector2f.class);
        eventHandler.registerEventType("mouseHover", Vector2f.class, Boolean.class);  // 三参数
        init();
    }

    protected abstract void init();

    public STYLE style() {
        return style;
    }

    @SuppressWarnings("unchecked")
    public T style(STYLE style) {
        this.style = style;
        return (T) this;
    }

    @Override
    public void mouseMove(float x, float y) {
        Vector2f mousePos = new Vector2f(x, y);
        boolean isHovering = getRectangle().in(x, y);

        if (isHovering != this.hovered) {
            eventHandler.triggerEvent("mouseHover", mousePos, isHovering);
            setHovered(isHovering);
        }
    }

    @Override
    public void mousePress(float x, float y, int button) {
        if (button == MouseButton.Left.id() && getRectangle().in(x, y)) {
            Vector2f mousePos = new Vector2f(x, y);
            eventHandler.triggerEvent("mousePress", mousePos);
            setPressed(true);
        }
    }

    @Override
    public void mouseRelease(float x, float y, int button) {
        if (button == MouseButton.Left.id()) {
            Vector2f mousePos = new Vector2f(x, y);
            if (isPressed()) {
                eventHandler.triggerEvent("mouseRelease", mousePos);
            }
            setPressed(false);
        }
    }

    @Override
    public void addEventListener(String type, Object listener) {
        eventHandler.addEventListener(type, listener);
    }

    @Override
    public void removeEventListener(String type, Object listener) {
        eventHandler.removeEventListener(type, listener);
    }

    public boolean isVisible() {
        return visible;
    }

    @SuppressWarnings("unchecked")
    public T setVisible(boolean visible) {
        if (visible == this.visible) return (T) this;

        this.visible = visible;
        return (T) this;

    }

    public boolean isHovered() {
        return hovered;
    }

    @SuppressWarnings("unchecked")
    public T setHovered(boolean hovered) {
        if (hovered == this.hovered) return (T) this;
        this.hovered = hovered;
        if (isVisible()) {
            if (hovered) {
            } else {
            }
        }
        return (T) this;
    }

    public boolean isFocused() {
        return focused;
    }

    @SuppressWarnings("unchecked")
    public T setFocused(boolean focused) {
        if (focused == this.focused) return (T) this;

        this.focused = focused;
        return (T) this;
    }

    public boolean isPressed() {
        return pressed;
    }

    @SuppressWarnings("unchecked")
    public T setPressed(boolean pressed) {
        this.pressed = pressed;
        return (T) this;
    }

    public abstract Rectangle getRectangle();

    public void onMouseHover(MouseHoverEvent<T> listener) {
        eventHandler.addEventListener("mouseHover", listener);
    }

    public void onMousePress(MousePressEvent<T> listener) {
        eventHandler.addEventListener("mousePress", listener::accept);
    }

    public void onMouseRelease(MouseReleaseEvent<T> listener) {
        eventHandler.addEventListener("mouseRelease", listener::accept);
    }

    public void onFocus(FocusEvent<T> listener) {
        eventHandler.addEventListener("focus", (widget) -> listener.accept((T) widget));
    }

    public AbstractContainerWidget<?, ?, ?> getParent() {
        return parent;
    }

    public void setParent(AbstractContainerWidget<?, ?, ?> parent) {
        this.parent = parent;
    }

    @Override
    public void setParent(ILayoutElement parent) {
        setParent((AbstractContainerWidget<?, ?, ?>) parent);
    }

    public Vector2f getAbsolutePosition() {
        return parent == null ? new Vector2f(0, 0) : parent.layout().getElementPosition(this);
    }
}
