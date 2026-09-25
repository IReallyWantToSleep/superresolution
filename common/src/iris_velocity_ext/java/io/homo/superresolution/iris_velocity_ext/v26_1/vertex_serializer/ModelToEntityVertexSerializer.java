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

import io.homo.superresolution.iris_velocity_ext.v26_1.IrisExtVertexFormats;
import io.homo.superresolution.iris_velocity_ext.v26_1.VelocityRenderContext;
import net.caffeinemc.mods.sodium.api.memory.MemoryIntrinsics;
import net.caffeinemc.mods.sodium.api.util.NormI8;
import net.caffeinemc.mods.sodium.api.vertex.serializer.VertexSerializer;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.irisshaders.iris.vertices.IrisVertexFormats;
import net.irisshaders.iris.vertices.MemoryAccess;
import net.irisshaders.iris.vertices.NormalHelper;
import org.joml.Matrix4x3f;

import java.nio.ByteOrder;

public class ModelToEntityVertexSerializer implements VertexSerializer {
    private static final int MIDCOORD;
    private static final int TANGENT;
    private static final int VELOCITY;
    private static final int SRC_STRIDE = 36;
    private static final int DST_STRIDE;
    private static final boolean LITTLE_ENDIAN =
            ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;

    static {
        MIDCOORD = IrisExtVertexFormats.ENTITY_VELOCITY.getOffset(IrisVertexFormats.MID_TEXTURE_ELEMENT);
        TANGENT = IrisExtVertexFormats.ENTITY_VELOCITY.getOffset(IrisVertexFormats.TANGENT_ELEMENT);
        VELOCITY = IrisExtVertexFormats.ENTITY_VELOCITY.getOffset(IrisExtVertexFormats.VELOCITY_ELEMENT);

        DST_STRIDE = IrisExtVertexFormats.ENTITY_VELOCITY.getVertexSize();
    }

    public ModelToEntityVertexSerializer() {
        super();
    }

    static long pack2f(float lowAddr, float highAddr) {
        int lo = Float.floatToRawIntBits(lowAddr);
        int hi = Float.floatToRawIntBits(highAddr);
        return LITTLE_ENDIAN
                ? ((long) hi << 32) | (lo & 0xFFFFFFFFL)
                : ((long) lo << 32) | (hi & 0xFFFFFFFFL);
    }

    public void serialize(long srcBase, long dstBase, int vertexCount) {
        int quadCount = vertexCount >> 2;
        short entity = (short) CapturedRenderingState.INSTANCE.getCurrentRenderedEntity();
        short blockEntity = (short) CapturedRenderingState.INSTANCE.getCurrentRenderedBlockEntity();
        short item = (short) CapturedRenderingState.INSTANCE.getCurrentRenderedItem();
        long src = srcBase;
        long dst = dstBase;
        boolean shouldCalculateVelocity = VelocityRenderContext.currentTransformState != null;
        final long packedShorts =
                ((long) entity      & 0xFFFFL)
                        | (((long) blockEntity & 0xFFFFL) << 16)
                        | (((long) item        & 0xFFFFL) << 32);

        for (int q = 0; q < quadCount; ++q) {
            // We each every quad, and get their four vertex's `Position`, `UV` and others
            long v1 = src + SRC_STRIDE;
            long v2 = v1 + SRC_STRIDE;
            long v3 = v2 + SRC_STRIDE;

            int packedNormal = MemoryAccess.getInt(src + 32L);
            float nx = NormI8.unpackX(packedNormal);
            float ny = NormI8.unpackY(packedNormal);
            float nz = NormI8.unpackZ(packedNormal);
            float v0x = MemoryAccess.getFloat(src);
            float v0y = MemoryAccess.getFloat(src + 4L);
            float v0z = MemoryAccess.getFloat(src + 8L);
            float v0u = MemoryAccess.getFloat(src + 16L);
            float v0v = MemoryAccess.getFloat(src + 20L);
            float v1x = MemoryAccess.getFloat(v1);
            float v1y = MemoryAccess.getFloat(v1 + 4L);
            float v1z = MemoryAccess.getFloat(v1 + 8L);
            float v1u = MemoryAccess.getFloat(v1 + 16L);
            float v1v = MemoryAccess.getFloat(v1 + 20L);
            float v2x = MemoryAccess.getFloat(v2);
            float v2y = MemoryAccess.getFloat(v2 + 4L);
            float v2z = MemoryAccess.getFloat(v2 + 8L);
            float v2u = MemoryAccess.getFloat(v2 + 16L);
            float v2v = MemoryAccess.getFloat(v2 + 20L);
            float v3x = 0f, v3y = 0f, v3z = 0f;
            if (shouldCalculateVelocity) {
                // For calculate velocity only
                v3x = MemoryAccess.getFloat(v3);
                v3y = MemoryAccess.getFloat(v3 + 4L);
                v3z = MemoryAccess.getFloat(v3 + 8L);
            }

            int tangent = NormalHelper.computeTangent(null,
                    nx, ny, nz,
                    v0x, v0y, v0z, v0u, v0v,
                    v1x, v1y, v1z, v1u, v1v,
                    v2x, v2y, v2z, v2u, v2v);

            float midU = (v0u + v1u + v2u + MemoryAccess.getFloat(v3 + 16L)) * 0.25F;
            float midV = (v0v + v1v + v2v + MemoryAccess.getFloat(v3 + 20L)) * 0.25F;

            Matrix4x3f d = shouldCalculateVelocity
                    ? VelocityRenderContext.currentTransformState.delta : null;

            long writeSrc = src;
            long writeDst = dst;

            // In here, we write four vertex each quad. At the last, we write our velocity
            for (int vertexIndex = 0; vertexIndex < 4; ++vertexIndex) {
                MemoryIntrinsics.copyMemory(writeSrc, writeDst, 36);
                MemoryAccess.setLong(writeDst + 36L, packedShorts);

                MemoryAccess.setFloat(writeDst + (long) MIDCOORD, midU);
                MemoryAccess.setFloat(writeDst + (long) MIDCOORD + 4L, midV);
                MemoryAccess.setInt(writeDst + (long) TANGENT, tangent);

                if (shouldCalculateVelocity) {
                    float pX, pY, pZ;
                    switch (vertexIndex) {
                        case 0 -> {
                            pX = v0x;
                            pY = v0y;
                            pZ = v0z;
                        }
                        case 1 -> {
                            pX = v1x;
                            pY = v1y;
                            pZ = v1z;
                        }
                        case 2 -> {
                            pX = v2x;
                            pY = v2y;
                            pZ = v2z;
                        }
                        default -> {
                            pX = v3x;
                            pY = v3y;
                            pZ = v3z;
                        }
                    }
                    float vx = org.joml.Math.fma(d.m00(), pX,
                            org.joml.Math.fma(d.m10(), pY,
                                    org.joml.Math.fma(d.m20(), pZ, d.m30())));
                    float vy = org.joml.Math.fma(d.m01(), pX,
                            org.joml.Math.fma(d.m11(), pY,
                                    org.joml.Math.fma(d.m21(), pZ, d.m31())));
                    float vz = org.joml.Math.fma(d.m02(), pX,
                            org.joml.Math.fma(d.m12(), pY,
                                    org.joml.Math.fma(d.m22(), pZ, d.m32())));
                    MemoryAccess.setFloat(writeDst + (long) VELOCITY, vx);
                    MemoryAccess.setFloat(writeDst + (long) VELOCITY + 4L, vy);
                    MemoryAccess.setFloat(writeDst + (long) VELOCITY + 8L, vz);
                } else {

                    MemoryAccess.setFloat(writeDst + (long) VELOCITY, 0L);
                    MemoryAccess.setFloat(writeDst + (long) VELOCITY + 4L, 0L);
                    MemoryAccess.setFloat(writeDst + (long) VELOCITY + 8L, 0f);
                }

                writeSrc += SRC_STRIDE;
                writeDst += DST_STRIDE;
            }

            src += SRC_STRIDE * 4L;
            dst += DST_STRIDE * 4L;
        }
    }
}
