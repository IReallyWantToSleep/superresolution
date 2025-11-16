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

import io.homo.superresolution.core.gui.MaterialUI;
import io.homo.superresolution.core.gui.core.animator.AnimationSet;
import io.homo.superresolution.core.gui.core.event.GuiEventListener;
import io.homo.superresolution.core.gui.core.event.events.MouseEvent;
import io.homo.superresolution.core.gui.core.event.events.WidgetEvent;
import io.homo.superresolution.core.gui.core.impl.Renderable;
import io.homo.superresolution.core.gui.core.impl.TooltipHolder;
import io.homo.superresolution.core.gui.core.layout.AbstractLayoutElement;
import io.homo.superresolution.core.impl.Destroyable;
import net.neoforged.bus.api.IEventBus;
import org.joml.Vector2f;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class AbstractWidget<
        T extends AbstractWidget<?, ?, ?>,
        STYLE extends WidgetStyle<?>,
        ANIM extends AnimationSet
        > extends AbstractLayoutElement implements
        GuiEventListener,
        Renderable,
        TooltipHolder,
        Destroyable {
    protected boolean visible = true;
    protected boolean disabled = false;
    protected boolean hovered = false;
    protected boolean pressed = false;
    protected boolean focused = false;

    public IEventBus getEventBus() {
        return eventBus;
    }

    protected IEventBus eventBus;
    protected STYLE style;
    protected ANIM animationSet;
    protected Supplier<Optional<String>> tooltipSupplier = Optional::empty;

    public AbstractWidget() {
        this.eventBus = MaterialUI.createEventBus(
                this.getClass().getName()
        );
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
        eventBus.post(new MouseEvent.MouseMoveEvent(mousePos));
        if (isHovering != this.hovered) {
            eventBus.post(new WidgetEvent.HoverEvent(mousePos, isHovering));
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
            eventBus.post(new MouseEvent.MousePressEvent(mousePos, button));
        }
    }

    @Override
    public void mouseRelease(float x, float y, int button) {
        if (button == MouseButton.Left.id()) {
            Vector2f mousePos = new Vector2f(x, y);
            if (isPressed()) {
                eventBus.post(new MouseEvent.MouseReleaseEvent(mousePos, button));
            }
            setPressed(false);
        }
    }

    @Override
    public void mouseDrag(float mouseX, float mouseY, float dragX, float dragY, int button) {
        eventBus.post(new MouseEvent.MouseDragEvent(button, new Vector2f(mouseX, mouseY), new Vector2f(dragX, dragY)));
    }

    @Override
    public void mouseScroll(float x, float y, double scrollX) {
        eventBus.post(new MouseEvent.MouseScrollEvent(new Vector2f(x, y), (float) scrollX));
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

    public void onHover(Consumer<WidgetEvent.HoverEvent> listener) {
        eventBus.addListener(WidgetEvent.HoverEvent.class, listener);
    }

    public void onFocus(Consumer<WidgetEvent.FocusEvent> listener) {
        eventBus.addListener(WidgetEvent.FocusEvent.class, listener);
    }

    protected boolean isInteractive() {
        return false;
    }

    public AbstractWidget<?, ?, ?> findInteractiveWidgetAt(Vector2f absPos) {
        if (!hitTest(absPos)) {
            return null;
        }
        return isInteractive() ? this : null;
    }

    public void onMousePress(Consumer<MouseEvent.MousePressEvent> listener) {
        eventBus.addListener(MouseEvent.MousePressEvent.class, listener);
    }

    public void onMouseMove(Consumer<MouseEvent.MouseMoveEvent> listener) {
        eventBus.addListener(MouseEvent.MouseMoveEvent.class, listener);
    }

    public void onMouseRelease(Consumer<MouseEvent.MouseReleaseEvent> listener) {
        eventBus.addListener(MouseEvent.MouseReleaseEvent.class, listener);
    }

    public void onMouseDrag(Consumer<MouseEvent.MouseDragEvent> listener) {
        eventBus.addListener(MouseEvent.MouseDragEvent.class, listener);
    }

    public void onMouseScroll(Consumer<MouseEvent.MouseScrollEvent> listener) {
        eventBus.addListener(MouseEvent.MouseScrollEvent.class, listener);
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

    public void destroy() {
    }

}
