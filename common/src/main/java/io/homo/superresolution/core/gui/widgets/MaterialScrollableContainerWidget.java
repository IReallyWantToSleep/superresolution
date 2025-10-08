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

import io.homo.superresolution.core.gui.MaterialScheme;
import io.homo.superresolution.core.gui.core.AbstractScrollableContainerWidget;
import io.homo.superresolution.core.gui.core.UIInputState;
import io.homo.superresolution.core.gui.core.backends.interfaces.IUIDrawContext;
import io.homo.superresolution.core.gui.core.impl.Rectangle;
import io.homo.superresolution.core.utils.Color;
import org.joml.Vector2f;

public class MaterialScrollableContainerWidget extends AbstractScrollableContainerWidget {
    private final int SCROLLBAR_SIZE = 8; // 滚动条宽度
    private final int SCROLLBAR_MARGIN = 2; // 滚动条边距
    private final int MIN_SCROLLBAR_LENGTH = 20; // 滚动条最小长度

    private Color scrollbarColor = Color.rgba(0.3f, 0.3f, 0.3f, 0.6f);
    private Color scrollbarHoverColor = Color.rgba(0.4f, 0.4f, 0.4f, 0.8f);
    private Color scrollbarActiveColor = Color.rgba(0.5f, 0.5f, 0.5f, 0.9f);

    private boolean verticalScrollbarVisible = false;
    private boolean horizontalScrollbarVisible = false;
    private boolean verticalScrollbarHovered = false;
    private boolean horizontalScrollbarHovered = false;
    private boolean verticalScrollbarDragging = false;
    private boolean horizontalScrollbarDragging = false;

    private Rectangle verticalScrollbarRect = new Rectangle();
    private Rectangle horizontalScrollbarRect = new Rectangle();
    protected MaterialScheme scheme = MaterialScheme.defaultLight;

    public MaterialScheme scheme() {
        return scheme;
    }

    public MaterialScrollableContainerWidget scheme(MaterialScheme scheme) {
        this.scheme = scheme;
        return this;
    }

    @Override
    protected void renderScrollbar(IUIDrawContext drawContext, UIInputState inputState) {
        updateScrollbarVisibility();
        renderVerticalScrollbar(drawContext, inputState);
        renderHorizontalScrollbar(drawContext, inputState);
    }

    private void updateScrollbarVisibility() {
        Rectangle containerBounds = getLayout().getContainerBounds();
        Vector2f viewRegion = getViewRegion().getSize();

        verticalScrollbarVisible = isVerticalScrollEnabled() &&
                containerBounds.height > viewRegion.y;

        horizontalScrollbarVisible = isHorizontalScrollEnabled() &&
                containerBounds.width > viewRegion.x;
    }

    private void renderVerticalScrollbar(IUIDrawContext drawContext, UIInputState inputState) {
        if (!verticalScrollbarVisible) return;

        Rectangle containerBounds = getLayout().getContainerBounds();
        Vector2f viewRegion = getViewRegion().getSize();
        Vector2f scrollOffset = getScrollOffset();
        Vector2f absolutePos = getAbsolutePosition();

        float scrollbarWidth = SCROLLBAR_SIZE;
        float scrollbarX = absolutePos.x + viewRegion.x - SCROLLBAR_SIZE - SCROLLBAR_MARGIN;
        float scrollbarY = absolutePos.y + SCROLLBAR_MARGIN;
        float scrollbarHeight = viewRegion.y - 2 * SCROLLBAR_MARGIN;

        if (horizontalScrollbarVisible) {
            scrollbarHeight -= SCROLLBAR_SIZE + SCROLLBAR_MARGIN;
        }

        float contentHeight = containerBounds.height;
        float visibleRatio = viewRegion.y / contentHeight;
        float thumbHeight = Math.max(scrollbarHeight * visibleRatio, MIN_SCROLLBAR_LENGTH);

        float scrollRange = contentHeight - viewRegion.y;
        float scrollProgress = scrollOffset.y / scrollRange;
        float thumbY = scrollbarY + (scrollbarHeight - thumbHeight) * scrollProgress;

        verticalScrollbarRect.setBounds(scrollbarX, thumbY, scrollbarWidth, thumbHeight);

        Color thumbColor = scheme.controlNormal();
        if (verticalScrollbarDragging) {
            thumbColor = scheme.controlActivated();
        } else if (verticalScrollbarHovered) {
            thumbColor = scheme.controlHighlight();
        }

        drawContext.drawRoundedRect(
                scrollbarX, thumbY,
                scrollbarWidth, thumbHeight,
                scrollbarWidth / 2,
                thumbColor,
                true
        );
    }

    private void renderHorizontalScrollbar(IUIDrawContext drawContext, UIInputState inputState) {
        if (!horizontalScrollbarVisible) return;

        Rectangle containerBounds = getLayout().getContainerBounds();
        Vector2f viewRegion = getViewRegion().getSize();
        Vector2f scrollOffset = getScrollOffset();
        Vector2f absolutePos = getAbsolutePosition();

        float scrollbarHeight = SCROLLBAR_SIZE;
        float scrollbarX = absolutePos.x + SCROLLBAR_MARGIN;
        float scrollbarY = absolutePos.y + viewRegion.y - SCROLLBAR_SIZE - SCROLLBAR_MARGIN;
        float scrollbarWidth = viewRegion.x - 2 * SCROLLBAR_MARGIN;

        if (verticalScrollbarVisible) {
            scrollbarWidth -= SCROLLBAR_SIZE + SCROLLBAR_MARGIN;
        }

        float contentWidth = containerBounds.width;
        float visibleRatio = viewRegion.x / contentWidth;
        float thumbWidth = Math.max(scrollbarWidth * visibleRatio, MIN_SCROLLBAR_LENGTH);

        float scrollRange = contentWidth - viewRegion.x;
        float scrollProgress = scrollOffset.x / scrollRange;
        float thumbX = scrollbarX + (scrollbarWidth - thumbWidth) * scrollProgress;

        horizontalScrollbarRect.setBounds(thumbX, scrollbarY, thumbWidth, scrollbarHeight);

        Color thumbColor = scheme.controlNormal();
        if (horizontalScrollbarDragging) {
            thumbColor = scheme.controlActivated();
        } else if (horizontalScrollbarHovered) {
            thumbColor = scheme.controlHighlight();
        }

        drawContext.drawRoundedRect(
                thumbX, scrollbarY,
                thumbWidth, scrollbarHeight,
                scrollbarHeight / 2,
                thumbColor,
                true
        );
    }

    @Override
    public void mouseMove(float x, float y) {
        super.mouseMove(x, y);

        verticalScrollbarHovered = verticalScrollbarVisible &&
                verticalScrollbarRect.in(x, y);
        horizontalScrollbarHovered = horizontalScrollbarVisible &&
                horizontalScrollbarRect.in(x, y);
    }

    protected void onMousePressed(Vector2f mousePosition) {
        super.onMousePressed(mousePosition);
        if (verticalScrollbarHovered) {
            verticalScrollbarDragging = true;
        } else if (horizontalScrollbarHovered) {
            horizontalScrollbarDragging = true;
        }
    }

    protected void onMouseDragged(Vector2f mousePosition, Vector2f dragDelta, int button) {
        super.onMouseDragged(mousePosition, dragDelta, button);

        if (verticalScrollbarDragging) {
            handleVerticalScrollbarDrag(dragDelta.y);
        } else if (horizontalScrollbarDragging) {
            handleHorizontalScrollbarDrag(dragDelta.x);
        }
    }

    protected void onMouseReleased(Vector2f mousePosition) {
        super.onMouseReleased(mousePosition);
        verticalScrollbarDragging = false;
        horizontalScrollbarDragging = false;
    }

    private void handleVerticalScrollbarDrag(float mouseY) {
        Rectangle containerBounds = getLayout().getContainerBounds();
        Vector2f viewRegion = getViewRegion().getSize();

        float contentHeight = containerBounds.height;
        float scrollRange = contentHeight - viewRegion.y;
        if (scrollRange <= 0) return;

        float scrollbarHeight = viewRegion.y - 2 * SCROLLBAR_MARGIN;
        if (horizontalScrollbarVisible) {
            scrollbarHeight -= SCROLLBAR_SIZE + SCROLLBAR_MARGIN;
        }

        float visibleRatio = viewRegion.y / contentHeight;
        float thumbHeight = Math.max(scrollbarHeight * visibleRatio, MIN_SCROLLBAR_LENGTH);
        float maxThumbMove = scrollbarHeight - thumbHeight;

        float deltaScrollY = (mouseY / maxThumbMove) * scrollRange;

        float newScrollY = getScrollOffset().y + deltaScrollY;
        newScrollY = Math.max(0, Math.min(scrollRange, newScrollY));

        getScrollHandler().setScroll(new Vector2f(getScrollOffset().x, newScrollY));
    }

    private void handleHorizontalScrollbarDrag(float mouseX) {
        Rectangle containerBounds = getLayout().getContainerBounds();
        Vector2f viewRegion = getViewRegion().getSize();

        float contentWidth = containerBounds.width;
        float scrollRange = contentWidth - viewRegion.x;
        if (scrollRange <= 0) return;

        float scrollbarWidth = viewRegion.x - 2 * SCROLLBAR_MARGIN;
        if (verticalScrollbarVisible) {
            scrollbarWidth -= SCROLLBAR_SIZE + SCROLLBAR_MARGIN;
        }

        float visibleRatio = viewRegion.x / contentWidth;
        float thumbWidth = Math.max(scrollbarWidth * visibleRatio, MIN_SCROLLBAR_LENGTH);
        float maxThumbMove = scrollbarWidth - thumbWidth;

        float deltaScrollX = (mouseX / maxThumbMove) * scrollRange;

        float newScrollX = getScrollOffset().x + deltaScrollX;
        newScrollX = Math.max(0, Math.min(scrollRange, newScrollX));

        getScrollHandler().setScroll(new Vector2f(newScrollX, getScrollOffset().y));
    }

    @Override
    protected Rectangle getAbsoluteViewRect() {
        return new Rectangle(
                getAbsolutePosition().x,
                getAbsolutePosition().y,
                getBounds().width - SCROLLBAR_SIZE,
                getBounds().height - SCROLLBAR_SIZE);
    }

    @Override
    public Rectangle getBounds() {
        Vector2f position = getAbsolutePosition();
        return new Rectangle(
                position.x,
                position.y,
                viewRegion.x + SCROLLBAR_SIZE,
                viewRegion.y + SCROLLBAR_SIZE
        );
    }

    public MaterialScrollableContainerWidget setViewRegion(Vector2f viewRegion) {
        this.viewRegion = viewRegion.sub(SCROLLBAR_SIZE, SCROLLBAR_SIZE);
        return this;
    }
}