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

package io.homo.superresolution.api;

import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Objects;

public final class InputResourceSet {
    private final EnumMap<InputResourceType, ITexture> resources = new EnumMap<>(InputResourceType.class);

    private InputResourceSet() {
    }

    public static InputResourceSet create() {
        return new InputResourceSet();
    }

    public InputResourceSet with(InputResourceType type, @Nullable ITexture resource) {
        if (resource == null) {
            resources.remove(Objects.requireNonNull(type, "type"));
        } else {
            resources.put(Objects.requireNonNull(type, "type"), resource);
        }
        return this;
    }

    @Nullable
    public ITexture get(InputResourceType type) {
        return resources.get(Objects.requireNonNull(type, "type"));
    }

    public boolean has(InputResourceType type) {
        return resources.containsKey(Objects.requireNonNull(type, "type"));
    }
}
