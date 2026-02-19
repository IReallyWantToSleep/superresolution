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
import io.homo.superresolution.common.upscale.ffxfsr.FSRVersion;
import io.homo.superresolution.thirdparty.fsr2.common.Fsr2Version;
import net.minecraft.network.chat.Component;

import java.util.Map;
import java.util.Optional;

public class FSRSpecialConfig extends SpecialConfig {
    public EnumValue<FSRVersion> VERSION = specBuilder.defineEnum(
            "special/fsr/version",
            FSRVersion.class,
            () -> FSRVersion.V2
    );

    public FSRSpecialConfig(ModConfigSpecBuilder specBuilder) {
        super(specBuilder);
    }

    @Override
    protected void buildDescriptions(Map<String, SpecialConfigDescription<?>> map) {
        map.put(
                "version",
                new SpecialConfigDescription<>()
                        .setValue(getSpecialConfigs().FSR.VERSION.get())
                        .setDefaultValue(FSRVersion.V2)
                        .setValueNameSupplier((variant) -> switch ((FSRVersion) variant) {
                            case V2 -> Optional.of(Component.literal("2.3.3"));
                            case V3 -> Optional.of(Component.literal("3.1.4"));
                        })
                        .setName(Component.translatable("superresolution.screen.config.special.fsr.version.name"))
                        .setTooltip(Component.translatable("superresolution.screen.config.special.fsr.version.tooltip"))
                        .setKey("version")
                        .setSaveConsumer((v) -> {
                            if (getSpecialConfigs().FSR.VERSION.get() != v) {
                                getSpecialConfigs().FSR.VERSION.set(v);
                                if (SuperResolutionAPI.getCurrentAlgorithmDescription() == AlgorithmDescriptions.FSR) {
                                    SuperResolution.recreateAlgorithm();
                                }
                            }
                        })
                        .setType(ConfigSpecType.ENUM)
                        .setClazz(FSRVersion.class)
        );
    }
}
