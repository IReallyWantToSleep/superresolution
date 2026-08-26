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

package io.homo.superresolution.iris_velocity_ext.v26_1;

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;

import java.util.Map;

public final class VelocityCache {
    private final Map<ModelPart, VelocityTransformState> parts = new Reference2ObjectOpenHashMap<>();
    private final Map<BakedQuad, VelocityVertexState[]> quads = new Reference2ObjectOpenHashMap<>();

    public VelocityTransformState getOrCreatePartState(ModelPart part) {
        return parts.computeIfAbsent(part, key -> new VelocityTransformState());
    }

    public VelocityVertexState[] getOrCreateQuadStates(BakedQuad quad) {
        return quads.computeIfAbsent(quad, key -> {
            VelocityVertexState[] states = new VelocityVertexState[4];
            for (int vertex = 0; vertex < 4; vertex++) {
                states[vertex] = new VelocityVertexState();
            }
            return states;
        });
    }
}
