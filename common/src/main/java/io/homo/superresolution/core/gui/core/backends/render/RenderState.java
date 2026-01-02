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

package io.homo.superresolution.core.gui.core.backends.render;

import io.homo.superresolution.core.gui.core.backends.interfaces.Transform;
import org.joml.Vector2f;

public class RenderState {
    private float alpha = 1.0f;
    private Transform transform = Transform.identity();
    private ScissorRect scissor = null;

    public RenderState() {
    }

    public RenderState(float alpha, Transform transform, ScissorRect scissor) {
        this.alpha = alpha;
        this.transform = transform.copy();
        this.scissor = scissor != null ? scissor.copy() : null;
    }

    public RenderState copy() {
        return new RenderState(alpha, transform, scissor);
    }

    public void copyFrom(RenderState other) {
        this.alpha = other.alpha;
        this.transform = other.transform.copy();
        this.scissor = other.scissor != null ? other.scissor.copy() : null;
    }

    public void reset() {
        this.alpha = 1.0f;
        this.transform = Transform.identity();
        this.scissor = null;
    }

    public float alpha() {
        return alpha;
    }

    public RenderState alpha(float alpha) {
        this.alpha = alpha;
        return this;
    }

    public RenderState multiplyAlpha(float alpha) {
        this.alpha *= alpha;
        return this;
    }

    public Transform transform() {
        return transform;
    }

    public RenderState transform(Transform transform) {
        this.transform = transform.copy();
        return this;
    }

    public RenderState translate(float x, float y) {
        this.transform.translate(x, y);
        return this;
    }

    public RenderState scale(float sx, float sy) {
        this.transform.scale(sx, sy);
        return this;
    }

    public RenderState rotate(float radians) {
        this.transform.rotate(radians);
        return this;
    }

    public Vector2f transformPoint(float x, float y) {
        return transform.transformPoint(new Vector2f(x, y));
    }

    public Vector2f transformPoint(Vector2f point) {
        return transform.transformPoint(point);
    }

    public ScissorRect scissor() {
        return scissor;
    }

    public RenderState scissor(float x, float y, float width, float height) {
        this.scissor = new ScissorRect(x, y, width, height);
        return this;
    }

    public RenderState scissor(ScissorRect scissor) {
        this.scissor = scissor != null ? scissor.copy() : null;
        return this;
    }

    public RenderState resetScissor() {
        this.scissor = null;
        return this;
    }

    public boolean hasScissor() {
        return scissor != null;
    }

    public record ScissorRect(float x, float y, float width, float height) {
        public ScissorRect copy() {
            return new ScissorRect(x, y, width, height);
        }
    }
}
