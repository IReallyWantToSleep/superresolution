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
}