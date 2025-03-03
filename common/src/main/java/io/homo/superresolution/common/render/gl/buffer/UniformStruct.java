package io.homo.superresolution.common.render.gl.buffer;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.Struct;

import java.nio.ByteBuffer;

public abstract class UniformStruct extends Struct {

    protected UniformStruct(long address, @Nullable ByteBuffer container) {
        super(address, container);
    }

    public ByteBuffer container() {
        return this.container;
    }
}
