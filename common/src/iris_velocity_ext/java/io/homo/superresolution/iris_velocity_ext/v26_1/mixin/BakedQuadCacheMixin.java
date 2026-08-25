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

package io.homo.superresolution.iris_velocity_ext.v26_1.mixin;

import io.homo.superresolution.iris_velocity_ext.v26_1.VelocityCalc;
import io.homo.superresolution.iris_velocity_ext.v26_1.VelocityQuadCacheHolder;
import io.homo.superresolution.iris_velocity_ext.v26_1.VelocityVertexState;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(BakedQuad.class)
public abstract class BakedQuadCacheMixin implements VelocityQuadCacheHolder {
    @Unique
    private Long2ObjectOpenHashMap<VelocityVertexState[]> irisExt$velocityCache;

    @Override
    public VelocityVertexState[] irisExt$getOrCreateStates(long key) {
        if (irisExt$velocityCache == null) {
            irisExt$velocityCache = new Long2ObjectOpenHashMap<>();
        }
        VelocityVertexState[] states = irisExt$velocityCache.get(key);
        if (states == null) {
            states = new VelocityVertexState[4];
            for (int vertex = 0; vertex < 4; vertex++) {
                states[vertex] = new VelocityVertexState();
            }
            irisExt$velocityCache.put(key, states);
        } else {
            irisExt$prune();
        }
        states[0].lastAccessFrame = VelocityCalc.frameId;
        return states;
    }

    @Unique
    private void irisExt$prune() {
        if (irisExt$velocityCache.size() < 8) {
            return;
        }
        var iterator = irisExt$velocityCache.long2ObjectEntrySet().fastIterator();
        while (iterator.hasNext()) {
            VelocityVertexState[] bucket = iterator.next().getValue();
            if (bucket.length > 0
                    && VelocityCalc.frameId - bucket[0].lastAccessFrame > VelocityCalc.EVICT_AFTER_FRAMES) {
                iterator.remove();
            }
        }
    }
}
