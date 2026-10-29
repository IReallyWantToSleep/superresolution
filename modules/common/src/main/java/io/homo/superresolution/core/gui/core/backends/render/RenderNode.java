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

public abstract class RenderNode {
    protected float zIndex;
    protected RenderLayer layer;
    protected RenderState capturedState;

    public RenderNode() {
        this(0, RenderLayer.Content);
    }

    public RenderNode(float zIndex, RenderLayer layer) {
        this.zIndex = zIndex;
        this.layer = layer;
    }

    public abstract void render(RenderContext ctx);

    public float zIndex() {
        return zIndex;
    }

    public RenderNode zIndex(float zIndex) {
        this.zIndex = zIndex;
        return this;
    }

    public RenderLayer layer() {
        return layer;
    }

    public RenderNode layer(RenderLayer layer) {
        this.layer = layer;
        return this;
    }

    public RenderState capturedState() {
        return capturedState;
    }

    public RenderNode capturedState(RenderState state) {
        this.capturedState = state;
        return this;
    }

    protected void applyState(RenderContext ctx) {
        if (capturedState != null) {
            ctx.applyState(capturedState);
        }
    }

    protected void restoreState(RenderContext ctx) {
        if (capturedState != null) {
            ctx.restoreState();
        }
    }
}
