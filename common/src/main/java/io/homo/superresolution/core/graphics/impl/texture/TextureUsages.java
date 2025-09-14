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

package io.homo.superresolution.core.graphics.impl.texture;

import java.util.ArrayList;
import java.util.List;

public class TextureUsages {
    private List<TextureUsage> usages = new ArrayList<>();

    private TextureUsages(List<TextureUsage> usages) {
        this.usages = usages;
    }

    private TextureUsages() {
    }

    public static TextureUsages create() {
        return new TextureUsages();
    }

    public List<TextureUsage> getUsages() {
        return usages;
    }

    public TextureUsages copy() {
        return new TextureUsages(new ArrayList<>(usages));
    }

    public boolean isEmpty() {
        return usages.isEmpty();
    }

    public TextureUsages sampler() {
        usages.add(TextureUsage.Sampler);
        return this;
    }

    public TextureUsages storage() {
        usages.add(TextureUsage.Storage);
        return this;
    }

    public TextureUsages attachmentColor() {
        usages.add(TextureUsage.AttachmentColor);
        return this;
    }

    public TextureUsages attachmentDepth() {
        usages.add(TextureUsage.AttachmentDepth);
        return this;
    }

    public TextureUsages transferSource() {
        usages.add(TextureUsage.TransferSource);
        return this;
    }

    public TextureUsages transferDestination() {
        usages.add(TextureUsage.TransferDestination);
        return this;
    }
}
