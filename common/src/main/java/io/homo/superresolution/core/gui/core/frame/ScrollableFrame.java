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

package io.homo.superresolution.core.gui.core.frame;

import io.homo.superresolution.core.gui.core.AbstractWidget;
import io.homo.superresolution.core.gui.core.IScrollHandler;
import io.homo.superresolution.core.gui.core.SmoothDragScrollHandler;
import io.homo.superresolution.core.gui.core.UIInputState;
import io.homo.superresolution.core.gui.core.backends.interfaces.Transform;
import io.homo.superresolution.core.gui.core.backends.render.RenderContext;
import io.homo.superresolution.core.gui.core.impl.Rectangle;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaDirection;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaNode;
import org.joml.Vector2f;

public class ScrollableFrame extends Frame {

    private IScrollHandler scrollHandler;
    private Vector2f currentScroll = new Vector2f();

    private float contentPaddingLeft = 0;
    private float contentPaddingTop = 0;
    private float contentPaddingRight = 0;
    private float contentPaddingBottom = 0;

    private boolean enableHorizontalScroll = true;
    private boolean enableVerticalScroll = true;

    private boolean isDragging = false;
    private Vector2f lastDragPos = new Vector2f();

    private float uiScale = 1.0f;

    public ScrollableFrame() {
        scrollHandler = new SmoothDragScrollHandler(this::onScrollOffsetChanged);
    }

    public IScrollHandler getScrollHandler() {
        return scrollHandler;
    }

    public void setScrollHandler(IScrollHandler handler) {
        if (handler != null) {
            this.scrollHandler = handler;
            this.scrollHandler.setOnOffsetChanged(this::onScrollOffsetChanged);
            updateScrollBounds();
        }
    }

    private void onScrollOffsetChanged(Vector2f newOffset) {
        this.currentScroll.set(newOffset);
    }

    public void setContentPadding(float padding) {
        setContentPadding(padding, padding, padding, padding);
    }

    public void setContentPadding(float left, float top, float right, float bottom) {
        this.contentPaddingLeft = left;
        this.contentPaddingTop = top;
        this.contentPaddingRight = right;
        this.contentPaddingBottom = bottom;
        markLayoutDirty();
    }

    public float getContentPaddingLeft() {
        return contentPaddingLeft;
    }

    public float getContentPaddingTop() {
        return contentPaddingTop;
    }

    public float getContentPaddingRight() {
        return contentPaddingRight;
    }

    public float getContentPaddingBottom() {
        return contentPaddingBottom;
    }

    public void setHorizontalScrollEnabled(boolean enabled) {
        this.enableHorizontalScroll = enabled;
        updateScrollBounds();
    }

    public void setVerticalScrollEnabled(boolean enabled) {
        this.enableVerticalScroll = enabled;
        updateScrollBounds();
    }

    public float getScrollX() {
        return currentScroll.x;
    }

    public float getScrollY() {
        return currentScroll.y;
    }

    public Vector2f getScrollOffset() {
        return new Vector2f(currentScroll);
    }

    public Vector2f getContentOrigin() {
        return new Vector2f(
                contentPaddingLeft - currentScroll.x,
                contentPaddingTop - currentScroll.y
        );
    }

    @Override
    protected boolean isOutsideViewport(AbstractWidget<?> widget, Transform accumulatedTransform) {
        Rectangle transformedBounds = transformBounds(widget.getBounds(), accumulatedTransform);

        Vector2f contentOrigin = getContentOrigin();
        Rectangle screenBounds = new Rectangle(
                transformedBounds.x + contentOrigin.x,
                transformedBounds.y + contentOrigin.y,
                transformedBounds.width,
                transformedBounds.height
        );

        Rectangle viewport = getViewport();
        Rectangle contentClipRect = new Rectangle(
                contentPaddingLeft,
                contentPaddingTop,
                viewport.width - contentPaddingLeft - contentPaddingRight,
                viewport.height - contentPaddingTop - contentPaddingBottom
        );

        return !(isInViewport(screenBounds) || contentClipRect.intersect(screenBounds));
    }

    @Override
    public void calculateLayout() {
        AbstractWidget<?> root = getRoot();
        if (root == null) {
            return;
        }

        Rectangle viewport = getViewport();

        float viewportWidth = viewport.width - contentPaddingLeft - contentPaddingRight;
        float viewportHeight = viewport.height - contentPaddingTop - contentPaddingBottom;
        YogaNode tempNode = new YogaNode();

        root.getLayoutNode().setOwner(null);
        tempNode.addChildAt(root.getLayoutNode(), 0);
        float layoutWidth = enableHorizontalScroll ? Float.NaN : viewportWidth;
        float layoutHeight = enableVerticalScroll ? Float.NaN : viewportHeight;
        tempNode.calculateLayout(
                layoutWidth,
                layoutHeight,
                YogaDirection.LTR
        );
        tempNode.calculateLayout(layoutWidth, layoutHeight);

        updateScrollBounds();

    }

    @Override
    public void render(RenderContext ctx, UIInputState inputState) {
        AbstractWidget<?> root = getRoot();
        if (root == null) {
            return;
        }
        layoutWidgets(root, ctx);

        calculateLayout();

        update(inputState.frameTime());

        Vector2f contentOrigin = getContentOrigin();

        ctx.save();
        Rectangle viewport = getViewport();
        ctx.scissor(
                contentPaddingLeft,
                contentPaddingTop,
                viewport.width - contentPaddingLeft - contentPaddingRight,
                viewport.height - contentPaddingTop - contentPaddingBottom
        );
        ctx.save();
        ctx.translate(contentOrigin.x, contentOrigin.y);

        super.render(ctx, inputState);

        ctx.restore();
        ctx.resetScissor();

        ctx.restore();
    }

    @Override
    public void dispatchMouseMove(float x, float y) {
        if (isDragging && scrollHandler != null) {
            Vector2f currentPos = new Vector2f(x, y);
            Vector2f delta = new Vector2f(currentPos).sub(lastDragPos);
            scrollHandler.onDragMove(currentPos, delta);
            lastDragPos.set(currentPos);
        }

        Vector2f contentPos = screenToContent(x, y);
        super.dispatchMouseMove(contentPos.x, contentPos.y);
    }

    @Override
    public void dispatchMousePress(float x, float y, int button) {
        if (button == 0 && scrollHandler != null) {
            isDragging = true;
            lastDragPos.set(x, y);
            scrollHandler.onDragStart(new Vector2f(x, y));
        }

        Vector2f contentPos = screenToContent(x, y);
        super.dispatchMousePress(contentPos.x, contentPos.y, button);
    }

    @Override
    public void dispatchMouseRelease(float x, float y, int button) {
        if (button == 0 && isDragging && scrollHandler != null) {
            isDragging = false;
            scrollHandler.onDragEnd(new Vector2f(x, y));
        }

        Vector2f contentPos = screenToContent(x, y);
        super.dispatchMouseRelease(contentPos.x, contentPos.y, button);
    }

    @Override
    public void dispatchMouseDrag(float mouseX, float mouseY, float dragX, float dragY, int button) {
        Vector2f contentPos = screenToContent(mouseX, mouseY);
        super.dispatchMouseDrag(contentPos.x, contentPos.y, dragX, dragY, button);
    }

    @Override
    public void dispatchMouseScroll(float x, float y, double scrollAmount) {
        if (scrollHandler != null) {
            float deltaX = 0;
            float deltaY = enableVerticalScroll ? (float) scrollAmount : 0;
            scrollHandler.onScroll(deltaX, deltaY);
        }

        Vector2f contentPos = screenToContent(x, y);
        super.dispatchMouseScroll(contentPos.x, contentPos.y, scrollAmount);
    }

    @Override
    public AbstractWidget<?> findInteractiveWidgetAt(Vector2f pos) {
        return super.findInteractiveWidgetAt(pos);
    }

    private void updateScrollBounds() {
        AbstractWidget<?> root = getRoot();
        if (root == null || scrollHandler == null) {
            return;
        }
        Rectangle viewport = getViewport();

        float viewportWidth = viewport.width - contentPaddingLeft - contentPaddingRight;
        float viewportHeight = viewport.height - contentPaddingTop - contentPaddingBottom;

        float maxX = enableHorizontalScroll ? Math.max(0, root.getLayoutNode().getLayoutWidth() - (viewportWidth)) : 0;
        float maxY = enableVerticalScroll ? Math.max(0, root.getLayoutNode().getLayoutHeight() - (viewportHeight)) : 0;

        scrollHandler.setScrollBounds(new Vector2f(0, 0), new Vector2f(maxX, maxY));
    }

    public void update(float deltaTime) {
        if (scrollHandler != null) {
            scrollHandler.update(deltaTime);
        }
    }

    private Vector2f screenToContent(float screenX, float screenY) {
        return new Vector2f(
                screenX - contentPaddingLeft + currentScroll.x,
                screenY - contentPaddingTop + currentScroll.y
        );
    }

    private Vector2f contentToScreen(float contentX, float contentY) {
        return new Vector2f(
                contentX + contentPaddingLeft - currentScroll.x,
                contentY + contentPaddingTop - currentScroll.y
        );
    }

    public void scrollTo(float x, float y) {
        if (scrollHandler != null) {
            scrollHandler.scrollTo(new Vector2f(x, y));
        }
    }

    public void setScroll(float x, float y) {
        if (scrollHandler != null) {
            scrollHandler.setScroll(new Vector2f(x, y));
        }
    }

    public void scrollBy(float deltaX, float deltaY) {
        if (scrollHandler != null) {
            scrollHandler.scrollBy(new Vector2f(deltaX, deltaY));
        }
    }

    public void resetScroll() {
        scrollTo(0, 0);
    }

    public void stopScroll() {
        if (scrollHandler != null) {
            scrollHandler.stop();
        }
    }
}
