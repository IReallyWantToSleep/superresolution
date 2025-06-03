package io.homo.superresolution.fsr2.struct;

import io.homo.superresolution.core.graphics.impl.IUniformStruct;
import io.homo.superresolution.fsr2.Fsr2Context;
import io.homo.superresolution.fsr2.Fsr2Dimensions;
import io.homo.superresolution.fsr2.Fsr2DispatchDescription;
import io.homo.superresolution.fsr2.Fsr2Utils;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Fsr2CBRcas implements IUniformStruct {
    private final ByteBuffer container;

    public Fsr2CBRcas() {
        this.container = MemoryStack.stackCalloc(sizeof());
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
        container.position(sizeof());
        container.flip();
    }

    @Override
    public ByteBuffer container() {
        return container.duplicate().rewind();
    }

    @Override
    public int sizeof() {
        return 16;
    }
}