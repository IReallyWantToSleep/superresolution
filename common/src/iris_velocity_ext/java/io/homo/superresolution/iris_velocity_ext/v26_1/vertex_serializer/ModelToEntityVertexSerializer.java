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

package io.homo.superresolution.iris_velocity_ext.v26_1.vertex_serializer;

import io.homo.superresolution.iris_velocity_ext.v26_1.VelocityRenderContext;
import net.caffeinemc.mods.sodium.api.vertex.serializer.VertexSerializer;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import org.joml.Matrix4x3fc;

public class ModelToEntityVertexSerializer implements VertexSerializer {

    public ModelToEntityVertexSerializer() {
        super();
    }

    public void serialize(long srcBase, long dstBase, int vertexCount) {

        final short entity      = (short) CapturedRenderingState.INSTANCE.getCurrentRenderedEntity();
        final short blockEntity = (short) CapturedRenderingState.INSTANCE.getCurrentRenderedBlockEntity();
        final short item        = (short) CapturedRenderingState.INSTANCE.getCurrentRenderedItem();

        final Matrix4x3fc delta =
                VelocityRenderContext.currentTransformState != null
                        ? VelocityRenderContext.currentTransformState.delta
                        : null;

        NativeSerializer.callNativeSerializer(
                srcBase,
                dstBase,
                vertexCount,
                entity,
                blockEntity,
                item,
                delta
        );
    }
}
