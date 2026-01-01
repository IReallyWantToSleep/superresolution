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

package io.homo.superresolution.api.registry;

import io.homo.superresolution.common.upscale.AlgorithmDescriptions;

import java.util.HashMap;
import java.util.Map;

public class AlgorithmRegistry {
    private static final Map<String, AlgorithmDescription<?>> algorithmMap = new HashMap<>();

    static {
        AlgorithmDescriptions.registryAlgorithms();
    }

    public static void registry(AlgorithmDescription<?> description) {
        algorithmMap.put(description.getCodeName(), description);
    }


    public static Map<String, AlgorithmDescription<?>> getAlgorithmMap() {
        return algorithmMap;
    }

    public static AlgorithmDescription<?> getDescriptionByID(String id) {
        return algorithmMap.get(id);
    }
}
