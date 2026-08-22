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
package io.homo.superresolution.common.config.special;

import io.homo.superresolution.api.SuperResolutionAPI;
import io.homo.superresolution.api.config.ModConfigSpecBuilder;
import io.homo.superresolution.api.config.values.single.EnumValue;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.ConfigSpecType;
import io.homo.superresolution.common.config.enums.DLSSRRRenderPreset;
import io.homo.superresolution.common.upscale.AlgorithmDescriptions;
import net.minecraft.network.chat.Component;

import java.util.Map;

public class DLSSRRSpecialConfig extends SpecialConfig {
    public EnumValue<DLSSRRRenderPreset> RENDER_PRESET = specBuilder.defineEnum(
            "special/dlssrr/render_preset",
            DLSSRRRenderPreset.class,
            () -> DLSSRRRenderPreset.D
    );

    public DLSSRRSpecialConfig(ModConfigSpecBuilder specBuilder) {
        super(specBuilder);
    }

    @Override
    protected void buildDescriptions(Map<String, SpecialConfigDescription<?>> map) {
        map.put(
                "render_preset",
                new SpecialConfigDescription<DLSSRRRenderPreset>()
                        .setKey("render_preset")
                        .setName(Component.translatable("superresolution.screen.config.special.dlssrr.renderpreset.name"))
                        .setTooltip(Component.translatable("superresolution.screen.config.special.dlssrr.renderpreset.tooltip"))
                        .setType(ConfigSpecType.ENUM)
                        .setClazz(DLSSRRRenderPreset.class)
                        .setDefaultValue(DLSSRRRenderPreset.D)
                        .setSaveConsumer((v) -> {
                            if (getSpecialConfigs().DLSSRR.RENDER_PRESET.get() != v) {
                                getSpecialConfigs().DLSSRR.RENDER_PRESET.set(v);
                                if (SuperResolutionAPI.getCurrentAlgorithmDescription() == AlgorithmDescriptions.DLSSRR) {
                                    SuperResolution.recreateAlgorithm();
                                }
                            }
                        })
                        .setValue(RENDER_PRESET.get())
        );
    }
}
