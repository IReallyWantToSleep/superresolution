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
import io.homo.superresolution.common.config.special.SpecialConfig;
import io.homo.superresolution.common.config.special.SpecialConfigDescription;
import io.homo.superresolution.common.gui.impl.Text;
import io.homo.superresolution.core.gui.core.ContainerWidget;
import io.homo.superresolution.core.gui.core.frame.Frame;
import io.homo.superresolution.core.gui.core.frame.ScrollableFrame;
import io.homo.superresolution.core.impl.Pair;

import java.util.*;

public final class AlgorithmPage implements ConfigPage {
    public static final AlgorithmPage INSTANCE = new AlgorithmPage();

    private AlgorithmPage() {
    }

    @Override
    public Frame create(ConfigPageContext context) {
        return build(context);
    }

    private static Frame build(ConfigPageContext context) {
        ScrollableFrame frame = context.createStandardScrollableFrame();
        ContainerWidget container = context.createStandardContainer();
        context.addFrameTitle(container, Text.translatable("superresolution.screen.config.section.algorithm"));

        for (String key : SuperResolutionConfig.SPECIAL.description.keySet()) {
            Pair<SpecialConfig, String> specialConfigPair = SuperResolutionConfig.SPECIAL.description.get(key);
            SpecialConfig specialConfig = specialConfigPair.left();
            String displayName = specialConfigPair.right();
            Map<String, SpecialConfigDescription<?>> configDescriptions = specialConfig.getDescriptions();

            if (configDescriptions.isEmpty()) {
                continue;
            }

            context.addLabeledOptionGroup(container, Text.literal(displayName), builder -> {
                for (String configKey : configDescriptions.keySet()) {
                    SpecialConfigDescription<?> desc = configDescriptions.get(configKey);
                    context.buildSpecialConfigOption(builder, desc);
                }
            });
        }

        context.finalizeFrame(frame, container);
        return frame;
    }

}
