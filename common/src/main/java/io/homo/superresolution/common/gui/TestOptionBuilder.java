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
import io.homo.superresolution.common.gui.options.OptionScreenBuilder;
import io.homo.superresolution.common.gui.screens.MaterialStyleConfigScreen;
import io.homo.superresolution.core.graphics.impl.texture.TextureFormat;
import net.minecraft.client.gui.screens.Screen;

public class TestOptionBuilder {
    public static MaterialStyleConfigScreen build(Screen parent) {
        OptionScreenBuilder builder = new OptionScreenBuilder();
        OptionBuilder testOptionBuilder = builder.getOptionBuilder(Text.literal("Test"));
        testOptionBuilder.booleanOption(
                Text.literal("喵喵喵喵"),
                false
        ).build();
        testOptionBuilder.booleanOption(
                Text.literal("reter喵喵喵喵"),
                true
        ).build();
        testOptionBuilder.numberOption(
                Text.literal("喵喵tert喵喵"),
                0.5,
                1,
                0
        ).build();
        testOptionBuilder.numberOption(
                Text.literal("喵喵tert喵喵"),
                0.5,
                1,
                0
        ).build();
        testOptionBuilder.enumSelectorOption(
                Text.literal("喵喵tert喵喵"),
                TextureFormat.class,
                TextureFormat.R8
        ).build();
        testOptionBuilder.numberOption(
                Text.literal("喵喵tert喵喵"),
                0.5,
                1,
                0
        ).build();
        testOptionBuilder.numberOption(
                Text.literal("喵喵tert喵喵"),
                0.5,
                1,
                0
        ).build();
        testOptionBuilder.numberOption(
                Text.literal("喵喵tert喵喵"),
                0.5,
                1,
                0
        ).build();
        testOptionBuilder.numberOption(
                Text.literal("喵喵tert喵喵"),
                0.5,
                1,
                0
        ).build();
        testOptionBuilder.numberOption(
                Text.literal("喵喵tert喵喵"),
                0.5,
                1,
                0
        ).build();
        testOptionBuilder.numberOption(
                Text.literal("喵喵tert喵喵"),
                0.5,
                1,
                0
        ).build();
        testOptionBuilder.numberOption(
                Text.literal("喵喵tert喵喵"),
                0.5,
                1,
                0
        ).build();
        testOptionBuilder.numberOption(
                Text.literal("喵喵tert喵喵"),
                0.5,
                1,
                0
        ).build();
        testOptionBuilder.numberOption(
                Text.literal("喵喵tert喵喵"),
                0.5,
                1,
                0
        ).build();
        testOptionBuilder.numberOption(
                Text.literal("喵喵tert喵喵"),
                0.5,
                1,
                0
        ).build();

        testOptionBuilder.numberOption(
                Text.literal("喵喵tert喵喵"),
                0.5,
                1,
                0
        ).build();
        testOptionBuilder.numberOption(
                Text.literal("喵喵tert喵喵"),
                0.5,
                1,
                0
        ).build();
        testOptionBuilder.numberOption(
                Text.literal("喵喵tert喵喵"),
                0.5,
                1,
                0
        ).build();
        testOptionBuilder.numberOption(
                Text.literal("喵喵tert喵喵"),
                0.5,
                1,
                0
        ).build();

        testOptionBuilder.numberOption(
                Text.literal("喵喵tert喵喵"),
                0.5,
                1,
                0
        ).build();
        testOptionBuilder.numberOption(
                Text.literal("喵喵tert喵喵"),
                0.5,
                1,
                0
        ).build();
        testOptionBuilder.numberOption(
                Text.literal("喵喵tert喵喵"),
                0.5,
                1,
                0
        ).build();
        testOptionBuilder.numberOption(
                Text.literal("喵喵tert喵喵"),
                0.5,
                1,
                0
        ).build();

        testOptionBuilder.numberOption(
                Text.literal("喵喵tert喵喵"),
                0.5,
                1,
                0
        ).build();
        testOptionBuilder.numberOption(
                Text.literal("喵喵tert喵喵"),
                0.5,
                1,
                0
        ).build();
        testOptionBuilder.numberOption(
                Text.literal("喵喵tert喵喵"),
                0.5,
                1,
                0
        ).build();
        testOptionBuilder.numberOption(
                Text.literal("喵喵tert喵喵"),
                0.5,
                1,
                0
        ).build();

        testOptionBuilder.numberOption(
                Text.literal("喵喵tert喵喵"),
                0.5,
                1,
                0
        ).build();
        testOptionBuilder.numberOption(
                Text.literal("喵喵tert喵喵"),
                0.5,
                1,
                0
        ).build();
        testOptionBuilder.numberOption(
                Text.literal("喵喵tert喵喵"),
                0.5,
                1,
                0
        ).build();
        testOptionBuilder.numberOption(
                Text.literal("喵喵tert喵喵"),
                0.5,
                1,
                0
        ).build();


        return builder.build().setParent(parent);
    }
}
