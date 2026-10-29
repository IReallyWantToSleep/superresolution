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

import io.homo.superresolution.iris_velocity_ext.v26_1.VelocityCache;
import io.homo.superresolution.iris_velocity_ext.v26_1.VelocityCacheHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin({Entity.class, BlockEntity.class})
public class VelocityCacheHolderMixin implements VelocityCacheHolder {
    @Unique
    private VelocityCache irisExt$velocityCache;

    @Override
    public VelocityCache irisExt$getOrCreateVelocityCache() {
        if (irisExt$velocityCache == null) {
            irisExt$velocityCache = new VelocityCache();
        }
        return irisExt$velocityCache;
    }
}
