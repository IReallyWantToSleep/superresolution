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

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.iris_velocity_ext.v26_1.VelocityBufferBuilderAccess;
import io.homo.superresolution.iris_velocity_ext.v26_1.VelocityCalc;
import io.homo.superresolution.iris_velocity_ext.v26_1.VelocityRenderContext;
import io.homo.superresolution.iris_velocity_ext.v26_1.VelocityVertexState;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelPart.Cube.class)
public abstract class ModelPartCubeVelocityMixin {
    @Shadow
    @Final
    public ModelPart.Polygon[] polygons;

    @Unique
    private Long2ObjectOpenHashMap<VelocityVertexState[][]> irisExt$velocityCache;

    @Inject(method = "compile", at = @At("HEAD"))
    private void irisExt$attach(PoseStack.Pose pose, VertexConsumer builder, int lightCoords, int overlayCoords, int color, CallbackInfo ci) {
        if (!SuperResolutionConfig.isIrisExtensionEnabledAtStartup()) {
            return;
        }
        if (!VelocityRenderContext.hasKey || !(builder instanceof VelocityBufferBuilderAccess access)) {
            return;
        }
        if (irisExt$velocityCache == null) {
            irisExt$velocityCache = new Long2ObjectOpenHashMap<>();
        }
        VelocityVertexState[][] states = irisExt$velocityCache.get(VelocityRenderContext.currentKey);
        if (states == null) {
            states = new VelocityVertexState[this.polygons.length][4];
            for (int polygon = 0; polygon < states.length; polygon++) {
                for (int vertex = 0; vertex < 4; vertex++) {
                    states[polygon][vertex] = new VelocityVertexState();
                }
            }
            irisExt$velocityCache.put(VelocityRenderContext.currentKey, states);
        } else {
            irisExt$prune();
        }
        // One bucket shares a single access stamp (all of its states are written together).
        states[0][0].lastAccessFrame = VelocityCalc.frameId;
        access.irisExt$attachCubeStates(states);
    }

    @Inject(method = "compile", at = @At("RETURN"))
    private void irisExt$detach(PoseStack.Pose pose, VertexConsumer builder, int lightCoords, int overlayCoords, int color, CallbackInfo ci) {
        if (!SuperResolutionConfig.isIrisExtensionEnabledAtStartup()) {
            return;
        }
        if (builder instanceof VelocityBufferBuilderAccess access) {
            access.irisExt$detachStates();
        }
    }

    @Unique
    private void irisExt$prune() {
        if (irisExt$velocityCache.size() < 8) {
            return;
        }
        var iterator = irisExt$velocityCache.long2ObjectEntrySet().fastIterator();
        while (iterator.hasNext()) {
            VelocityVertexState[][] bucket = iterator.next().getValue();
            if (bucket.length > 0 && bucket[0].length > 0
                    && VelocityCalc.frameId - bucket[0][0].lastAccessFrame > VelocityCalc.EVICT_AFTER_FRAMES) {
                iterator.remove();
            }
        }
    }
}
