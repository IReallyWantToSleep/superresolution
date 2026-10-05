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

/*
 * Super Resolution
 */
package io.homo.superresolution.common.config.special;

import io.homo.superresolution.api.SuperResolutionAPI;
import io.homo.superresolution.api.config.ModConfigSpecBuilder;
import io.homo.superresolution.api.config.values.single.EnumValue;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.ConfigSpecType;
import io.homo.superresolution.common.config.enums.DLSSRenderPreset;
import io.homo.superresolution.common.upscale.AlgorithmDescriptions;
import net.minecraft.network.chat.Component;

import java.util.Map;

public class DLSSSpecialConfig extends SpecialConfig {
    public EnumValue<DLSSRenderPreset> RENDER_PRESET = specBuilder.defineEnum(
            "special/dlss/render_preset",
            DLSSRenderPreset.class,
            () -> DLSSRenderPreset.K
    );

    public DLSSSpecialConfig(ModConfigSpecBuilder specBuilder) {
        super(specBuilder);
    }

    @Override
    protected void buildDescriptions(Map<String, SpecialConfigDescription<?>> map) {
        map.put(
                "render_preset",
                new SpecialConfigDescription<DLSSRenderPreset>()
                        .setKey("render_preset")
                        .setName(Component.translatable("superresolution.screen.config.special.dlss.renderpreset.name"))
                        .setTooltip(Component.translatable("superresolution.screen.config.special.dlss.renderpreset.tooltip"))
                        .setType(ConfigSpecType.ENUM)
                        .setClazz(DLSSRenderPreset.class)
                        .setDefaultValue(DLSSRenderPreset.K)
                        .setSaveConsumer((v) -> {
                            if (getSpecialConfigs().DLSS.RENDER_PRESET.get() != v) {
                                getSpecialConfigs().DLSS.RENDER_PRESET.set(v);
                                if (SuperResolutionAPI.getCurrentAlgorithmDescription() == AlgorithmDescriptions.DLSS) {
                                    SuperResolution.recreateAlgorithm();
                                }
                            }
                        })
                        .setValue(RENDER_PRESET.get())
        );
    }
}
