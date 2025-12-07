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

package io.homo.superresolution.core.graphics.impl.pipeline;

import java.util.HashMap;
import java.util.Map;

public class PassClearState {
    private final Map<Integer, float[]> colorClearValuesOnBegin;
    private final Float depthClearValueOnBegin;
    private final Integer stencilClearValueOnBegin;

    private final Map<Integer, float[]> colorClearValuesOnEnd;
    private final Float depthClearValueOnEnd;
    private final Integer stencilClearValueOnEnd;

    private PassClearState(Map<Integer, float[]> colorClearValuesOnBegin,
                           Float depthClearValueOnBegin,
                           Integer stencilClearValueOnBegin,
                           Map<Integer, float[]> colorClearValuesOnEnd,
                           Float depthClearValueOnEnd,
                           Integer stencilClearValueOnEnd) {
        this.colorClearValuesOnBegin = new HashMap<>(colorClearValuesOnBegin);
        this.depthClearValueOnBegin = depthClearValueOnBegin;
        this.stencilClearValueOnBegin = stencilClearValueOnBegin;
        this.colorClearValuesOnEnd = new HashMap<>(colorClearValuesOnEnd);
        this.depthClearValueOnEnd = depthClearValueOnEnd;
        this.stencilClearValueOnEnd = stencilClearValueOnEnd;
    }

    public boolean shouldClearColorOnBegin(int index) {
        return colorClearValuesOnBegin.containsKey(index);
    }

    public boolean shouldClearDepthOnBegin() {
        return depthClearValueOnBegin != null;
    }

    public boolean shouldClearStencilOnBegin() {
        return stencilClearValueOnBegin != null;
    }

    public boolean shouldClearColorOnEnd(int index) {
        return colorClearValuesOnEnd.containsKey(index);
    }

    public boolean shouldClearDepthOnEnd() {
        return depthClearValueOnEnd != null;
    }

    public boolean shouldClearStencilOnEnd() {
        return stencilClearValueOnEnd != null;
    }

    public float[] getColorClearValueOnBegin(int index) {
        return colorClearValuesOnBegin.get(index);
    }

    public float getDepthClearValueOnBegin() {
        return depthClearValueOnBegin != null ? depthClearValueOnBegin : 1.0f;
    }

    public int getStencilClearValueOnBegin() {
        return stencilClearValueOnBegin != null ? stencilClearValueOnBegin : 0;
    }

    public float[] getColorClearValueOnEnd(int index) {
        return colorClearValuesOnEnd.get(index);
    }

    public float getDepthClearValueOnEnd() {
        return depthClearValueOnEnd != null ? depthClearValueOnEnd : 1.0f;
    }

    public int getStencilClearValueOnEnd() {
        return stencilClearValueOnEnd != null ? stencilClearValueOnEnd : 0;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Map<Integer, float[]> colorClearValuesOnBegin = new HashMap<>();
        private Float depthClearValueOnBegin;
        private Integer stencilClearValueOnBegin;

        private final Map<Integer, float[]> colorClearValuesOnEnd = new HashMap<>();
        private Float depthClearValueOnEnd;
        private Integer stencilClearValueOnEnd;

        public Builder clearColorOnBegin(int index, float r, float g, float b, float a) {
            colorClearValuesOnBegin.put(index, new float[]{r, g, b, a});
            return this;
        }

        public Builder clearDepthOnBegin(float depth) {
            this.depthClearValueOnBegin = depth;
            return this;
        }

        public Builder clearStencilOnBegin(int stencil) {
            this.stencilClearValueOnBegin = stencil;
            return this;
        }

        public Builder clearColorOnEnd(int index, float r, float g, float b, float a) {
            colorClearValuesOnEnd.put(index, new float[]{r, g, b, a});
            return this;
        }

        public Builder clearDepthOnEnd(float depth) {
            this.depthClearValueOnEnd = depth;
            return this;
        }

        public Builder clearStencilOnEnd(int stencil) {
            this.stencilClearValueOnEnd = stencil;
            return this;
        }

        public PassClearState build() {
            return new PassClearState(
                    colorClearValuesOnBegin, depthClearValueOnBegin, stencilClearValueOnBegin,
                    colorClearValuesOnEnd, depthClearValueOnEnd, stencilClearValueOnEnd
            );
        }
    }
}
