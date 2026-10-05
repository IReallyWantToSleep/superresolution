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

import io.homo.superresolution.core.NativeLibManager;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;

public final class NativeSerializer {
    private static final MethodHandle SERIALIZER;

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

        SERIALIZER = linker.downcallHandle(
                fn,
                desc,
                Linker.Option.critical(false)
        );
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
            MemorySegment velocityDeltaMatrix
    ) {
        try {
            MemorySegment nativeDelta = velocityDeltaMatrix == null
                    ? MemorySegment.NULL
                    : velocityDeltaMatrix;
            SERIALIZER.invokeExact(
                    srcBase, dstBase, vertexCount,
                    entity, blockEntity, item,
                    nativeDelta
            );
        } catch (Throwable t) {
            throw new RuntimeException("Native serializer call failed", t);
        }
    }
}
