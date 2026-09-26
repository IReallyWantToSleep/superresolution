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

// NativeSerializer.java

import io.homo.superresolution.core.NativeLibManager;
import org.joml.Matrix4x3fc;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;

public final class NativeSerializer {
    private static final MethodHandle SERIALIZER;
    private static final ThreadLocal<MatrixScratch> MATRIX_SCRATCH =
            ThreadLocal.withInitial(MatrixScratch::new);

    private static final class MatrixScratch {
        private final Arena arena = Arena.ofConfined();
        private final MemorySegment segment = arena.allocate(ValueLayout.JAVA_FLOAT, 16);
    }

    static {
        if (!NativeLibManager.LIB_SUPER_RESOLUTION.available) {
            throw new IllegalStateException();
        }
        Linker linker = Linker.nativeLinker();
        SymbolLookup lookup = SymbolLookup.loaderLookup();
        MemorySegment fn = lookup.find("_superFastModelToEntityVertexSerializer")
                .orElseThrow(() -> new UnsatisfiedLinkError(
                        "_superFastModelToEntityVertexSerializer not found"));

        FunctionDescriptor desc = FunctionDescriptor.ofVoid(
                ValueLayout.JAVA_LONG,   // srcBase
                ValueLayout.JAVA_LONG,   // dstBase
                ValueLayout.JAVA_INT,    // vertexCount
                ValueLayout.JAVA_SHORT,  // entity
                ValueLayout.JAVA_SHORT,  // blockEntity
                ValueLayout.JAVA_SHORT,  // item
                ValueLayout.ADDRESS      // velocityDeltaMatrix
        );

        SERIALIZER = linker.downcallHandle(fn, desc);
    }

    private NativeSerializer() {
    }

    public static void callNativeSerializer(
            long srcBase,
            long dstBase,
            int vertexCount,
            short entity,
            short blockEntity,
            short item,
            Matrix4x3fc velocityDeltaMatrix
    ) {
        try {
            MemorySegment mat = MemorySegment.NULL;
            if (velocityDeltaMatrix != null) {
                final MemorySegment matrix = MATRIX_SCRATCH.get().segment;
                matrix.setAtIndex(ValueLayout.JAVA_FLOAT, 0, velocityDeltaMatrix.m00());
                matrix.setAtIndex(ValueLayout.JAVA_FLOAT, 1, velocityDeltaMatrix.m01());
                matrix.setAtIndex(ValueLayout.JAVA_FLOAT, 2, velocityDeltaMatrix.m02());
                matrix.setAtIndex(ValueLayout.JAVA_FLOAT, 3, 0.0f);
                matrix.setAtIndex(ValueLayout.JAVA_FLOAT, 4, velocityDeltaMatrix.m10());
                matrix.setAtIndex(ValueLayout.JAVA_FLOAT, 5, velocityDeltaMatrix.m11());
                matrix.setAtIndex(ValueLayout.JAVA_FLOAT, 6, velocityDeltaMatrix.m12());
                matrix.setAtIndex(ValueLayout.JAVA_FLOAT, 7, 0.0f);
                matrix.setAtIndex(ValueLayout.JAVA_FLOAT, 8, velocityDeltaMatrix.m20());
                matrix.setAtIndex(ValueLayout.JAVA_FLOAT, 9, velocityDeltaMatrix.m21());
                matrix.setAtIndex(ValueLayout.JAVA_FLOAT, 10, velocityDeltaMatrix.m22());
                matrix.setAtIndex(ValueLayout.JAVA_FLOAT, 11, 0.0f);
                matrix.setAtIndex(ValueLayout.JAVA_FLOAT, 12, velocityDeltaMatrix.m30());
                matrix.setAtIndex(ValueLayout.JAVA_FLOAT, 13, velocityDeltaMatrix.m31());
                matrix.setAtIndex(ValueLayout.JAVA_FLOAT, 14, velocityDeltaMatrix.m32());
                matrix.setAtIndex(ValueLayout.JAVA_FLOAT, 15, 0.0f);
                mat = matrix;
            }

            SERIALIZER.invokeExact(
                    srcBase, dstBase, vertexCount,
                    entity, blockEntity, item,
                    mat
            );
        } catch (Throwable t) {
            throw new RuntimeException("Native serializer call failed", t);
        }
    }
}
