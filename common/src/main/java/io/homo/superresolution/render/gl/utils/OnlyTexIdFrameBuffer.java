package io.homo.superresolution.render.gl.utils;

public class OnlyTexIdFrameBuffer extends FrameBuffer {
    private final int colorTexId;
    public OnlyTexIdFrameBuffer(int id) {
        super(false);
        colorTexId = id;
    }
    @Override
    public int getColorTextureId() {
        return this.colorTexId;
    }

}
