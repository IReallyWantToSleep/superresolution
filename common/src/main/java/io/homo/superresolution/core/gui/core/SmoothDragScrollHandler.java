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

    private Vector2f offset = new Vector2f();
    private Vector2f targetOffset = new Vector2f();
    private Vector2f velocity = new Vector2f();
    private OnOffsetChangedListener listener;

    private Vector2f minOffset = new Vector2f(Float.NEGATIVE_INFINITY);
    private Vector2f maxOffset = new Vector2f(Float.POSITIVE_INFINITY);
    private boolean enableBounds = false;
    private boolean dragging = false;
    private float smoothTime = 50f;
    private float scrollStep = 40f;

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
            scrollBy(delta.mul(-1.8f));
        }
    }

    @Override
    public void onDragEnd(Vector2f pos) {
        dragging = false;
    }

    @Override
    public void onScroll(float deltaX, float deltaY) {
        targetOffset.add(deltaX * scrollStep, -deltaY * scrollStep);
        applyBoundsToTarget();
    }

    @Override
    public void scrollTo(Vector2f target) {
        targetOffset.set(target);
        applyBoundsToTarget();
    }

    @Override
    public void setScroll(Vector2f target) {
        offset.set(target);
        targetOffset.set(target);
        notifyOffset();
    }

    @Override
    public void scrollBy(Vector2f delta) {
        targetOffset.add(delta);
        applyBoundsToTarget();
    }

    @Override
    public void update(float deltaTime) {
        float factor = 1.0f - (float) Math.exp(-deltaTime / smoothTime);
        offset.lerp(targetOffset, factor);
        notifyOffset();
    }

    @Override
    public void stop() {
        velocity.zero();
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
    public void setScrollBounds(Vector2f min, Vector2f max) {
        this.minOffset.set(min);
        this.maxOffset.set(max);
        this.enableBounds = true;
        applyBoundsToTarget();
        applyBoundsToOffset();
    }

    @Override
    public void clearScrollBounds() {
        this.enableBounds = false;
        this.minOffset.set(Float.NEGATIVE_INFINITY);
        this.maxOffset.set(Float.POSITIVE_INFINITY);
    }

    private void applyBoundsToTarget() {
        if (enableBounds) {
            targetOffset = MathUtil.clamp(targetOffset, minOffset, maxOffset);
        }
    }

    private void applyBoundsToOffset() {
        if (enableBounds) {
            offset = MathUtil.clamp(offset, minOffset, maxOffset);
        }
    }

    private void notifyOffset() {
        if (listener != null) {
            listener.onOffsetChanged(new Vector2f(offset));
        }
    }
}
