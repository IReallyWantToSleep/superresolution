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

package io.homo.superresolution.core.graphics.impl.buffer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BufferUsages {
    private final List<BufferUsage> usages;

    private BufferUsages(List<BufferUsage> usages) {
        this.usages = usages;
    }

    private BufferUsages() {
        this.usages = new ArrayList<>();
    }

    protected BufferUsages add(BufferUsage... usage) {
        this.usages.addAll(List.of(usage));
        return this;
    }

    public static BufferUsages create() {
        return new BufferUsages();
    }

    public List<BufferUsage> getUsages() {
        return Collections.unmodifiableList(usages);
    }

    public BufferUsages copy() {
        return new BufferUsages(new ArrayList<>(usages));
    }

    public boolean isEmpty() {
        return usages.isEmpty();
    }

    public BufferUsages staticDraw() {
        usages.add(BufferUsage.StaticDraw);
        return this;
    }

    public BufferUsages dynamicDraw() {
        usages.add(BufferUsage.DynamicDraw);
        return this;
    }

    public BufferUsages ubo() {
        usages.add(BufferUsage.Ubo);
        return this;
    }

    public BufferUsages transferSrc() {
        usages.add(BufferUsage.TransferSrc);
        return this;
    }

    public BufferUsages transferDst() {
        usages.add(BufferUsage.TransferDst);
        return this;
    }

    public boolean has(BufferUsage usage) {
        return usages.contains(usage);
    }

    @Override
    public String toString() {
        return "BufferUsages{" + usages + '}';
    }
}