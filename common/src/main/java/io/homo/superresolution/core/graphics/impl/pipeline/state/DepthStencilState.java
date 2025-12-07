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

package io.homo.superresolution.core.graphics.impl.pipeline.state;

public class DepthStencilState {
    private final boolean depthTestEnable;
    private final boolean depthWriteEnable;
    private final CompareOp depthCompareOp;
    private final boolean stencilTestEnable;

    public DepthStencilState(boolean depthTestEnable, boolean depthWriteEnable,
                             CompareOp depthCompareOp, boolean stencilTestEnable) {
        this.depthTestEnable = depthTestEnable;
        this.depthWriteEnable = depthWriteEnable;
        this.depthCompareOp = depthCompareOp;
        this.stencilTestEnable = stencilTestEnable;
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

    public static class Builder {
        private boolean depthTestEnable = false;
        private boolean depthWriteEnable = false;
        private CompareOp depthCompareOp = CompareOp.Less;
        private boolean stencilTestEnable = false;

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

        public DepthStencilState build() {
            return new DepthStencilState(depthTestEnable, depthWriteEnable,
                    depthCompareOp, stencilTestEnable);
        }
    }
}
