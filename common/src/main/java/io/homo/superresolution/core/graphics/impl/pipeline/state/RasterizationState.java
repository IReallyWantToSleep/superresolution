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

package io.homo.superresolution.core.graphics.impl.pipeline.state;

public class RasterizationState {
    private final PolygonMode polygonMode;
    private final CullMode cullMode;
    private final FrontFace frontFace;
    private final boolean depthClampEnable;
    private final boolean rasterizerDiscardEnable;

    public RasterizationState(PolygonMode polygonMode, CullMode cullMode, FrontFace frontFace,
                              boolean depthClampEnable, boolean rasterizerDiscardEnable) {
        this.polygonMode = polygonMode;
        this.cullMode = cullMode;
        this.frontFace = frontFace;
        this.depthClampEnable = depthClampEnable;
        this.rasterizerDiscardEnable = rasterizerDiscardEnable;
    }

    public static RasterizationState defaults() {
        return new RasterizationState(PolygonMode.Fill, CullMode.None,
                FrontFace.CounterClockwise, false, false);
    }

    public PolygonMode polygonMode() {
        return polygonMode;
    }

    public CullMode cullMode() {
        return cullMode;
    }

    public FrontFace frontFace() {
        return frontFace;
    }

    public boolean depthClampEnable() {
        return depthClampEnable;
    }

    public boolean rasterizerDiscardEnable() {
        return rasterizerDiscardEnable;
    }

    public static class Builder {
        private PolygonMode polygonMode = PolygonMode.Fill;
        private CullMode cullMode = CullMode.None;
        private FrontFace frontFace = FrontFace.CounterClockwise;
        private boolean depthClampEnable = false;
        private boolean rasterizerDiscardEnable = false;

        public Builder polygonMode(PolygonMode mode) {
            this.polygonMode = mode;
            return this;
        }

        public Builder cullMode(CullMode mode) {
            this.cullMode = mode;
            return this;
        }

        public Builder frontFace(FrontFace face) {
            this.frontFace = face;
            return this;
        }

        public Builder depthClampEnable(boolean enable) {
            this.depthClampEnable = enable;
            return this;
        }

        public Builder rasterizerDiscardEnable(boolean enable) {
            this.rasterizerDiscardEnable = enable;
            return this;
        }

        public RasterizationState build() {
            return new RasterizationState(polygonMode, cullMode, frontFace,
                    depthClampEnable, rasterizerDiscardEnable);
        }
    }
}
