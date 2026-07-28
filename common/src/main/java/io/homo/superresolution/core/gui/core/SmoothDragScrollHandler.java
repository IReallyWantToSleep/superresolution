/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
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

import io.homo.superresolution.core.math.MathUtil;
import org.joml.Vector2f;

public class SmoothDragScrollHandler implements IScrollHandler {

    private static final float DEFAULT_SCROLL_STEP = 40.0f;
    private static final float DEFAULT_ANIMATION_DURATION_MILLIS = 500.0f;
    private static final float DRAG_SCROLL_SCALE = -1.8f;
    private static final float EPSILON = 0.0001f;

    private static final double EASING_X1 = 0.27;
    private static final double EASING_Y1 = 1.06;
    private static final double EASING_X2 = 0.18;
    private static final double EASING_Y2 = 1.0;

    private final Vector2f offset = new Vector2f();
    private final Vector2f targetOffset = new Vector2f();
    private final Vector2f animationStartOffset = new Vector2f();
    private final Vector2f minOffset = new Vector2f();
    private final Vector2f maxOffset = new Vector2f();
    private OnOffsetChangedListener listener;

    private boolean dragging = false;
    private boolean animating = false;
    private float elapsedMillis = 0.0f;

    public SmoothDragScrollHandler(OnOffsetChangedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onDragStart(Vector2f pos) {
        dragging = true;
    }

    @Override
    public void onDragMove(Vector2f pos, Vector2f delta) {
        if (dragging) {
            scrollBy(new Vector2f(delta).mul(DRAG_SCROLL_SCALE));
        }
    }

    @Override
    public void onDragEnd(Vector2f pos) {
        dragging = false;
    }

    @Override
    public void onScroll(float deltaX, float deltaY) {
        targetOffset.add(deltaX * DEFAULT_SCROLL_STEP, -deltaY * DEFAULT_SCROLL_STEP);
        applyBoundsToTarget();
        restartAnimation();
    }

    @Override
    public void scrollTo(Vector2f target) {
        targetOffset.set(target);
        applyBoundsToTarget();
        restartAnimation();
    }

    @Override
    public void setScroll(Vector2f target) {
        targetOffset.set(target);
        applyBoundsToTarget();
        offset.set(targetOffset);
        animationStartOffset.set(offset);
        elapsedMillis = 0.0f;
        animating = false;
        notifyOffset();
    }

    @Override
    public void scrollBy(Vector2f delta) {
        targetOffset.add(delta);
        applyBoundsToTarget();
        restartAnimation();
    }

    @Override
    public void update(float deltaTime) {
        if (!animating || !Float.isFinite(deltaTime) || deltaTime <= 0.0f) {
            return;
        }

        elapsedMillis = Math.min(
                DEFAULT_ANIMATION_DURATION_MILLIS,
                elapsedMillis + deltaTime
        );
        float fraction = elapsedMillis / DEFAULT_ANIMATION_DURATION_MILLIS;
        float easedFraction = materialSpatialEasing(fraction);
        offset.x = animationStartOffset.x
                + (targetOffset.x - animationStartOffset.x) * easedFraction;
        offset.y = animationStartOffset.y
                + (targetOffset.y - animationStartOffset.y) * easedFraction;
        applyBoundsToOffset();

        if (elapsedMillis >= DEFAULT_ANIMATION_DURATION_MILLIS) {
            offset.set(targetOffset);
            applyBoundsToOffset();
            animationStartOffset.set(offset);
            elapsedMillis = 0.0f;
            animating = false;
        }

        notifyOffset();
    }

    @Override
    public void stop() {
        applyBoundsToOffset();
        targetOffset.set(offset);
        animationStartOffset.set(offset);
        elapsedMillis = 0.0f;
        animating = false;
    }

    @Override
    public Vector2f getCurrentOffset() {
        return new Vector2f(offset);
    }

    @Override
    public void setOnOffsetChanged(OnOffsetChangedListener listener) {
        this.listener = listener;
    }

    @Override
    public void setScrollMetrics(ScrollMetrics metrics) {
        ScrollMetrics checkedMetrics = java.util.Objects.requireNonNull(metrics, "metrics");
        float oldTargetX = targetOffset.x;
        float oldTargetY = targetOffset.y;
        float oldOffsetX = offset.x;
        float oldOffsetY = offset.y;

        minOffset.zero();
        maxOffset.set(
                checkedMetrics.horizontalEnabled()
                        ? Math.max(0.0f, checkedMetrics.contentWidth() - checkedMetrics.viewportWidth())
                        : 0.0f,
                checkedMetrics.verticalEnabled()
                        ? Math.max(0.0f, checkedMetrics.contentHeight() - checkedMetrics.viewportHeight())
                        : 0.0f
        );
        applyBoundsToTarget();
        applyBoundsToOffset();

        boolean targetChanged = !close(oldTargetX, targetOffset.x) || !close(oldTargetY, targetOffset.y);
        boolean offsetChanged = !close(oldOffsetX, offset.x) || !close(oldOffsetY, offset.y);
        if (animating && targetChanged) {
            restartAnimation();
        } else if (offsetChanged && !animating) {
            animationStartOffset.set(offset);
        }
    }

    private void restartAnimation() {
        applyBoundsToTarget();
        if (close(offset.x, targetOffset.x) && close(offset.y, targetOffset.y)) {
            offset.set(targetOffset);
            animationStartOffset.set(offset);
            elapsedMillis = 0.0f;
            animating = false;
            return;
        }

        animationStartOffset.set(offset);
        elapsedMillis = 0.0f;
        animating = true;
    }

    private void applyBoundsToTarget() {
        targetOffset.x = MathUtil.clamp(targetOffset.x, minOffset.x, maxOffset.x);
        targetOffset.y = MathUtil.clamp(targetOffset.y, minOffset.y, maxOffset.y);
    }

    private void applyBoundsToOffset() {
        offset.x = MathUtil.clamp(offset.x, minOffset.x, maxOffset.x);
        offset.y = MathUtil.clamp(offset.y, minOffset.y, maxOffset.y);
    }

    private static float materialSpatialEasing(float fraction) {
        if (fraction <= 0.0f) {
            return 0.0f;
        }
        if (fraction >= 1.0f) {
            return 1.0f;
        }

        double low = 0.0;
        double high = 1.0;
        double parameter = fraction;
        for (int i = 0; i < 24; i++) {
            parameter = (low + high) / 2.0;
            double estimate = cubicCoordinate(EASING_X1, EASING_X2, parameter);
            if (estimate < fraction) {
                low = parameter;
            } else {
                high = parameter;
            }
        }
        return (float) cubicCoordinate(EASING_Y1, EASING_Y2, parameter);
    }

    private static double cubicCoordinate(double firstControl, double secondControl, double parameter) {
        double inverse = 1.0 - parameter;
        return 3.0 * firstControl * inverse * inverse * parameter
                + 3.0 * secondControl * inverse * parameter * parameter
                + parameter * parameter * parameter;
    }

    private static boolean close(float first, float second) {
        return Math.abs(first - second) <= EPSILON;
    }

    private void notifyOffset() {
        if (listener != null) {
            listener.onOffsetChanged(new Vector2f(offset));
        }
    }
}
