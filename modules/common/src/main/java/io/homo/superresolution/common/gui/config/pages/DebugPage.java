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
import io.homo.superresolution.core.gui.core.ContainerWidget;
import io.homo.superresolution.core.gui.core.frame.Frame;
import io.homo.superresolution.core.gui.core.frame.ScrollableFrame;

public final class DebugPage implements ConfigPage {
    public static final DebugPage INSTANCE = new DebugPage();

    private DebugPage() {
    }

    @Override
    public Frame create(ConfigPageContext context) {
        return build(context);
    }

    private static Frame build(ConfigPageContext context) {
        ScrollableFrame frame = context.createStandardScrollableFrame();
        ContainerWidget container = context.createStandardContainer();
        context.addFrameTitle(container, Text.translatable("superresolution.screen.config.section.debug"));
        OptionBuilder builder = context.createOptionBuilder(Text.translatable("superresolution.screen.config.category.debug"));
        builder.booleanOption(
                        Text.translatable("superresolution.screen.config.options.label.enable_debug"),
                        SuperResolutionConfig.isEnableDebug()
                )
                .setDefaultValue(() -> false)
                .setDescription(Text.translatable("superresolution.screen.config.options.tooltip.enable_debug"))
                .setSaveConsumer(SuperResolutionConfig::setEnableDebug)
                .build();
        builder.booleanOption(
                        Text.translatable("superresolution.screen.config.options.label.debug_dump_shader"),
                        SuperResolutionConfig.isDebugDumpShader())
                .setDefaultValue(() -> false)
                .setDescription(Text.translatable("superresolution.screen.config.options.tooltip.debug_dump_shader"))
                .setSaveConsumer(SuperResolutionConfig::setDebugDumpShader)
                .build();
        builder.booleanOption(
                        Text.translatable("superresolution.screen.config.options.label.enable_imgui"),
                        SuperResolutionConfig.isEnableImgui())
                .setDefaultValue(() -> true)
                .setDescription(Text.translatable("superresolution.screen.config.options.tooltip.enable_imgui"))
                .setSaveConsumer(SuperResolutionConfig::setEnableImgui)
                .build();
        builder.booleanOption(
                        Text.translatable("superresolution.screen.config.options.label.enable_present_indicator"),
                        SuperResolutionConfig.isEnablePresentIndicator())
                .setDefaultValue(() -> false)
                .setDescription(Text.translatable("superresolution.screen.config.options.tooltip.enable_present_indicator"))
                .setSaveConsumer(SuperResolutionConfig::setEnablePresentIndicator)
                .build();
        context.addOptionGroupToContainer(container, builder);
        context.finalizeFrame(frame, container);
        return frame;
    }

}
