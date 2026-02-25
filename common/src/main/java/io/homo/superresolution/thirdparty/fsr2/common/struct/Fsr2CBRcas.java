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

package io.homo.superresolution.thirdparty.fsr2.common.struct;

import io.homo.superresolution.core.graphics.impl.buffer.IBufferData;
import io.homo.superresolution.thirdparty.fsr2.common.Fsr2Context;
import io.homo.superresolution.thirdparty.fsr2.common.Fsr2Dimensions;
import io.homo.superresolution.thirdparty.fsr2.common.Fsr2DispatchDescription;
import io.homo.superresolution.thirdparty.fsr2.common.Fsr2Utils;
import org.lwjgl.system.MemoryUtil;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Fsr2CBRcas implements IBufferData {
    private final ByteBuffer container;

    public Fsr2CBRcas() {
        this.container = MemoryUtil.memCalloc((int) size());
        this.container.order(ByteOrder.LITTLE_ENDIAN);
    }

    public void update(Fsr2Context context, Fsr2DispatchDescription desc, Fsr2Dimensions dims) {
        container.clear();
        int[] rcasConfig = new int[4];
        float sharpness = (-2.0f * desc.sharpness) + 2.0f;
        Fsr2Utils.rcasCon(rcasConfig, sharpness);
        for (int i = 0; i < 4; ++i) {
            container.putInt(rcasConfig[i]);
        }
        container.position((int) size());
        container.flip();
    }

    @Override
    public ByteBuffer container() {
        return container.duplicate().rewind();
    }

    @Override
    public long size() {
        return 16;
    }

    @Override
    public void free() {
        MemoryUtil.memFree(container);
    }

    @Override
    public void put(byte[] src, long offset) {
        throw new RuntimeException();
    }

    @Override
    public void updatePartial(Buffer data, long offset, long length) {
        throw new RuntimeException();
    }

    @Override
    public void update(Buffer data) {
        throw new RuntimeException();
    }
}