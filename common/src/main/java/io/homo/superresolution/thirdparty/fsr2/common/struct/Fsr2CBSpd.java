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

public class Fsr2CBSpd implements IBufferData {
    private final ByteBuffer container;
    private final int[] workGroupOffset = new int[2];
    private final int[] renderSize = new int[2];
    private int numWorkGroups = 0;
    private int mips = 0;

    public Fsr2CBSpd() {
        this.container = MemoryUtil.memCalloc((int) size());
        this.container.order(ByteOrder.LITTLE_ENDIAN);
    }

    public void update(Fsr2Context context, Fsr2DispatchDescription desc, Fsr2Dimensions dims) {
        int[] dispatchThreadGroupCountXY = new int[2];
        int[] workGroupOffset = new int[2];
        int[] numWorkGroupsAndMips = new int[2];

        int[] rectInfo = new int[]{
                0, 0,
                (int) desc.renderSize().x,
                (int) desc.renderSize().y
        };

        Fsr2Utils.spdSetup(
                dispatchThreadGroupCountXY,
                workGroupOffset,
                numWorkGroupsAndMips,
                rectInfo
        );

        this.numWorkGroups = numWorkGroupsAndMips[0];
        this.mips = numWorkGroupsAndMips[1];
        System.arraycopy(workGroupOffset, 0, this.workGroupOffset, 0, 2);
        this.renderSize[0] = (int) desc.renderSize().x;
        this.renderSize[1] = (int) desc.renderSize().y;

        fillBuffer();
    }

    @Override
    public ByteBuffer container() {
        return container.duplicate().rewind();
    }

    @Override
    public long size() {
        return 32;
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

    public void fillBuffer() {
        container.clear();
        container.putInt(0, mips);
        container.putInt(4, numWorkGroups);
        container.putInt(8, this.workGroupOffset[0]);
        container.putInt(12, this.workGroupOffset[1]);
        container.putInt(16, this.renderSize[0]);
        container.putInt(20, this.renderSize[1]);
        for (int i = 24; i < 32; i += 4) {
            container.putInt(i, 0);
        }
        container.position(32);
        container.flip();
    }
}