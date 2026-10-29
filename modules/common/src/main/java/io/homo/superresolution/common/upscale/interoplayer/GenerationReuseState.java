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

package io.homo.superresolution.common.upscale.interoplayer;

final class GenerationReuseState<G> {
    private G invalidGeneration;

    boolean recordDispatchOutcome(
            G activeGeneration,
            G dispatchedGeneration,
            boolean generationReusable) {
        if (generationReusable || dispatchedGeneration == null ||
                activeGeneration != dispatchedGeneration) {
            return false;
        }
        invalidGeneration = dispatchedGeneration;
        return true;
    }

    boolean invalidateActive(G activeGeneration, G generation) {
        return recordDispatchOutcome(activeGeneration, generation, false);
    }

    boolean requiresRebuild(G generation) {
        return generation != null && invalidGeneration == generation;
    }

    void clearIfInvalid(G generation) {
        if (invalidGeneration == generation) {
            invalidGeneration = null;
        }
    }

    void reset() {
        invalidGeneration = null;
    }
}
