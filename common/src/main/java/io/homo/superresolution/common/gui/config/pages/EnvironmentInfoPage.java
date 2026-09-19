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

import io.homo.superresolution.common.gui.impl.Text;
import io.homo.superresolution.core.graphics.GraphicsCapabilities;
import io.homo.superresolution.core.gui.core.ContainerWidget;
import io.homo.superresolution.core.gui.core.frame.Frame;
import io.homo.superresolution.core.gui.core.frame.ScrollableFrame;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.*;

import io.homo.superresolution.common.gui.config.pages.ConfigPageContext.TitlePill;
import io.homo.superresolution.common.gui.config.pages.ConfigPageContext.InfoCard;

public final class EnvironmentInfoPage implements ConfigPage {
    public static final EnvironmentInfoPage INSTANCE = new EnvironmentInfoPage();

    private EnvironmentInfoPage() {
    }

    @Override
    public Frame create(ConfigPageContext context) {
        return build(context);
    }

    private static Frame build(ConfigPageContext context) {
        ScrollableFrame frame = context.createStandardScrollableFrame();
        ContainerWidget container = context.createStandardContainer();
        context.addFrameTitle(container, Text.translatable("superresolution.screen.config.section.environment"));

        TitlePill label = context.createSectionPill(
                Text.translatable("superresolution.screen.config.info.environment.base").getString()
        );
        label.layout().setMargin(YogaEdge.TOP, 8);
        label.layout().setMargin(YogaEdge.BOTTOM, 6);
        container.addChild(label);

        InfoCard envCard = new InfoCard();
        envCard.addChild(context.createInfoLine(Text.translatable("superresolution.screen.config.info.environment.mod_version").getString(), context.safeGetModVersion()));
        envCard.addChild(context.createInfoLine(Text.translatable("superresolution.screen.config.info.environment.native_version").getString(), context.safeGetNativeVersion()));
        envCard.addChild(context.createInfoLine(Text.translatable("superresolution.screen.config.info.environment.system").getString(), context.safeGetOperatingSystem()));
        container.addChild(envCard);
        TitlePill labelOGL = context.createSectionPill(
                Text.translatable("superresolution.screen.config.info.environment.opengl").getString()
        );
        labelOGL.layout().setMargin(YogaEdge.TOP, 8);
        labelOGL.layout().setMargin(YogaEdge.BOTTOM, 6);
        container.addChild(labelOGL);

        container.addChild(context.createGraphicsInfoCard(
                Text.translatable("superresolution.screen.config.info.environment.opengl").getString(),
                GraphicsCapabilities.getGLVersionString(),
                GraphicsCapabilities.getGLExtensions()
        ));
        TitlePill labelVK = context.createSectionPill(
                Text.translatable("superresolution.screen.config.info.environment.vulkan").getString()
        );
        labelVK.layout().setMargin(YogaEdge.TOP, 8);
        labelVK.layout().setMargin(YogaEdge.BOTTOM, 6);
        container.addChild(labelVK);

        container.addChild(context.createGraphicsInfoCard(
                Text.translatable("superresolution.screen.config.info.environment.vulkan").getString(),
                GraphicsCapabilities.getVulkanVersionString(),
                GraphicsCapabilities.getVulkanDeviceExtensions()
        ));

        context.finalizeFrame(frame, container);
        return frame;
    }

}
