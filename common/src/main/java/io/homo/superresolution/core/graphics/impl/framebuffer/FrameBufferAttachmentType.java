package io.homo.superresolution.core.graphics.impl.framebuffer;

public enum FrameBufferAttachmentType {
    Color(),
    AnyDepth(),
    Depth(),
    DepthStencil();

    private int index;

    public int getIndex() {
        return index;
    }

    public FrameBufferAttachmentType index(int index) {
        this.index = index;
        return this;
    }
}
