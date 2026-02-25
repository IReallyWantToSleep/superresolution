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

import io.homo.superresolution.core.gui.MaterialUI;
import io.homo.superresolution.core.gui.core.backends.interfaces.Transform;
import io.homo.superresolution.core.gui.core.backends.render.RenderContext;
import io.homo.superresolution.core.gui.core.event.GuiEventListener;
import io.homo.superresolution.core.gui.core.event.events.MouseEvent;
import io.homo.superresolution.core.gui.core.event.events.WidgetEvent;
import io.homo.superresolution.core.gui.core.impl.Renderable;
import io.homo.superresolution.core.gui.core.impl.TooltipHolder;
import io.homo.superresolution.core.gui.core.layout.AbstractLayoutElement;
import io.homo.superresolution.core.gui.core.layout.ILayoutContainer;
import io.homo.superresolution.core.gui.core.layout.ILayoutElement;
import io.homo.superresolution.core.impl.Destroyable;
import net.neoforged.bus.api.IEventBus;
import org.joml.Vector2f;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class AbstractWidget<
        T extends AbstractWidget<T>
        > extends AbstractLayoutElement implements
        GuiEventListener,
        Renderable,
        TooltipHolder<T>,
        Destroyable {
    protected boolean visible = true;
    protected boolean disabled = false;
    protected boolean hovered = false;
    protected boolean pressed = false;
    protected boolean focused = false;
    protected IEventBus eventBus;
    protected WidgetStyle<?> style;
    protected Supplier<Optional<String>> tooltipSupplier = Optional::empty;

    public AbstractWidget() {
        this.eventBus = MaterialUI.createEventBus(
                this.getClass().getName()
        );
        init();
    }

    protected abstract void init();

    public void layouting(RenderContext ctx) {
    }

    public IEventBus getEventBus() {
        return eventBus;
    }

    public WidgetStyle<?> style() {
        return style;
    }

    @SuppressWarnings("unchecked")
    public T style(WidgetStyle<?> style) {
        this.style = style;
        return (T) this;
    }

    @Override
    public Transform getTransform() {
        if (style != null) {
            return style.transform();
        }
        return super.getTransform();
    }

    public void clearHover() {
        if (this.hovered) {
            Vector2f emptyPos = new Vector2f(-1, -1);
            eventBus.post(new MouseEvent.MouseMoveEvent(emptyPos));
            eventBus.post(new WidgetEvent.HoverEvent(emptyPos, false));
            this.hovered = false;
        }
    }

    @Override
    public void mousePress(float x, float y, int button) {
        if (isDisabled()) {
            return;
        }
        Vector2f mousePos = new Vector2f(x, y);
        if (button == MouseButton.Left.id()) {
            setPressed(true);
        }
        eventBus.post(new MouseEvent.MousePressEvent(mousePos, button));
    }

    @Override
    public void mouseRelease(float x, float y, int button) {
        if (isDisabled()) {
            setPressed(false);
            return;
        }
        if (button == MouseButton.Left.id()) {
            Vector2f mousePos = new Vector2f(x, y);
            if (isPressed()) {
                eventBus.post(new MouseEvent.MouseReleaseEvent(mousePos, button));
            }
            setPressed(false);
        }
    }

    @Override
    public void mouseMove(float x, float y) {
        if (isDisabled()) {
            if (this.hovered) {
                Vector2f mousePos = new Vector2f(x, y);
                eventBus.post(new WidgetEvent.HoverEvent(mousePos, false));
                setHovered(false);
            }
            return;
        }
        Vector2f mousePos = new Vector2f(x, y);
        boolean isHovering = getRawBounds().in(x, y);
        eventBus.post(new MouseEvent.MouseMoveEvent(mousePos));
        if (isHovering != this.hovered) {
            eventBus.post(new WidgetEvent.HoverEvent(mousePos, isHovering));
            setHovered(isHovering);
        }
    }

    @Override
    public void mouseDrag(float mouseX, float mouseY, float dragX, float dragY, int button) {
        if (isDisabled()) {
            return;
        }
        eventBus.post(new MouseEvent.MouseDragEvent(button, new Vector2f(mouseX, mouseY), new Vector2f(dragX, dragY)));
    }

    @Override
    public void mouseScroll(float x, float y, double scrollX) {
        if (isDisabled()) {
            return;
        }
        eventBus.post(new MouseEvent.MouseScrollEvent(new Vector2f(x, y), (float) scrollX));
    }

    @Override
    public void keyPress(int keyCode, int scancode, int modifiers) {
        if (isDisabled()) {
            return;
        }
        GuiEventListener.super.keyPress(keyCode, scancode, modifiers);
    }

    @Override
    public void keyRelease(int keyCode, int scancode, int modifiers) {
        if (isDisabled()) {
            return;
        }
        GuiEventListener.super.keyRelease(keyCode, scancode, modifiers);
    }

    @Override
    public void charTyped(char codePoint, int modifiers) {
        if (isDisabled()) {
            return;
        }
        GuiEventListener.super.charTyped(codePoint, modifiers);
    }

    public boolean isVisible() {
        return visible;
    }

    @SuppressWarnings("unchecked")
    public T setVisible(boolean visible) {
        if (visible == this.visible) {
            return (T) this;
        }

        this.visible = visible;
        return (T) this;

    }

    public boolean isHovered() {
        return hovered;
    }

    @SuppressWarnings("unchecked")
    public T setHovered(boolean hovered) {
        if (hovered == this.hovered) {
            return (T) this;
        }
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
        if (focused == this.focused) {
            return (T) this;
        }

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

    @SuppressWarnings("unchecked")
    public T onHover(Consumer<WidgetEvent.HoverEvent> listener) {
        eventBus.addListener(WidgetEvent.HoverEvent.class, listener);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T onFocus(Consumer<WidgetEvent.FocusEvent> listener) {
        eventBus.addListener(WidgetEvent.FocusEvent.class, listener);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T onClick(Consumer<WidgetEvent.ClickEvent> listener) {
        eventBus.addListener(WidgetEvent.ClickEvent.class, listener);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T onChange(Consumer<WidgetEvent.ChangeEvent> listener) {
        eventBus.addListener(WidgetEvent.ChangeEvent.class, listener);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T onInput(Consumer<WidgetEvent.InputEvent> listener) {
        eventBus.addListener(WidgetEvent.InputEvent.class, listener);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T onMousePress(Consumer<MouseEvent.MousePressEvent> listener) {
        eventBus.addListener(MouseEvent.MousePressEvent.class, listener);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T onMouseMove(Consumer<MouseEvent.MouseMoveEvent> listener) {
        eventBus.addListener(MouseEvent.MouseMoveEvent.class, listener);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T onMouseRelease(Consumer<MouseEvent.MouseReleaseEvent> listener) {
        eventBus.addListener(MouseEvent.MouseReleaseEvent.class, listener);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T onMouseDrag(Consumer<MouseEvent.MouseDragEvent> listener) {
        eventBus.addListener(MouseEvent.MouseDragEvent.class, listener);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T onMouseScroll(Consumer<MouseEvent.MouseScrollEvent> listener) {
        eventBus.addListener(MouseEvent.MouseScrollEvent.class, listener);
        return (T) this;
    }

    protected boolean isInteractive() {
        return false;
    }

    public boolean checkInteractive() {
        return isInteractive();
    }

    public void render(RenderContext ctx, UIInputState inputState) {
    }

    @Override
    public int getZIndex() {
        return style == null ? 0 : style.zIndex();
    }

    public void renderWithChildren(RenderContext ctx, UIInputState inputState) {
        if (!isVisible()) {
            return;
        }
        render(ctx, inputState);

        if (managesChildRendering()) {
            return;
        }

        if (this instanceof ILayoutContainer container) {
            for (ILayoutElement child : container.getChildren()) {
                if (child instanceof AbstractWidget<?> childWidget) {
                    childWidget.renderWithChildren(ctx, inputState);
                }
            }
        }
    }

    public boolean managesChildRendering() {
        return false;
    }

    public boolean managesChildEvents() {
        return false;
    }

    public boolean isFloatingWidget() {
        return false;
    }

    public AbstractWidget<?> findInteractiveWidgetAt(Vector2f absPos) {
        if (!hitTest(absPos)) {
            return null;
        }
        return isInteractive() ? this : null;
    }

    public boolean isDisabled() {
        return disabled;
    }

    @SuppressWarnings("unchecked")
    public T setDisabled(boolean disabled) {
        this.disabled = disabled;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T enable() {
        return setDisabled(false);
    }

    @SuppressWarnings("unchecked")
    public T disable() {
        return setDisabled(true);
    }

    @SuppressWarnings("unchecked")
    @Override
    public T setTooltipSupplier(Supplier<Optional<String>> supplier) {
        tooltipSupplier = supplier;
        return (T) this;
    }

    @Override
    public Optional<String> getTooltip() {
        return tooltipSupplier.get();
    }

    @Override
    @SuppressWarnings("unchecked")
    public T setTooltip(String tooltip) {
        this.tooltipSupplier = () -> Optional.ofNullable(tooltip);
        return (T) this;
    }

    public void destroy() {
    }

}
