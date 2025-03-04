package io.homo.superresolution.common.render.gl.buffer;

import java.nio.ByteBuffer;

public interface IUniformStruct {
    ByteBuffer container();

    int sizeof();
}
