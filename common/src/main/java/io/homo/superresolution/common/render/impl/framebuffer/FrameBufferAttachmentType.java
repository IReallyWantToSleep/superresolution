package io.homo.superresolution.common.render.impl.framebuffer;

public enum FrameBufferAttachmentType {
    COLOR(),
    DEPTH(),
    DEPTH_STENCIL();

    private int index;

    public int getIndex() {
        return index;
    }

    public FrameBufferAttachmentType index(int index) {
        this.index = index;
        return this;
    }

}
