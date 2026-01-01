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

package io.homo.superresolution.core.gui.core.backends.interfaces;

import java.util.ArrayList;
import java.util.List;


public class CommandsBatch {
    private final List<Object> sequence = new ArrayList<>();
    private int zIndex;
    private CommandsBatch parent;

    public void zIndex(int z) {
        this.zIndex = z;
    }

    public int zIndex() {
        return zIndex;
    }

    public void addCommand(DrawCommand cmd) {
        sequence.add(cmd);
    }

    public void addChildBatch(CommandsBatch child) {
        child.parent = this;
        sequence.add(child);
    }

    public List<Object> getSequence() {
        return sequence;
    }

    public void execute() {
        for (Object o : sequence) {
            if (o instanceof DrawCommand cmd) {
                cmd.execute();
            } else if (o instanceof CommandsBatch batch) {
                batch.execute();
            }
        }
    }

    public boolean isEmpty() {
        return sequence.isEmpty();
    }
}
