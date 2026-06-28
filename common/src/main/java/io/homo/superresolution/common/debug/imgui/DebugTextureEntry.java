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

public record DebugTextureEntry(
        String id,
        String label,
        long textureId,
        int width,
        int height,
        String category,
        String notes,
        boolean preferInteractiveViewer
) {
    public boolean isValid() {
        return textureId != 0L && width > 0 && height > 0;
    }

    public float previewHeight(float previewWidth) {
        if (width <= 0 || height <= 0) {
            return previewWidth;
        }
        return previewWidth * ((float) height / (float) width);
    }
}
