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
import io.homo.superresolution.core.gui.core.layout.*;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaNode;
import org.joml.Vector2f;

public abstract class AbstractScrollableContainerWidget extends AbstractContainerWidget<AbstractScrollableContainerWidget, WidgetStyle<?>, AnimationSet> {
    protected Vector2f viewRegion = new Vector2f(0, 0);
    protected Vector2f scrollOffset = new Vector2f(0, 0);
    protected boolean horizontalScrollEnabled = true;
    protected boolean verticalScrollEnabled = true;
    protected float topPadding = 0;
    protected float bottomPadding = 0;
    protected float leftPadding = 0;
    protected float rightPadding = 0;
    protected boolean isContainerDragging = false;
    protected YogaNode wrapperNode = new YogaNode(AbstractWidget.yogaConfig);
    private IScrollHandler scrollHandler = new SmoothDragScrollHandler(this::setScrollOffset);

    public AbstractScrollableContainerWidget() {
        wrapperNode.addChildAt(getWrapperedLayoutNode(), 0);
        wrapperNode.setDebugName("ScrollableContainerWrapperNode");
    }

    @Override
    public YogaNode getLayoutNode() {
        return wrapperNode;
    }

    public YogaNode getWrapperedLayoutNode() {
        return super.getLayoutNode();
    }

    @Override
    public void setElementSize(float width, float height) {
        super.setElementSize(width, height);
        viewRegion.set(width, height);
        if (width > 0) wrapperNode.setWidth(width);
        if (height > 0) wrapperNode.setHeight(height);
    }

    @Override
    public void addChild(ILayoutElement element) {
        children.add(element);
        element.setParent(this);
        getWrapperedLayoutNode().addChildAt(element.getLayoutNode(), Math.max(getLayoutNode().getChildCount() - 1, 0));
    }

    @Override
    public void removeChild(ILayoutElement element) {
        children.remove(element);
        element.setParent(null);
        getWrapperedLayoutNode().removeChild(element.getLayoutNode());
    }

    public IScrollHandler getScrollHandler() {
        return scrollHandler;
    }

    public AbstractScrollableContainerWidget setScrollHandler(IScrollHandler scrollHandler) {
        this.scrollHandler = scrollHandler;
        return this;
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

    public void setTopPadding(float padding) {
        this.topPadding = Math.max(0, padding);
    }

    public void setBottomPadding(float padding) {
        this.bottomPadding = Math.max(0, padding);
    }

    public void setLeftPadding(float padding) {
        this.leftPadding = Math.max(0, padding);
    }

    public void setRightPadding(float padding) {
        this.rightPadding = Math.max(0, padding);
    }

    public Vector2f getScrollOffset() {
        return new Vector2f(scrollOffset);
    }

    public void setScrollOffset(Vector2f offset) {
        this.scrollOffset = offset != null ? new Vector2f(offset) : new Vector2f(0, 0);
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
    }

    public boolean isVerticalScrollEnabled() {
        return verticalScrollEnabled;
    }

    public void setVerticalScrollEnabled(boolean enabled) {
        this.verticalScrollEnabled = enabled;
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
        Vector2f contentSize = new Vector2f(
                getWrapperedLayoutNode().getLayoutWidth(),
                getWrapperedLayoutNode().getLayoutHeight()
        );
        Vector2f viewSize = new Vector2f(viewRegion.x, viewRegion.y);

        Vector2f min = new Vector2f(0, 0);
        Vector2f max = new Vector2f(
                Math.max(0, contentSize.x - viewSize.x),
                Math.max(0, contentSize.y - viewSize.y)
        );

        scrollHandler.setScrollBounds(min, max);
        scrollHandler.update(inputState.frameTime());
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
/*
//debug
        drawContext.drawRect(
                region.x,
                region.y,
                region.width,
                region.height,
                Color.rgba(255, 0, 0, 255),
                false
        );

        drawContext.drawRect(
                region.x - scrollOffset.x,
                region.y - scrollOffset.y,
                layout.getContainerBounds().width,
                layout.getContainerBounds().height,
                Color.rgba(255, 255, 0, 255),
                false
        );
*/

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

    public Rectangle getViewRegion() {
        return getBounds();
    }

    public AbstractScrollableContainerWidget setViewRegion(Vector2f viewRegion) {
        this.viewRegion = viewRegion;
        return this;
    }

    @Override
    public Rectangle getBounds() {
        Vector2f position = getAbsolutePosition();
        return super.getBounds();
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