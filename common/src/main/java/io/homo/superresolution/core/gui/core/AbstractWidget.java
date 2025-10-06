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

package io.homo.superresolution.core.gui.core;

import io.homo.superresolution.core.gui.core.animator.AnimationSet;
import io.homo.superresolution.core.gui.core.event.EventHandle;
import io.homo.superresolution.core.gui.core.event.EventHandler;
import io.homo.superresolution.core.gui.core.event.EventListener;
import io.homo.superresolution.core.gui.core.event.events.FocusEvent;
import io.homo.superresolution.core.gui.core.event.events.MouseHoverEvent;
import io.homo.superresolution.core.gui.core.event.events.MousePressEvent;
import io.homo.superresolution.core.gui.core.event.events.MouseReleaseEvent;
import io.homo.superresolution.core.gui.core.impl.*;
import io.homo.superresolution.core.gui.core.layout.AbstractLayoutElement;
import io.homo.superresolution.core.math.Vector2f;

import java.util.Optional;
import java.util.function.Supplier;

public abstract class AbstractWidget<
        T extends AbstractWidget<?, ?, ?>,
        STYLE extends WidgetStyle<?>,
        ANIM extends AnimationSet
        > extends AbstractLayoutElement implements
        EventListener,
        Renderable,
        EventHandle<T>,
        TooltipHolder {
    protected boolean visible = true;
    protected boolean disabled = false;
    protected boolean hovered = false;
    protected boolean pressed = false;
    protected boolean focused = false;
    protected EventHandler<T> eventHandler;
    protected STYLE style;
    protected ANIM animationSet;
    protected Supplier<Optional<String>> tooltipSupplier = Optional::empty;

    public AbstractWidget() {
        this.eventHandler = new EventHandler<>((T) this);

        eventHandler.registerEventType("focus");
        eventHandler.registerEventType("mousePress", Vector2f.class);
        eventHandler.registerEventType("mouseRelease", Vector2f.class);
        eventHandler.registerEventType("mouseHover", Vector2f.class, Boolean.class);
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
        boolean isHovering = getBounds().in(x, y);

        if (isHovering != this.hovered) {
            eventHandler.triggerEvent("mouseHover", mousePos, isHovering);
            setHovered(isHovering);
        }
    }

    @Override
    public void mousePress(float x, float y, int button) {
        if (button == MouseButton.Left.id() && getBounds().in(x, y)) {
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
    public void mouseDrag(float mouseX, float mouseY, float dragX, float dragY, int button) {
        EventListener.super.mouseDrag(mouseX, mouseY, dragX, dragY, button);
    }

    @Override
    public void mouseScroll(float x, float y, double scrollX) {
        EventListener.super.mouseScroll(x, y, scrollX);
    }

    @Override
    public void keyPress(int keyCode, int scancode, int modifiers) {
        EventListener.super.keyPress(keyCode, scancode, modifiers);
    }

    @Override
    public void keyRelease(int keyCode, int scancode, int modifiers) {
        EventListener.super.keyRelease(keyCode, scancode, modifiers);
    }

    @Override
    public void charTyped(char codePoint, int modifiers) {
        EventListener.super.charTyped(codePoint, modifiers);
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

    public abstract Rectangle getBounds();

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
        eventHandler.addEventListener("focus", (widget) -> listener.accept(widget));
    }


    public boolean isDisabled() {
        return disabled;
    }

    public AbstractWidget<T, STYLE, ANIM> setDisabled(boolean disabled) {
        this.disabled = disabled;
        return this;
    }

    @Override
    public void setTooltipSupplier(Supplier<Optional<String>> supplier) {
        tooltipSupplier = supplier;
    }

    @Override
    public Optional<String> getTooltip() {
        return tooltipSupplier.get();
    }

    @Override
    public void setTooltip(String tooltip) {
        this.tooltipSupplier = () -> Optional.ofNullable(tooltip);
    }

    @Override
    public int getZIndex() {
        return style == null ? 0 : style.zIndex();
    }
}
