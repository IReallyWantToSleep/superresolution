package io.homo.superresolution.core.gl.framebuffer;

import io.homo.superresolution.core.gl.GlConst;
import io.homo.superresolution.core.impl.texture.ITexture;

public class GlFrameBufferAttachment {
    public FrameBufferAttachmentType type;
    public ITexture texture;

    public GlFrameBufferAttachment(FrameBufferAttachmentType type, ITexture texture) {
        this.type = type;
        this.texture = texture;
    }

    public enum FrameBufferAttachmentType {
        COLOR(GlConst.GL_COLOR_ATTACHMENT0),
        DEPTH(GlConst.GL_DEPTH_ATTACHMENT),
        DEPTH_STENCIL(GlConst.GL_DEPTH_STENCIL_ATTACHMENT);
        private final int srcAttachmentId;
        private int attachmentId;

        FrameBufferAttachmentType(int attachmentId) {
            this.attachmentId = attachmentId;
            this.srcAttachmentId = attachmentId;
        }

        public FrameBufferAttachmentType index(int index) {
            if (this.srcAttachmentId != GlConst.GL_COLOR_ATTACHMENT0) {
                throw new RuntimeException();
            }
            this.attachmentId = srcAttachmentId + index;
            return this;
        }

        public int attachmentId() {
            return attachmentId;
        }
    }
}
