/*
 * Super Resolution
 * Copyright (c) 2025-2026. 187J3X1-114514
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

package io.homo.superresolution.common.debug.imgui;

import imgui.ImGui;
import io.homo.superresolution.core.graphics.impl.framebuffer.FrameBufferAttachmentType;
import io.homo.superresolution.core.graphics.impl.framebuffer.IBindableFrameBuffer;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;

import java.util.Objects;
import java.util.function.Consumer;

public class ImGuiDebugContext {
    private final String category;
    private final Consumer<DebugTextureEntry> textureSink;
    private final Consumer<DebugTextureEntry> viewerOpener;

    public ImGuiDebugContext(
            String category,
            Consumer<DebugTextureEntry> textureSink,
            Consumer<DebugTextureEntry> viewerOpener
    ) {
        this.category = Objects.requireNonNull(category, "category");
        this.textureSink = Objects.requireNonNull(textureSink, "textureSink");
        this.viewerOpener = Objects.requireNonNull(viewerOpener, "viewerOpener");
    }

    public String category() {
        return category;
    }

    public void property(String label, Object value) {
        ImGui.text(label + ": " + String.valueOf(value));
    }

    public void text(String value) {
        ImGui.text(String.valueOf(value));
    }

    public void openViewer(DebugTextureEntry entry) {
        if (entry != null) {
            viewerOpener.accept(entry);
        }
    }

    public void addTexture(String id, String label, ITexture texture) {
        addTexture(id, label, texture, null, true);
    }

    public void addTexture(String id, String label, ITexture texture, String notes, boolean preferInteractiveViewer) {
        if (texture == null) {
            return;
        }
        addTexture(
                id,
                label,
                texture.handle(),
                texture.getWidth(),
                texture.getHeight(),
                notes,
                preferInteractiveViewer
        );
    }

    public void addTexture(
            String id,
            String label,
            long textureId,
            int width,
            int height,
            String notes,
            boolean preferInteractiveViewer
    ) {
        DebugTextureEntry entry = new DebugTextureEntry(
                category + ":" + id,
                label,
                textureId,
                width,
                height,
                category,
                notes,
                preferInteractiveViewer
        );
        if (entry.isValid()) {
            textureSink.accept(entry);
        }
    }

    public void addFramebufferTextures(String idPrefix, String label, IBindableFrameBuffer framebuffer) {
        addFramebufferTextures(idPrefix, label, framebuffer, null, true);
    }

    public void addFramebufferTextures(
            String idPrefix,
            String label,
            IBindableFrameBuffer framebuffer,
            String notes,
            boolean preferInteractiveViewer
    ) {
        if (framebuffer == null) {
            return;
        }
        addTexture(
                idPrefix + ".color",
                label + " Color",
                framebuffer.getTexture(FrameBufferAttachmentType.Color),
                notes,
                preferInteractiveViewer
        );
        addTexture(
                idPrefix + ".depth",
                label + " Depth",
                framebuffer.getTexture(FrameBufferAttachmentType.AnyDepth),
                notes,
                preferInteractiveViewer
        );
    }

    public void addFramebufferTextures(String idPrefix, String label, IFrameBuffer framebuffer) {
        addFramebufferTextures(idPrefix, label, framebuffer, null, true);
    }

    public void addFramebufferTextures(
            String idPrefix,
            String label,
            IFrameBuffer framebuffer,
            String notes,
            boolean preferInteractiveViewer
    ) {
        if (framebuffer == null) {
            return;
        }
        addTexture(
                idPrefix + ".color",
                label + " Color",
                framebuffer.getTexture(FrameBufferAttachmentType.Color),
                notes,
                preferInteractiveViewer
        );
        addTexture(
                idPrefix + ".depth",
                label + " Depth",
                framebuffer.getTexture(FrameBufferAttachmentType.AnyDepth),
                notes,
                preferInteractiveViewer
        );
    }
}
