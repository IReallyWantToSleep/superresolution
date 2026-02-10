/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
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

package io.homo.superresolution.common;

import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.dataset.DataSetGenerator;
import net.minecraft.client.KeyMapping;

public class SuperResolutionKeyMapping {
    #if MC_VER > MC_1_21_8
    //??
    //key.category + . + namespace + path
    #if MC_VER > MC_1_21_10
    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            net.minecraft.resources.Identifier.fromNamespaceAndPath("super_resolution", "keys")
    );
    #else
    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("super_resolution", "keys")
    );
    #endif

    public static final KeyMapping OPENGUI_KEYMAPPING = new KeyMapping(
            "key.super_resolution.open_config",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_F6,
            CATEGORY
    );
    #else
    public static final KeyMapping OPENGUI_KEYMAPPING = new KeyMapping(
            "key.super_resolution.open_config",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_F6,
            "Super Resolution"
    );
    #endif
    private static boolean registeredKeyMapping = false;

    public static void registerKeyMapping() {
        if (!registeredKeyMapping) {
            if (SuperResolutionConfig.isEnableDatasetGenerator()) {
                DataSetGenerator.init();
            }

            KeyMappingRegistry.register(OPENGUI_KEYMAPPING);
        }
        registeredKeyMapping = true;
    }
}
