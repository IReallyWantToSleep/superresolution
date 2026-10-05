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

package io.homo.superresolution.core.graphics.impl.command;

import io.homo.superresolution.core.graphics.impl.buffer.IBuffer;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;

import java.util.IdentityHashMap;

public class ResourceStateTracker {
    private final IdentityHashMap<Object, ResourceState> states = new IdentityHashMap<>();

    public ResourceState getState(ITexture texture) {
        return states.getOrDefault(texture, ResourceState.UNDEFINED);
    }

    public ResourceState getState(IBuffer buffer) {
        return states.getOrDefault(buffer, ResourceState.UNDEFINED);
    }

    public void setState(ITexture texture, ResourceState state) {
        states.put(texture, state);
    }

    public boolean hasState(ITexture texture) {
        return states.containsKey(texture);
    }

    public boolean replaceState(ITexture texture, ResourceState state) {
        if (!states.containsKey(texture)) {
            return false;
        }
        states.put(texture, state);
        return true;
    }

    public void setState(IBuffer buffer, ResourceState state) {
        states.put(buffer, state);
    }

    public void remove(Object resource) {
        states.remove(resource);
    }

    public void clear() {
        states.clear();
    }

    public ResourceStateTracker snapshot() {
        ResourceStateTracker snapshot = new ResourceStateTracker();
        snapshot.states.putAll(states);
        return snapshot;
    }
}
