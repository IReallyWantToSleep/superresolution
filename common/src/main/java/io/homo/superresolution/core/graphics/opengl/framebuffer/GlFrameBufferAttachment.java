/*
 * Super Resolution
 * Copyright (c) 2025. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.homo.superresolution.core.graphics.opengl.framebuffer;

import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.opengl.GlConst;

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
