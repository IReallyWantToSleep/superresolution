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

package io.homo.superresolution.forge;

import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.SuperResolutionKeyMapping;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.dataset.DataSetGenerator;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class SuperResolutionEventSubscriberForge {
    @SubscribeEvent
    public static void registerBindings(RegisterKeyMappingsEvent event) {
        event.register(SuperResolutionKeyMapping.OPENGUI_KEYMAPPING);
        if (SuperResolutionConfig.isEnableDatasetGenerator()) {
            event.register(DataSetGenerator.SAVE_KEYMAPPING);
            event.register(DataSetGenerator.SEQUENCE_KEYMAPPING);
        }
    }
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        //oculus load shaderpack too early,we need register event at IMixinConfigPlugin.load
        //fuck you oculus
        /*
        */
    }
}
