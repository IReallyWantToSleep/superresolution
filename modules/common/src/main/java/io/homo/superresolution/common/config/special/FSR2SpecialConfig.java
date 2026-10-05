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

package io.homo.superresolution.common.config.special;

import io.homo.superresolution.api.SuperResolutionAPI;
import io.homo.superresolution.api.config.ModConfigSpecBuilder;
import io.homo.superresolution.api.config.values.single.BooleanValue;
import io.homo.superresolution.api.config.values.single.EnumValue;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.ConfigSpecType;
import io.homo.superresolution.common.upscale.AlgorithmDescriptions;
import io.homo.superresolution.thirdparty.fsr2.Fsr2Version;
import net.minecraft.network.chat.Component;

import java.util.Map;

public class FSR2SpecialConfig extends SpecialConfig {
    public BooleanValue FP16 = this.specBuilder.defineBoolean(
            "special/fsr2/fp16",
            () -> true,
            ""
    );

    public FSR2SpecialConfig(ModConfigSpecBuilder specBuilder) {
        super(specBuilder);
    }

    @Override
    protected void buildDescriptions(Map<String, SpecialConfigDescription<?>> map) {
        map.put(
                "fp16",
                new SpecialConfigDescription<Boolean>()
                        .setValue(getSpecialConfigs().FSR2.FP16.get())
                        .setKey("fp16")
                        .setName(Component.translatable("superresolution.screen.config.special.fsr2.fp16.name"))
                        .setTooltip(Component.translatable("superresolution.screen.config.special.fsr2.fp16.tooltip"))
                        .setType(ConfigSpecType.BOOLEAN)
                        .setSaveConsumer((v) -> {
                            if (getSpecialConfigs().FSR2.FP16.get() != v) {
                                getSpecialConfigs().FSR2.FP16.set(v);
                                if (SuperResolutionAPI.getCurrentAlgorithmDescription() == AlgorithmDescriptions.FSR2) {
                                    SuperResolution.recreateAlgorithm();
                                }
                            }
                        })
                        .setDefaultValue(true)
        );
    }
}
