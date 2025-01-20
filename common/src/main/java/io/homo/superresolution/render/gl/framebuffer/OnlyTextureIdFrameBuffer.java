package io.homo.superresolution.render.gl.framebuffer;

public class OnlyTextureIdFrameBuffer extends FrameBuffer {
    private final int colorTexId;
    public OnlyTextureIdFrameBuffer(int id) {
        super(false);
        colorTexId = id;
    }
    @Override
    public int getColorTextureId() {
        return this.colorTexId;
    }

    @Override
    public void resize(int width, int height, boolean clearError) {}

    @Override
    public void createBuffers(int width, int height, boolean clearError) {}
}
