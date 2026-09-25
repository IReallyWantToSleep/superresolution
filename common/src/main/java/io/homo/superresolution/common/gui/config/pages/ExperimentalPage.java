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

public final class ExperimentalPage implements ConfigPage {
    public static final ExperimentalPage INSTANCE = new ExperimentalPage();

    private ExperimentalPage() {
    }

    @Override
    public Frame create(ConfigPageContext context) {
        return build(context);
    }

    private static Frame build(ConfigPageContext context) {
        ScrollableFrame frame = context.createStandardScrollableFrame();
        ContainerWidget container = context.createStandardContainer();
        context.addFrameTitle(container, Text.translatable("superresolution.screen.config.section.experimental"));

        context.addLabeledOptionGroup(
                container,
                Text.translatable("superresolution.screen.config.group.experimental.algorithms"),
                builder -> builder.booleanOption(
                                Text.translatable("superresolution.screen.config.options.label.enable_experimental_algorithms"),
                                SuperResolutionConfig.isEnableExperimentalAlgorithms())
                        .setDescription(Text.translatable("superresolution.screen.config.options.tooltip.enable_experimental_algorithms"))
                        .setDefaultValue(() -> false)
                        .setSaveConsumer(SuperResolutionConfig::setEnableExperimentalAlgorithms)
                        .build()
        );

        context.addLabeledOptionGroup(
                container,
                Text.translatable("superresolution.screen.config.group.experimental.dlss_ray_reconstruction"),
                builder -> builder.booleanOption(
                                Text.translatable("superresolution.screen.config.options.label.enable_dlss_ray_reconstruction"),
                                SuperResolutionConfig.isEnableDlssRayReconstruction())
                        .setDescription(Text.translatable("superresolution.screen.config.options.tooltip.enable_dlss_ray_reconstruction"))
                        .setDefaultValue(() -> false)
                        .setSaveConsumer(SuperResolutionConfig::setEnableDlssRayReconstruction)
                        .setRequireRestartGame(true)
                        .build()
        );

        context.addLabeledOptionGroup(
                container,
                Text.translatable("superresolution.screen.config.group.experimental.iris_extension"),
                builder -> builder.booleanOption(
                                Text.translatable("superresolution.screen.config.options.label.enable_iris_extension"),
                                SuperResolutionConfig.isEnableIrisExtension())
                        .setDescription(Text.translatable("superresolution.screen.config.options.tooltip.enable_iris_extension"))
                        .setDefaultValue(() -> false)
                        .setRequireRestartGame(true)
                        .setSaveConsumer(SuperResolutionConfig::setEnableIrisExtension)
                        .build()
        );

        context.finalizeFrame(frame, container);
        return frame;
    }

}
