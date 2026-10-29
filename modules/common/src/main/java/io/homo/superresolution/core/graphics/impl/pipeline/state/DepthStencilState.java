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

public class DepthStencilState {
    private final boolean depthTestEnable;
    private final boolean depthWriteEnable;
    private final CompareOp depthCompareOp;
    private final boolean stencilTestEnable;
    private final CompareOp stencilCompareOpFront;
    private final CompareOp stencilCompareOpBack;
    private final StencilOp stencilFailOpFront;
    private final StencilOp stencilPassOpFront;
    private final StencilOp stencilDepthFailOpFront;
    private final StencilOp stencilFailOpBack;
    private final StencilOp stencilPassOpBack;
    private final StencilOp stencilDepthFailOpBack;
    private final int stencilCompareMask;
    private final int stencilWriteMask;
    private final int stencilReference;

    public DepthStencilState(boolean depthTestEnable, boolean depthWriteEnable,
                             CompareOp depthCompareOp, boolean stencilTestEnable) {
        this(
                depthTestEnable,
                depthWriteEnable,
                depthCompareOp,
                stencilTestEnable,
                CompareOp.Always,
                CompareOp.Always,
                StencilOp.Keep,
                StencilOp.Keep,
                StencilOp.Keep,
                StencilOp.Keep,
                StencilOp.Keep,
                StencilOp.Keep,
                0xFF,
                0xFF,
                0
        );
    }

    public DepthStencilState(boolean depthTestEnable, boolean depthWriteEnable,
                             CompareOp depthCompareOp, boolean stencilTestEnable,
                             CompareOp stencilCompareOpFront, CompareOp stencilCompareOpBack,
                             StencilOp stencilFailOpFront, StencilOp stencilPassOpFront, StencilOp stencilDepthFailOpFront,
                             StencilOp stencilFailOpBack, StencilOp stencilPassOpBack, StencilOp stencilDepthFailOpBack,
                             int stencilCompareMask, int stencilWriteMask, int stencilReference) {
        this.depthTestEnable = depthTestEnable;
        this.depthWriteEnable = depthWriteEnable;
        this.depthCompareOp = depthCompareOp;
        this.stencilTestEnable = stencilTestEnable;
        this.stencilCompareOpFront = stencilCompareOpFront;
        this.stencilCompareOpBack = stencilCompareOpBack;
        this.stencilFailOpFront = stencilFailOpFront;
        this.stencilPassOpFront = stencilPassOpFront;
        this.stencilDepthFailOpFront = stencilDepthFailOpFront;
        this.stencilFailOpBack = stencilFailOpBack;
        this.stencilPassOpBack = stencilPassOpBack;
        this.stencilDepthFailOpBack = stencilDepthFailOpBack;
        this.stencilCompareMask = stencilCompareMask;
        this.stencilWriteMask = stencilWriteMask;
        this.stencilReference = stencilReference;
    }

    public static DepthStencilState disabled() {
        return new DepthStencilState(false, false, CompareOp.Less, false);
    }

    public boolean depthTestEnable() {
        return depthTestEnable;
    }

    public boolean depthWriteEnable() {
        return depthWriteEnable;
    }

    public CompareOp depthCompareOp() {
        return depthCompareOp;
    }

    public boolean stencilTestEnable() {
        return stencilTestEnable;
    }

    public CompareOp stencilCompareOpFront() {
        return stencilCompareOpFront;
    }

    public CompareOp stencilCompareOpBack() {
        return stencilCompareOpBack;
    }

    public StencilOp stencilFailOpFront() {
        return stencilFailOpFront;
    }

    public StencilOp stencilPassOpFront() {
        return stencilPassOpFront;
    }

    public StencilOp stencilDepthFailOpFront() {
        return stencilDepthFailOpFront;
    }

    public StencilOp stencilFailOpBack() {
        return stencilFailOpBack;
    }

    public StencilOp stencilPassOpBack() {
        return stencilPassOpBack;
    }

    public StencilOp stencilDepthFailOpBack() {
        return stencilDepthFailOpBack;
    }

    public int stencilCompareMask() {
        return stencilCompareMask;
    }

    public int stencilWriteMask() {
        return stencilWriteMask;
    }

    public int stencilReference() {
        return stencilReference;
    }

    public static class Builder {
        private boolean depthTestEnable = false;
        private boolean depthWriteEnable = false;
        private CompareOp depthCompareOp = CompareOp.Less;
        private boolean stencilTestEnable = false;
        private CompareOp stencilCompareOpFront = CompareOp.Always;
        private CompareOp stencilCompareOpBack = CompareOp.Always;
        private StencilOp stencilFailOpFront = StencilOp.Keep;
        private StencilOp stencilPassOpFront = StencilOp.Keep;
        private StencilOp stencilDepthFailOpFront = StencilOp.Keep;
        private StencilOp stencilFailOpBack = StencilOp.Keep;
        private StencilOp stencilPassOpBack = StencilOp.Keep;
        private StencilOp stencilDepthFailOpBack = StencilOp.Keep;
        private int stencilCompareMask = 0xFF;
        private int stencilWriteMask = 0xFF;
        private int stencilReference = 0;

        public Builder depthTestEnable(boolean enable) {
            this.depthTestEnable = enable;
            return this;
        }

        public Builder depthWriteEnable(boolean enable) {
            this.depthWriteEnable = enable;
            return this;
        }

        public Builder depthCompareOp(CompareOp op) {
            this.depthCompareOp = op;
            return this;
        }

        public Builder stencilTestEnable(boolean enable) {
            this.stencilTestEnable = enable;
            return this;
        }

        public Builder stencilCompareOp(CompareOp op) {
            this.stencilCompareOpFront = op;
            this.stencilCompareOpBack = op;
            return this;
        }

        public Builder stencilCompareOpFront(CompareOp op) {
            this.stencilCompareOpFront = op;
            return this;
        }

        public Builder stencilCompareOpBack(CompareOp op) {
            this.stencilCompareOpBack = op;
            return this;
        }

        public Builder stencilFailOp(StencilOp op) {
            this.stencilFailOpFront = op;
            this.stencilFailOpBack = op;
            return this;
        }

        public Builder stencilFailOpFront(StencilOp op) {
            this.stencilFailOpFront = op;
            return this;
        }

        public Builder stencilFailOpBack(StencilOp op) {
            this.stencilFailOpBack = op;
            return this;
        }

        public Builder stencilPassOp(StencilOp op) {
            this.stencilPassOpFront = op;
            this.stencilPassOpBack = op;
            return this;
        }

        public Builder stencilPassOpFront(StencilOp op) {
            this.stencilPassOpFront = op;
            return this;
        }

        public Builder stencilPassOpBack(StencilOp op) {
            this.stencilPassOpBack = op;
            return this;
        }

        public Builder stencilDepthFailOp(StencilOp op) {
            this.stencilDepthFailOpFront = op;
            this.stencilDepthFailOpBack = op;
            return this;
        }

        public Builder stencilDepthFailOpFront(StencilOp op) {
            this.stencilDepthFailOpFront = op;
            return this;
        }

        public Builder stencilDepthFailOpBack(StencilOp op) {
            this.stencilDepthFailOpBack = op;
            return this;
        }

        public Builder stencilCompareMask(int mask) {
            this.stencilCompareMask = mask;
            return this;
        }

        public Builder stencilWriteMask(int mask) {
            this.stencilWriteMask = mask;
            return this;
        }

        public Builder stencilReference(int reference) {
            this.stencilReference = reference;
            return this;
        }

        public DepthStencilState build() {
            return new DepthStencilState(depthTestEnable, depthWriteEnable,
                    depthCompareOp, stencilTestEnable,
                    stencilCompareOpFront, stencilCompareOpBack,
                    stencilFailOpFront, stencilPassOpFront, stencilDepthFailOpFront,
                    stencilFailOpBack, stencilPassOpBack, stencilDepthFailOpBack,
                    stencilCompareMask, stencilWriteMask, stencilReference);
        }
    }
}
