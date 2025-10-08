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
import io.homo.superresolution.core.gui.core.event.EventHandler;
import io.homo.superresolution.core.gui.core.event.EventListener;
import io.homo.superresolution.core.gui.core.event.GuiEventListener;
import io.homo.superresolution.core.gui.core.event.events.*;
import io.homo.superresolution.core.gui.core.impl.*;
import io.homo.superresolution.core.gui.core.layout.AbstractLayoutElement;
import org.joml.Vector2f;

import java.util.Optional;
import java.util.function.Supplier;

public abstract class AbstractWidget<
        T extends AbstractWidget<?, ?, ?>,
        STYLE extends WidgetStyle<?>,
        ANIM extends AnimationSet
        > extends AbstractLayoutElement implements
        GuiEventListener,
        Renderable,
        TooltipHolder {
    protected boolean visible = true;
    protected boolean disabled = false;
    protected boolean hovered = false;
    protected boolean pressed = false;
    protected boolean focused = false;
    protected EventHandler eventHandler;
    protected STYLE style;
    protected ANIM animationSet;
    protected Supplier<Optional<String>> tooltipSupplier = Optional::empty;

    public AbstractWidget() {
        this.eventHandler = new EventHandler();
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
        eventHandler.fire(new MouseEvent.MouseMoveEvent(mousePos));
        if (isHovering != this.hovered) {
            eventHandler.fire(new WidgetEvent.HoverEvent(mousePos, isHovering));
            setHovered(isHovering);
        }
    }

    @Override
    public void mousePress(float x, float y, int button) {
        if (getBounds().in(x, y)) {
            Vector2f mousePos = new Vector2f(x, y);
            if (button == MouseButton.Left.id()) {
                setPressed(true);
            }
            eventHandler.fire(new MouseEvent.MousePressEvent(mousePos, button));
        }
    }

    @Override
    public void mouseRelease(float x, float y, int button) {
        if (button == MouseButton.Left.id()) {
            Vector2f mousePos = new Vector2f(x, y);
            if (isPressed()) {
                eventHandler.fire(new MouseEvent.MouseReleaseEvent(mousePos, button));
            }
            setPressed(false);
        }
    }

    @Override
    public void mouseDrag(float mouseX, float mouseY, float dragX, float dragY, int button) {
        eventHandler.fire(new MouseEvent.MouseDragEvent(button, new Vector2f(mouseX, mouseY), new Vector2f(dragX, dragY)));
    }

    @Override
    public void mouseScroll(float x, float y, double scrollX) {
        eventHandler.fire(new MouseEvent.MouseScrollEvent(new Vector2f(x, y), (float) scrollX));
    }

    @Override
    public void keyPress(int keyCode, int scancode, int modifiers) {
        GuiEventListener.super.keyPress(keyCode, scancode, modifiers);
    }

    @Override
    public void keyRelease(int keyCode, int scancode, int modifiers) {
        GuiEventListener.super.keyRelease(keyCode, scancode, modifiers);
    }

    @Override
    public void charTyped(char codePoint, int modifiers) {
        GuiEventListener.super.charTyped(codePoint, modifiers);
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

    public void onHover(EventListener<WidgetEvent.HoverEvent> listener) {
        eventHandler.on(WidgetEvent.HoverEvent.class, listener);
    }

    public void onFocus(EventListener<WidgetEvent.FocusEvent> listener) {
        eventHandler.on(WidgetEvent.FocusEvent.class, listener);
    }


    public void onMousePress(EventListener<MouseEvent.MousePressEvent> listener) {
        eventHandler.on(MouseEvent.MousePressEvent.class, listener);
    }

    public void onMouseMove(EventListener<MouseEvent.MouseMoveEvent> listener) {
        eventHandler.on(MouseEvent.MouseMoveEvent.class, listener);
    }

    public void onMouseRelease(EventListener<MouseEvent.MouseReleaseEvent> listener) {
        eventHandler.on(MouseEvent.MouseReleaseEvent.class, listener);
    }

    public void onMouseDrag(EventListener<MouseEvent.MouseDragEvent> listener) {
        eventHandler.on(MouseEvent.MouseDragEvent.class, listener);
    }

    public void onMouseScroll(EventListener<MouseEvent.MouseScrollEvent> listener) {
        eventHandler.on(MouseEvent.MouseScrollEvent.class, listener);
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
