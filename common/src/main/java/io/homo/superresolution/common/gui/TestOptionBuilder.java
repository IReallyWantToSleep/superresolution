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

package io.homo.superresolution.common.gui;

import io.homo.superresolution.common.gui.impl.Text;
import io.homo.superresolution.common.gui.options.OptionBuilder;
import io.homo.superresolution.common.gui.options.OptionCategory;
import io.homo.superresolution.core.graphics.impl.texture.TextureFormat;
import io.homo.superresolution.core.gui.MaterialScheme;
import io.homo.superresolution.core.gui.MaterialTheme;
import io.homo.superresolution.core.utils.Color;
import net.minecraft.client.gui.screens.Screen;

public class TestOptionBuilder {
    public static OptionBuilder.OptionsContainer buildOptionsContainer() {
        MaterialScheme scheme = MaterialScheme.from(MaterialTheme.Dark, Color.from("#6750A4"));
        OptionCategory category = new OptionCategory(Text.literal("Super Resolution 设置"));
        OptionBuilder builder = new OptionBuilder(category).scheme(scheme);

        builder.booleanOption(
                        Text.literal("Enable Super Resolution"),
                        true)
                .setDescription("When enabled, the selected algorithm will be used for super resolution upscaling")
                .build();

        builder.enumSelectorOption(
                        Text.literal("Super Resolution Algorithm"),
                        TextureFormat.class,
                        TextureFormat.R8)
                .setDescription("Choose an algorithm")
                .build();

        builder.numberOption(
                        Text.literal("Super Resolution Ratio"),
                        1.70,
                        2.0,
                        1.0)
                .setStep(0.05)
                .setValueFormater(v -> String.format("%.2f", v.doubleValue()))
                .setDescriptions(
                        Text.literal("Higher values result in lower in-game resolution, blurrier visuals, reduced performance cost"),
                        Text.literal("Lower values result in higher in-game resolution, clearer visuals, better image quality but increased performance cost"),
                        Text.literal("Current rendering resolution: 1920x1080"),
                        Text.literal("Current rendering precision: 83%"),
                        Text.literal("Recommended range: 1.4~1.8")
                )
                .build();
        builder.numberOption(
                        Text.literal("Sharpness"),
                        0.5,
                        1.0,
                        0.0)
                .setStep(0.1)
                .setValueFormater(v -> String.format("%.0f%%", v.doubleValue() * 100))
                .setDescription("Adjust the sharpness of the upscaled image")
                .build();

        return builder.build();
    }

    public static Screen build(Screen parent) {
        return null;
    }
}
