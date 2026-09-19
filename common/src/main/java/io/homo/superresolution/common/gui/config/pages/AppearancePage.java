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

package io.homo.superresolution.common.gui.config.pages;

import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.gui.impl.Text;
import io.homo.superresolution.common.gui.options.*;
import io.homo.superresolution.core.gui.*;
import io.homo.superresolution.core.gui.core.ContainerWidget;
import io.homo.superresolution.core.gui.core.frame.Frame;
import io.homo.superresolution.core.gui.core.frame.ScrollableFrame;
import io.homo.superresolution.core.utils.Color;

public final class AppearancePage implements ConfigPage {
    public static final AppearancePage INSTANCE = new AppearancePage();

    private AppearancePage() {
    }

    @Override
    public Frame create(ConfigPageContext context) {
        return build(context);
    }

    private static Frame build(ConfigPageContext context) {
        ScrollableFrame frame = context.createStandardScrollableFrame();
        ContainerWidget container = context.createStandardContainer();
        context.addFrameTitle(container, Text.translatable("superresolution.screen.config.section.appearance"));
        OptionBuilder builder = context.createOptionBuilder(Text.translatable("superresolution.screen.config.category.appearance"));
        builder.enumSelectorOption(
                        Text.translatable("superresolution.screen.config.options.label.theme"),
                        MaterialTheme.class,
                        SuperResolutionConfig.getTheme())
                .setDefaultValue(MaterialTheme.Light)
                .setEnumNameProvider(t -> Text.translatable("superresolution.enum.theme." + t.name().toLowerCase()).getString())
                .setSaveConsumer(value -> {
                    SuperResolutionConfig.setTheme(value);
                    MaterialUI.setScheme(MaterialScheme.from(value, SuperResolutionConfig.getThemeColor(),
                            SuperResolutionConfig.getThemeSchemeVariant(), SuperResolutionConfig.getThemeContrastLevel()));
                    context.setMaterialScheme(MaterialUI.Scheme);
                })
                .build();
        builder.colorSelectOption(
                        Text.translatable("superresolution.screen.config.options.label.theme_color"),
                        SuperResolutionConfig.getThemeColor())
                .setDefaultValue(() -> Color.from("#78DC77"))
                .setValueChangeListener(value -> {
                    MaterialUI.setScheme(MaterialScheme.from(SuperResolutionConfig.getTheme(), value,
                            SuperResolutionConfig.getThemeSchemeVariant(), SuperResolutionConfig.getThemeContrastLevel()));
                    context.setMaterialScheme(MaterialUI.Scheme);
                })
                .setSaveConsumer(value -> {
                    SuperResolutionConfig.setThemeColor(value);
                    MaterialUI.setScheme(MaterialScheme.from(SuperResolutionConfig.getTheme(), value,
                            SuperResolutionConfig.getThemeSchemeVariant(), SuperResolutionConfig.getThemeContrastLevel()));
                    context.setMaterialScheme(MaterialUI.Scheme);
                })
                .build();
        builder.enumSelectorOption(
                        Text.translatable("superresolution.screen.config.options.label.theme_scheme_variant"),
                        SchemeVariant.class,
                        SuperResolutionConfig.getThemeSchemeVariant())
                .setDefaultValue(SchemeVariant.CONTENT)
                .setEnumNameProvider(v -> Text.translatable("superresolution.enum.schemevarinat." + v.name().toLowerCase()).getString())
                .setSaveConsumer(value -> {
                    SuperResolutionConfig.setThemeSchemeVariant(value);
                    MaterialUI.setScheme(MaterialScheme.from(SuperResolutionConfig.getTheme(), SuperResolutionConfig.getThemeColor(),
                            value, SuperResolutionConfig.getThemeContrastLevel()));
                    context.setMaterialScheme(MaterialUI.Scheme);
                })
                .build();
        builder.numberOption(
                        Text.translatable("superresolution.screen.config.options.label.theme_contrast_level"),
                        SuperResolutionConfig.getThemeContrastLevel(),
                        1.0f,
                        -1.0f)
                .setStep(0.2)
                .setValueFormater((value) -> String.format("%.0f", value.doubleValue() * 100) + "%")
                .setDescription(Text.translatable("superresolution.screen.config.options.tooltip.theme_contrast_level"))
                .setDefaultValue(() -> 0.0f)
                .setValueChangeListener(value -> {
                    MaterialUI.setScheme(MaterialScheme.from(SuperResolutionConfig.getTheme(), SuperResolutionConfig.getThemeColor(),
                            SuperResolutionConfig.getThemeSchemeVariant(), value.doubleValue()));
                    context.setMaterialScheme(MaterialUI.Scheme);
                })
                .setSaveConsumer(value -> {
                    SuperResolutionConfig.setThemeContrastLevel(value.floatValue());
                    MaterialUI.setScheme(MaterialScheme.from(SuperResolutionConfig.getTheme(), SuperResolutionConfig.getThemeColor(),
                            SuperResolutionConfig.getThemeSchemeVariant(), value.doubleValue()));
                    context.setMaterialScheme(MaterialUI.Scheme);
                })
                .build();

        context.addOptionGroupToContainer(container, builder);
        context.finalizeFrame(frame, container);
        return frame;
    }

}
