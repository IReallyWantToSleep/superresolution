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

package io.homo.superresolution.srapi;

import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.vulkan.VulkanTexture;

import java.util.EnumSet;
import java.util.Objects;

public class SRTextureResource {
    public SRTextureResourceDescription description;
    public ITexture texture;
    public long handle;
    public long imageView = -1;
    public int state = SRResourceStates.COMPUTE_READ.value;


    public SRTextureResource(ITexture texture) {
        this.texture = Objects.requireNonNull(texture, "texture");
        this.description = new SRTextureResourceDescription(texture);
        this.handle = texture.handle();
        if (texture instanceof VulkanTexture) {
            this.imageView = ((VulkanTexture) texture).getImageView();
        }
    }

    /**
     * Creates an SRAPI resource around an opaque native graphics resource.
     * Prefer {@link #SRTextureResource(ITexture)} for project-owned textures.
     */
    @Deprecated
    public SRTextureResource(long handle, SRTextureResourceDescription description) {
        this(handle, description, EnumSet.of(SRResourceStates.COMPUTE_READ));
    }

    @Deprecated
    public SRTextureResource(
            long handle,
            SRTextureResourceDescription description,
            EnumSet<SRResourceStates> states) {
        this.texture = null;
        this.description = Objects.requireNonNull(description, "description");
        this.handle = handle;
        this.imageView = 0;
        this.state = SRResourceStates.toBitmask(
                Objects.requireNonNull(states, "states"));
    }

    public long getHandle() {
        if (texture != null) {
            this.handle = texture.handle();
        }
        return handle;
    }

    public EnumSet<SRResourceStates> getStates() {
        return SRResourceStates.fromBitmask(state);
    }

    public void setStates(EnumSet<SRResourceStates> states) {
        this.state = SRResourceStates.toBitmask(states);
    }
}
