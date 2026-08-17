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

package io.homo.superresolution.api;

public class InitializationDescription {

    private boolean isHdrInput;
    private boolean isAutoExposure;
    private boolean isMotionJittered;

    public InitializationDescription() {
    }

    public static InitializationDescription defaults() {
        return new InitializationDescription();
    }

    public boolean isMotionJittered() {
        return isMotionJittered;
    }

    public InitializationDescription setMotionJittered(boolean motionJittered) {
        isMotionJittered = motionJittered;
        return this;
    }

    public boolean isHdrInput() {
        return isHdrInput;
    }

    public InitializationDescription setHdrInput(boolean isHdrInput) {
        this.isHdrInput = isHdrInput;
        return this;
    }

    public boolean isAutoExposure() {
        return isAutoExposure;
    }

    public InitializationDescription setAutoExposure(boolean autoExposure) {
        isAutoExposure = autoExposure;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InitializationDescription that)) return false;
        return isHdrInput == that.isHdrInput
                && isAutoExposure == that.isAutoExposure
                && isMotionJittered == that.isMotionJittered;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(isHdrInput, isAutoExposure, isMotionJittered);
    }
}
