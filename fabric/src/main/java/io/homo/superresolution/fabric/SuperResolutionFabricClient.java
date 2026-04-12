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

package io.homo.superresolution.fabric;

import io.homo.superresolution.api.platform.Platform;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.SuperResolutionKeyMapping;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.dataset.DataSetGenerator;
import io.homo.superresolution.core.graphics.renderdoc.RenderDoc;
import io.homo.superresolution.fabric.compat.sodium.SodiumOptionScreen;
import net.fabricmc.api.ClientModInitializer;
#if MC_VER > MC_1_21_11
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
#else
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
#endif
import net.fabricmc.loader.api.FabricLoader;

public final class SuperResolutionFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        if (FabricLoader.getInstance().isDevelopmentEnvironment() && SuperResolutionConfig.isEnableRenderDoc())
            RenderDoc.init();
        if (Platform.currentPlatform.isModLoaded("sodiumoptionsapi")) {
            SodiumOptionScreen.register();
        }
        SuperResolution.onClientSetup();
        #if MC_VER > MC_1_21_11
        KeyMappingHelper.registerKeyMapping(SuperResolutionKeyMapping.OPENGUI_KEYMAPPING);
        if (SuperResolutionConfig.isEnableDatasetGenerator()) {
            KeyMappingHelper.registerKeyMapping(DataSetGenerator.SAVE_KEYMAPPING);
            KeyMappingHelper.registerKeyMapping(DataSetGenerator.SEQUENCE_KEYMAPPING);
        }
        #else
        KeyBindingHelper.registerKeyBinding(SuperResolutionKeyMapping.OPENGUI_KEYMAPPING);
        if (SuperResolutionConfig.isEnableDatasetGenerator()) {
            KeyBindingHelper.registerKeyBinding(DataSetGenerator.SAVE_KEYMAPPING);
            KeyBindingHelper.registerKeyBinding(DataSetGenerator.SEQUENCE_KEYMAPPING);
        }
        #endif

        SuperResolutionKeyMapping.registerKeyMapping();
    }
}
