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
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;

import java.util.Map;

public class AlgorithmRegistry {
    private static final Map<String, AlgorithmDescription<?>> algorithmMap = new Object2ObjectLinkedOpenHashMap<>();

    static {
        AlgorithmDescriptions.registryAlgorithms();
        try {
            Class<?> bootstrap = Class.forName(
                    "io.homo.superresolution.common.upscale.algo.legacy.HackAlgorithmBootstrap"
            );
            bootstrap.getMethod("register").invoke(null);
        } catch (ClassNotFoundException ignored) {
            // Optional source set is not present in API-only consumers.
        } catch (Throwable throwable) {
            throw new ExceptionInInitializerError(throwable);
        }
        io.homo.superresolution.api.SuperResolutionAPI.EVENT_BUS.post(
                new io.homo.superresolution.api.event.AlgorithmRegisterEvent()
        );
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

    public static boolean isAlgorithmSupported(AlgorithmDescription<?> algorithmDescription) {
        AlgorithmDescription<?> description = getDescriptionByID(algorithmDescription.getCodeName());
        return description != null && description.checkRequirements().support();
    }
}
