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

import io.homo.superresolution.core.gui.MaterialUI;
import io.homo.superresolution.core.gui.core.AbstractWidget;
import io.homo.superresolution.core.gui.core.UIInputState;
import io.homo.superresolution.core.gui.core.animator.Animator;
import io.homo.superresolution.core.gui.core.animator.BezierInterpolator;
import io.homo.superresolution.core.gui.core.backends.render.RenderContext;
import io.homo.superresolution.core.gui.core.impl.Rectangle;
import io.homo.superresolution.core.utils.Color;

import java.util.function.BooleanSupplier;

public class ScrollableFrameWithScrollBar extends ScrollableFrame {
    private static final float SCROLLBAR_WIDTH = 8f;
    private static final float THUMB_WIDTH = 6f;
    private static final float ARROW_BUTTON_SIZE = 8f;
    private static final float MIN_THUMB_HEIGHT = 24f;
    private static final float SCROLL_STEP = 40f;
    private static final float THUMB_NORMAL_ALPHA = 0.7f;
    private static final float THUMB_HOVER_ALPHA = 1f;
    private static final float ARROW_PRESSED_SCALE = 0.8f;
    private static final long STATE_TRANSITION_DURATION = 150L;

    private final Animator.FloatAnimator thumbAlphaAnimator = new Animator.FloatAnimator(
            THUMB_NORMAL_ALPHA,
            THUMB_NORMAL_ALPHA
    ).duration(STATE_TRANSITION_DURATION).timeInterpolator(BezierInterpolator.EASE_OUT);
    private final Animator.FloatAnimator upArrowScaleAnimator = new Animator.FloatAnimator(1f, 1f)
            .duration(STATE_TRANSITION_DURATION).timeInterpolator(BezierInterpolator.EASE_OUT);
    private final Animator.FloatAnimator downArrowScaleAnimator = new Animator.FloatAnimator(1f, 1f)
            .duration(STATE_TRANSITION_DURATION).timeInterpolator(BezierInterpolator.EASE_OUT);

    private boolean verticalScrollEnabled = true;
    private boolean thumbHovered;
    private boolean scrollbarInteractionActive;
    private boolean scrollbarDragging;
    private int pressedArrow;
    private float thumbGrabOffsetY;

    protected boolean shouldRenderTrackBackground() {
        return false;
    }

    @Override
    public void setVerticalScrollEnabled(boolean enabled) {
        verticalScrollEnabled = enabled;
        super.setVerticalScrollEnabled(enabled);
    }

    @Override
    public void render(RenderContext ctx, UIInputState inputState) {
        super.render(ctx, inputState);

        ScrollbarGeometry geometry = scrollbarGeometry();
        if (!geometry.visible) {
            setThumbHovered(false);
            setPressedArrow(0);
            Animator.updateAll(
                    thumbAlphaAnimator,
                    upArrowScaleAnimator,
                    downArrowScaleAnimator
            );
            return;
        }

        Animator.updateAll(
                thumbAlphaAnimator,
                upArrowScaleAnimator,
                downArrowScaleAnimator
        );

        Color primary = MaterialUI.Scheme.primary();
        Color thumbColor = Color.rgb(
                primary.red() / 255f * thumbAlphaAnimator.get(),
                primary.green() / 255f * thumbAlphaAnimator.get(),
                primary.blue() / 255f * thumbAlphaAnimator.get()
        );

        float contentViewportHeight = Math.max(
                0f,
                ctx.viewportHeight() - getContentPaddingTop() - getContentPaddingBottom()
        );

        ctx.save();

        if (shouldRenderTrackBackground()){
            ctx.roundedRect(
                    geometry.upArrow.x,
                    geometry.upArrow.y,
                    geometry.track.width,
                    contentViewportHeight,
                    geometry.thumb.width / 2f,
                    MaterialUI.Scheme.surfaceVariant(),
                    true
            );
        }

        ctx.roundedRect(
                geometry.thumb.x,
                geometry.thumb.y,
                geometry.thumb.width,
                geometry.thumb.height,
                geometry.thumb.width / 2f,
                thumbColor,
                true
        );
        drawArrow(ctx, geometry.upArrow, true, upArrowScaleAnimator.get());
        drawArrow(ctx, geometry.downArrow, false, downArrowScaleAnimator.get());
        ctx.restore();
    }

    @Override
    public void dispatchMouseMove(float x, float y) {
        if (scrollbarInteractionActive) {
            return;
        }

        ScrollbarGeometry geometry = scrollbarGeometry();
        setThumbHovered(geometry.visible && geometry.thumbHitbox.in(x, y));
        super.dispatchMouseMove(x, y);
    }

    @Override
    public void dispatchMousePress(float x, float y, int button) {
        if (button != 0) {
            super.dispatchMousePress(x, y, button);
            return;
        }

        ScrollbarGeometry geometry = scrollbarGeometry();
        if (!geometry.visible) {
            super.dispatchMousePress(x, y, button);
            return;
        }

        if (geometry.upArrow.in(x, y)) {
            stopScroll();
            setThumbHovered(false);
            setPressedArrow(-1);
            scrollbarInteractionActive = true;
            scrollBy(0f, -SCROLL_STEP);
            return;
        }

        if (geometry.downArrow.in(x, y)) {
            stopScroll();
            setThumbHovered(false);
            setPressedArrow(1);
            scrollbarInteractionActive = true;
            scrollBy(0f, SCROLL_STEP);
            return;
        }

        if (geometry.thumbHitbox.in(x, y)) {
            stopScroll();
            setThumbHovered(true);
            setPressedArrow(0);
            scrollbarInteractionActive = true;
            scrollbarDragging = true;
            thumbGrabOffsetY = y - geometry.thumb.y;
            return;
        }

        setPressedArrow(0);
        setThumbHovered(false);

        if (geometry.track.in(x, y)) {
            stopScroll();
            setThumbHovered(true);
            scrollbarInteractionActive = true;
            thumbGrabOffsetY = geometry.thumb.height / 2f;
            scrollToThumbPosition(y, geometry);
            scrollbarDragging = true;
            return;
        }

        super.dispatchMousePress(x, y, button);
    }

    @Override
    public void dispatchMouseRelease(float x, float y, int button) {
        if (button == 0 && scrollbarInteractionActive) {
            boolean wasDragging = scrollbarDragging;
            scrollbarInteractionActive = false;
            scrollbarDragging = false;
            setPressedArrow(0);
            if (wasDragging) {
                ScrollbarGeometry geometry = scrollbarGeometry();
                setThumbHovered(geometry.visible && geometry.thumbHitbox.in(x, y));
            } else {
                setThumbHovered(false);
            }
            return;
        }
        super.dispatchMouseRelease(x, y, button);
    }

    @Override
    public void dispatchMouseDrag(float mouseX, float mouseY, float dragX, float dragY, int button) {
        if (button == 0 && scrollbarInteractionActive) {
            if (scrollbarDragging) {
                scrollToThumbPosition(mouseY, scrollbarGeometry());
            }
            return;
        }
        super.dispatchMouseDrag(mouseX, mouseY, dragX, dragY, button);
    }

    private void setThumbHovered(boolean hovered) {
        if (thumbHovered == hovered) {
            return;
        }
        thumbHovered = hovered;
        animateTo(thumbAlphaAnimator, hovered ? THUMB_HOVER_ALPHA : THUMB_NORMAL_ALPHA);
    }

    private void setPressedArrow(int arrow) {
        if (pressedArrow == arrow) {
            return;
        }
        pressedArrow = arrow;
        animateTo(upArrowScaleAnimator, arrow < 0 ? ARROW_PRESSED_SCALE : 1f);
        animateTo(downArrowScaleAnimator, arrow > 0 ? ARROW_PRESSED_SCALE : 1f);
    }

    private void animateTo(Animator.FloatAnimator animator, float target) {
        if (Math.abs(animator.get() - target) < 0.001f) {
            return;
        }
        if (animator.isRunning()) {
            animator.cancel();
        }
        animator.fromTo(animator.get(), target).start();
    }

    private void scrollToThumbPosition(float mouseY, ScrollbarGeometry geometry) {
        if (geometry.thumbTravel <= 0f) {
            return;
        }

        float thumbTop = clamp(
                mouseY - geometry.track.y - thumbGrabOffsetY,
                0f,
                geometry.thumbTravel
        );
        float progress = thumbTop / geometry.thumbTravel;
        setScroll(getScrollX(), progress * geometry.maxScrollY);
    }

    private ScrollbarGeometry scrollbarGeometry() {
        AbstractWidget<?> root = getRoot();
        Rectangle viewport = getViewport();
        float contentViewportHeight = Math.max(
                0f,
                viewport.height - getContentPaddingTop() - getContentPaddingBottom()
        );
        float contentHeight = root == null ? 0f : Math.max(0f, root.getLayoutNode().getLayoutHeight());
        float maxScrollY = Math.max(0f, contentHeight - contentViewportHeight);

        if (!verticalScrollEnabled || viewport.width < SCROLLBAR_WIDTH || maxScrollY <= 0f) {
            return ScrollbarGeometry.hidden();
        }

        float scrollbarY = getContentPaddingTop();
        float scrollbarHeight = contentViewportHeight;
        float trackHeight = scrollbarHeight - ARROW_BUTTON_SIZE * 2f;
        if (trackHeight <= 0f) {
            return ScrollbarGeometry.hidden();
        }

        float scrollbarX = viewport.width - SCROLLBAR_WIDTH;
        Rectangle upArrow = new Rectangle(scrollbarX, scrollbarY, SCROLLBAR_WIDTH, ARROW_BUTTON_SIZE);
        Rectangle downArrow = new Rectangle(
                scrollbarX,
                scrollbarY + scrollbarHeight - ARROW_BUTTON_SIZE,
                SCROLLBAR_WIDTH,
                ARROW_BUTTON_SIZE
        );
        Rectangle track = new Rectangle(
                scrollbarX,
                scrollbarY + ARROW_BUTTON_SIZE,
                SCROLLBAR_WIDTH,
                trackHeight
        );
        float thumbHeight = Math.min(
                track.height,
                Math.max(MIN_THUMB_HEIGHT, track.height * contentViewportHeight / contentHeight)
        );
        float thumbTravel = Math.max(0f, track.height - thumbHeight);
        float scrollProgress = maxScrollY == 0f ? 0f : clamp(getScrollY() / maxScrollY, 0f, 1f);
        Rectangle thumb = new Rectangle(
                track.x + (track.width - THUMB_WIDTH) / 2f,
                track.y + thumbTravel * scrollProgress,
                THUMB_WIDTH,
                thumbHeight
        );
        Rectangle thumbHitbox = new Rectangle(
                track.x,
                thumb.y,
                track.width,
                thumb.height
        );
        return new ScrollbarGeometry(true, upArrow, downArrow, track, thumb, thumbHitbox, maxScrollY, thumbTravel);
    }

    private void drawArrow(RenderContext ctx, Rectangle bounds, boolean up, float scale) {
        float centerX = bounds.getCenterX();
        float centerY = bounds.getCenterY();
        float halfWidth = THUMB_WIDTH / 2f * scale;
        float halfHeight = 2f * scale;

        ctx.beginPath();
        ctx.fillColor(MaterialUI.Scheme.primary());
        if (up) {
            ctx.move(centerX, centerY - halfHeight);
            ctx.lineTo(centerX - halfWidth, centerY + halfHeight);
            ctx.lineTo(centerX + halfWidth, centerY + halfHeight);
        } else {
            ctx.move(centerX, centerY + halfHeight);
            ctx.lineTo(centerX - halfWidth, centerY - halfHeight);
            ctx.lineTo(centerX + halfWidth, centerY - halfHeight);
        }
        ctx.lineTo(centerX, up ? centerY - halfHeight : centerY + halfHeight);
        ctx.endPath(true);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static class ScrollbarGeometry {
        private final boolean visible;
        private final Rectangle upArrow;
        private final Rectangle downArrow;
        private final Rectangle track;
        private final Rectangle thumb;
        private final Rectangle thumbHitbox;
        private final float maxScrollY;
        private final float thumbTravel;

        private ScrollbarGeometry(
                boolean visible,
                Rectangle upArrow,
                Rectangle downArrow,
                Rectangle track,
                Rectangle thumb,
                Rectangle thumbHitbox,
                float maxScrollY,
                float thumbTravel
        ) {
            this.visible = visible;
            this.upArrow = upArrow;
            this.downArrow = downArrow;
            this.track = track;
            this.thumb = thumb;
            this.thumbHitbox = thumbHitbox;
            this.maxScrollY = maxScrollY;
            this.thumbTravel = thumbTravel;
        }

        private static ScrollbarGeometry hidden() {
            Rectangle empty = new Rectangle();
            return new ScrollbarGeometry(false, empty, empty, empty, empty, empty, 0f, 0f);
        }
    }
}
