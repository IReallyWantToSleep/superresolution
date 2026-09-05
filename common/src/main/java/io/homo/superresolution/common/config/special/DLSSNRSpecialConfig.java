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

import io.homo.superresolution.api.config.ModConfigSpecBuilder;
import io.homo.superresolution.api.config.values.single.BooleanValue;
import io.homo.superresolution.api.config.values.single.FloatValue;
import io.homo.superresolution.common.config.ConfigSpecType;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.network.chat.Component;

import java.util.Map;
import java.util.Optional;

public class DLSSNRSpecialConfig extends SpecialConfig {
    public BooleanValue ENABLE = specBuilder.defineBoolean(
            "special/dlssnr/enable",
            () -> false
    );
    public FloatValue INTENSITY = specBuilder.defineFloat(
            "special/dlssnr/intensity",
            () -> 1.0f,
            v -> v >= 0.0f && v <= 2.0f
    );
    public FloatValue LOCAL_TONE_STRENGTH = specBuilder.defineFloat(
            "special/dlssnr/local_tone_strength",
            () -> 0.5f,
            v -> v >= 0.0f && v <= 2.0f
    );
    public FloatValue LOCAL_STRUCTURE_STRENGTH = specBuilder.defineFloat(
            "special/dlssnr/local_structure_strength",
            () -> 0.5f,
            v -> v >= 0.0f && v <= 2.0f
    );
    public FloatValue PASS_COUNT = specBuilder.defineFloat(
            "special/dlssnr/pass_count",
            () -> 1.0f,
            v -> v >= 1.0f && v <= 4.0f
    );
    public FloatValue COLOR_STRENGTH = specBuilder.defineFloat(
            "special/dlssnr/color_strength",
            () -> 1.0f,
            v -> v >= 0.0f && v <= 2.0f
    );
    public FloatValue SKIN_STRUCTURE_STRENGTH = specBuilder.defineFloat(
            "special/dlssnr/skin_structure_strength",
            () -> 0.5f
    );
    public FloatValue STYLE = specBuilder.defineFloat(
            "special/dlssnr/style",
            () -> 0.0f
    );
    public BooleanValue USE_AUTO_MASK = specBuilder.defineBoolean(
            "special/dlssnr/use_auto_mask",
            () -> false
    );
    public BooleanValue UI_CORRECTION = specBuilder.defineBoolean(
            "special/dlssnr/ui_correction",
            () -> false
    );
    public BooleanValue DEPTH_INVERTED = specBuilder.defineBoolean(
            "special/dlssnr/depth_inverted",
            () -> false
    );

    public DLSSNRSpecialConfig(ModConfigSpecBuilder specBuilder) {
        super(specBuilder);
    }

    @Override
    protected void buildDescriptions(Map<String, SpecialConfigDescription<?>> map) {
        map.put(
                "enable",
                new SpecialConfigDescription<Boolean>()
                        .setKey("enable")
                        .setName(Component.translatable("superresolution.screen.config.special.dlssnr.enable.name"))
                        .setTooltip(Component.translatable("superresolution.screen.config.special.dlssnr.enable.tooltip"))
                        .setType(ConfigSpecType.BOOLEAN)
                        .setDefaultValue(false)
                        .setSaveConsumer(v -> ENABLE.set(v))
                        .setValueSupplier(ENABLE::get)
        );
        map.put(
                "intensity",
                floatSlider("intensity", INTENSITY, Pair.of(0.0f, 2.0f), 1.0f, null)
        );
        map.put(
                "local_tone_strength",
                floatSlider("local_tone_strength", LOCAL_TONE_STRENGTH, Pair.of(0.0f, 2.0f), 0.5f, null)
        );
        map.put(
                "local_structure_strength",
                floatSlider("local_structure_strength", LOCAL_STRUCTURE_STRENGTH, Pair.of(0.0f, 2.0f), 0.5f, null)
        );
        map.put(
                "pass_count",
                floatSlider("pass_count", PASS_COUNT, Pair.of(1.0f, 4.0f), 1.0f, 1.0f)
        );
        map.put(
                "color_strength",
                floatSlider("color_strength", COLOR_STRENGTH, Pair.of(0.0f, 2.0f), 1.0f, null)
        );
        map.put(
                "skin_structure_strength",
                floatSlider("skin_structure_strength", SKIN_STRUCTURE_STRENGTH, Pair.of(0.0f, 1.0f), 0.5f, null)
        );
        map.put(
                "style",
                floatSlider("style", STYLE, Pair.of(0.0f, 5.0f), 0.0f, 1.0f)
        );
        map.put(
                "use_auto_mask",
                boolOption("use_auto_mask", USE_AUTO_MASK)
        );
        map.put(
                "ui_correction",
                boolOption("ui_correction", UI_CORRECTION)
        );
        map.put(
                "depth_inverted",
                boolOption("depth_inverted", DEPTH_INVERTED)
        );
    }

    private SpecialConfigDescription<Float> floatSlider(
            String key,
            FloatValue value,
            Pair<Float, Float> range,
            float defaultValue,
            Float step
    ) {
        SpecialConfigDescription<Float> desc = new SpecialConfigDescription<Float>()
                .setKey(key)
                .setName(Component.translatable("superresolution.screen.config.special.dlssnr." + key + ".name"))
                .setTooltip(Component.translatable("superresolution.screen.config.special.dlssnr." + key + ".tooltip"))
                .setType(ConfigSpecType.FLOAT)
                .setValueRange(range)
                .setDefaultValue(defaultValue)
                .setSaveConsumer(v -> value.set(step != null ? (float) Math.round(v) : v))
                .setValueSupplier(value::get);
        if (step != null) {
            desc.setStep(step);
            desc.setValueNameSupplier(v -> Optional.of(Component.literal(String.valueOf(Math.round(v)))));
        }
        return desc;
    }

    private SpecialConfigDescription<Boolean> boolOption(String key, BooleanValue value) {
        return new SpecialConfigDescription<Boolean>()
                .setKey(key)
                .setName(Component.translatable("superresolution.screen.config.special.dlssnr." + key + ".name"))
                .setTooltip(Component.translatable("superresolution.screen.config.special.dlssnr." + key + ".tooltip"))
                .setType(ConfigSpecType.BOOLEAN)
                .setDefaultValue(false)
                .setSaveConsumer(value::set)
                .setValueSupplier(value::get);
    }
}
