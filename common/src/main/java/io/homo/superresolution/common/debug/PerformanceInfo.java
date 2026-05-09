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

package io.homo.superresolution.common.debug;

import java.util.HashMap;
import java.util.Map;

public class PerformanceInfo {
    private static final Map<String, Long> beginTimeMap = new HashMap<>();
    private static final Map<String, Long> usingTimeMap = new HashMap<>();


    public static long begin(String name) {
        beginTimeMap.put(name, System.nanoTime());
        return beginTimeMap.get(name);
    }

    public static long end(String name) {
        usingTimeMap.put(name, System.nanoTime() - beginTimeMap.get(name));
        return usingTimeMap.get(name);
    }

    public static long end(String name, long time) {
        usingTimeMap.put(name, time);
        return usingTimeMap.get(name);
    }

    public static long getAsNano(String name) {
        if (usingTimeMap.get(name) == null) {
            return -1L;
        }
        return usingTimeMap.get(name);
    }

    public static float getAsMillis(String name) {
        if (usingTimeMap.get(name) == null) {
            return -1L;
        }
        return (float) usingTimeMap.get(name) / 1000000L;
    }
}
