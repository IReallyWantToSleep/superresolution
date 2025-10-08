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
import io.homo.superresolution.core.gui.core.backends.interfaces.IUIDrawContext;
import io.homo.superresolution.core.gui.core.impl.Rectangle;
import io.homo.superresolution.core.gui.core.impl.Renderable;
import io.homo.superresolution.core.gui.core.layout.*;
import io.homo.superresolution.core.utils.Color;
import org.joml.Vector2f;

import java.util.List;

public abstract class AbstractScrollableContainerWidget extends AbstractContainerWidget<AbstractScrollableContainerWidget, WidgetStyle<?>, AnimationSet> {
    protected Vector2f viewRegion = new Vector2f(0, 0);
    protected final ScrollWrapperLayout scrollWrapperLayout;
    protected Vector2f scrollOffset = new Vector2f(0, 0);
    protected boolean horizontalScrollEnabled = true;
    protected boolean verticalScrollEnabled = true;

    public IScrollHandler getScrollHandler() {
        return scrollHandler;
    }

    public AbstractScrollableContainerWidget setScrollHandler(IScrollHandler scrollHandler) {
        this.scrollHandler = scrollHandler;
        return this;
    }

    private IScrollHandler scrollHandler = new SmoothDragScrollHandler(this::setScrollOffset);

    public AbstractScrollableContainerWidget() {
        this.scrollWrapperLayout = new ScrollWrapperLayout();
        super.setLayout(scrollWrapperLayout);
    }

    @Override
    protected void init() {
        super.init();

        onMousePress(event -> {
            onMousePressed(event.getMousePosition());
        });

        onMouseDrag(event -> {
            //if (event.isConsumed()) return;
            onMouseDragged(event.getMousePosition(), event.getDragDelta(), event.getButton());
            //event.consume();
        });

        onMouseRelease(event -> {
            onMouseReleased(event.getMousePosition());
            //event.consume();
        });

        onMouseScroll(event -> {
            if (isDisabled()) return;
            scrollHandler.onScroll(0, event.getScrollY());
            //event.consume();
        });
    }


    @Override
    public void setLayout(ILayout layout) {
        scrollWrapperLayout.setWrappedLayout(layout);
    }

    @Override
    public ILayout getLayout() {
        return scrollWrapperLayout;
    }

    public ILayout getWrappedLayout() {
        return scrollWrapperLayout.getWrappedLayout();
    }

    public Vector2f getScrollOffset() {
        return new Vector2f(scrollOffset);
    }

    public void setScrollOffset(Vector2f offset) {
        this.scrollOffset = offset != null ? new Vector2f(offset) : new Vector2f(0, 0);
        if (layout != null) {
            layout.layout(this);
        }
    }

    public void setScrollOffset(float x, float y) {
        setScrollOffset(new Vector2f(x, y));
    }

    public void scrollBy(Vector2f delta) {
        setScrollOffset(scrollOffset.x + delta.x, scrollOffset.y + delta.y);
    }

    public void scrollBy(float deltaX, float deltaY) {
        setScrollOffset(scrollOffset.x + deltaX, scrollOffset.y + deltaY);
    }

    public boolean isHorizontalScrollEnabled() {
        return horizontalScrollEnabled;
    }

    public void setHorizontalScrollEnabled(boolean enabled) {
        this.horizontalScrollEnabled = enabled;
        if (layout != null) {
            layout.layout(this);
        }
    }

    public boolean isVerticalScrollEnabled() {
        return verticalScrollEnabled;
    }

    public void setVerticalScrollEnabled(boolean enabled) {
        this.verticalScrollEnabled = enabled;
        if (layout != null) {
            layout.layout(this);
        }
    }

    @Override
    public void mouseScroll(float x, float y, double scrollX) {
        super.mouseScroll(x, y, scrollX);

        if (isDisabled()) return;

        float scrollDelta = (float) scrollX;

        if (verticalScrollEnabled) {
            scrollHandler.scrollBy(new Vector2f(0, -scrollDelta));
        } else if (horizontalScrollEnabled) {
            scrollHandler.scrollBy(new Vector2f(-scrollDelta, 0));
        }
    }

    @Override
    public void render(IUIDrawContext drawContext, UIInputState inputState) {
        if (!isVisible()) return;
        Vector2f contentSize = new Vector2f(layout.getContainerBounds().width, layout.getContainerBounds().height);
        Vector2f viewSize = new Vector2f(viewRegion.x, viewRegion.y);

        Vector2f min = new Vector2f(0, 0);
        Vector2f max = new Vector2f(
                Math.max(0, contentSize.x - viewSize.x),
                Math.max(0, contentSize.y - viewSize.y)
        );

        scrollHandler.setScrollBounds(min, max);
        scrollHandler.update(inputState.frameTime());
        if (layout != null) {
            layout.layout(this);
        }
        Vector2f position = getAbsolutePosition();
        Rectangle region = new Rectangle(
                position.x,
                position.y,
                viewRegion.x,
                viewRegion.y
        );
        drawContext.beginBatch();
        renderSelf(drawContext, inputState);
        drawContext.scissor(
                region.x,
                region.y,
                region.width,
                region.height
        );
        for (ILayoutElement child : children) {
            if (child instanceof AbstractWidget<?, ?, ?> widget) {
                if (region.intersect(widget.getBounds())) {
                    widget.render(drawContext, inputState);
                }
            }
        }
        drawContext.resetScissor();
        renderScrollbar(drawContext, inputState);
        drawContext.endBatch(getZIndex());
    }

    protected abstract void renderScrollbar(IUIDrawContext drawContext, UIInputState inputState);

    public AbstractScrollableContainerWidget layout(ILayout layout) {
        setLayout(layout);
        return this;
    }

    private class ScrollWrapperLayout extends AbstractLayout {
        private ILayout wrappedLayout;
        private final AbsoluteLayout absoluteLayout = new AbsoluteLayout();

        public void setWrappedLayout(ILayout layout) {
            this.wrappedLayout = layout;
            if (wrappedLayout != null) {
                wrappedLayout.layout(AbstractScrollableContainerWidget.this);
            }
        }

        public ILayout getWrappedLayout() {
            return wrappedLayout;
        }

        @Override
        protected void performLayout(ILayoutContainer container) {
            if (wrappedLayout == null) {
                return;
            }

            wrappedLayout.layout(container);

            for (ILayoutElement child : container.getChildren()) {
                Vector2f originalPosition = wrappedLayout.getElementPosition(child);
                Vector2f scrolledPosition = new Vector2f(
                        originalPosition.x - (horizontalScrollEnabled ? scrollOffset.x : 0),
                        originalPosition.y - (verticalScrollEnabled ? scrollOffset.y : 0)
                );

                absoluteLayout.setPosition(child, scrolledPosition);
            }
        }

        @Override
        public Vector2f getElementPosition(ILayoutElement element) {
            return absoluteLayout.getElementPosition(element);
        }

        @Override
        protected void calculateContainerBounds(ILayoutContainer container) {
            if (wrappedLayout == null) {
                containerBounds.setBounds(0, 0, 0, 0);
                return;
            }

            Rectangle wrappedBounds = wrappedLayout.getContainerBounds();
            containerBounds.setBounds(
                    wrappedBounds.x,
                    wrappedBounds.y,
                    wrappedBounds.width,
                    wrappedBounds.height
            );
        }
    }

    public AbstractScrollableContainerWidget setViewRegion(Vector2f viewRegion) {
        this.viewRegion = viewRegion;
        return this;
    }

    public Rectangle getViewRegion() {
        return getBounds();
    }

    @Override
    public Rectangle getBounds() {
        Vector2f position = getAbsolutePosition();
        return new Rectangle(
                position.x,
                position.y,
                viewRegion.x,
                viewRegion.y
        );
    }

    protected void onMousePressed(Vector2f mousePosition) {
        if (getBounds().in(mousePosition.x, mousePosition.y)) {
            scrollHandler.onDragStart(mousePosition);
        }
    }

    protected void onMouseDragged(Vector2f mousePosition, Vector2f dragDelta, int button) {
        scrollHandler.onDragMove(mousePosition, dragDelta);
    }

    protected void onMouseReleased(Vector2f mousePosition) {
        scrollHandler.onDragEnd(mousePosition);
    }

}