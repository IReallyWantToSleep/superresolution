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

package io.homo.superresolution.core.graphics.opengl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GlStates {
    private static final Map<Object, GlState> states = new ConcurrentHashMap<>();

    public static GlState save(Object id) {
        return states.put(id, new GlState());
    }

    public static GlState pop(Object id) {
        return states.remove(id);
    }

    public static GlState get(Object id) {
        return states.get(id);
    }

    public static void remove(Object id) {
        states.remove(id);
    }

    public static void clear() {
        states.clear();
    }
}