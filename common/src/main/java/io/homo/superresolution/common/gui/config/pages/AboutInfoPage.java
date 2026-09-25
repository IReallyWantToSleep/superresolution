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
import io.homo.superresolution.core.gui.*;
import io.homo.superresolution.core.gui.core.ContainerWidget;
import io.homo.superresolution.core.gui.core.frame.Frame;
import io.homo.superresolution.core.gui.core.frame.ScrollableFrame;
import io.homo.superresolution.core.gui.widgets.label.MaterialLabel;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.*;

import java.util.*;

import io.homo.superresolution.common.gui.config.pages.ConfigPageContext.TitlePill;
import io.homo.superresolution.common.gui.config.pages.ConfigPageContext.InfoCard;
import io.homo.superresolution.common.gui.config.pages.ConfigPageContext.ContributorInfo;
import io.homo.superresolution.common.gui.config.pages.ConfigPageContext.LibraryInfo;
import io.homo.superresolution.common.gui.config.pages.ConfigPageContext.SponsorWrappingRow;

public final class AboutInfoPage implements ConfigPage {
    public static final AboutInfoPage INSTANCE = new AboutInfoPage();

    private AboutInfoPage() {
    }

    @Override
    public Frame create(ConfigPageContext context) {
        return build(context);
    }

    private static Frame build(ConfigPageContext context) {
        ScrollableFrame frame = context.createStandardScrollableFrame();
        ContainerWidget container = context.createStandardContainer();
        context.addFrameTitle(container, Text.translatable("superresolution.screen.config.section.about"));
        container.addChild(context.createAboutBrandCard());

        TitlePill authorSection = context.createSectionPill(
                Text.translatable("superresolution.screen.info.text.author").getString()
        );
        authorSection.layout().setMargin(YogaEdge.BOTTOM, 6);
        container.addChild(authorSection);

        ContributorInfo author = new ContributorInfo(
                "187J3X1",
                Text.translatable("superresolution.screen.config.info.about.contributor.187j3x1.desc").getString(),
                "https://github.com/187J3X1-114514",
                "/assets/super_resolution/textures/gui/contributors/114514.png"
        );

        InfoCard authorCard = new InfoCard();
        authorCard.addChild(context.createContributorRow(author));
        container.addChild(authorCard);

        ContainerWidget contributorSectionRow = new ContainerWidget();
        contributorSectionRow.layout().setFlexDirection(YogaFlexDirection.ROW);
        contributorSectionRow.layout().setWidthPercent(100);
        contributorSectionRow.layout().setAlignItems(YogaAlign.CENTER);
        contributorSectionRow.layout().setJustifyContent(YogaJustify.SPACE_BETWEEN);
        contributorSectionRow.layout().setMargin(YogaEdge.TOP, 12);
        contributorSectionRow.layout().setMargin(YogaEdge.BOTTOM, 6);

        TitlePill contributorSection = context.createSectionPill(
                Text.translatable("superresolution.screen.info.text.contributors").getString()
        );
        contributorSectionRow.addChild(contributorSection);

        MaterialLabel contributorOrderHint = MaterialLabel.create()
                .text(Text.translatable("superresolution.screen.info.text.contributors_order_random").getString())
                .fontSize(11)
                .color(MaterialScheme::onSurfaceVariant);
        contributorOrderHint.style().sizeToContent(true);
        contributorSectionRow.addChild(contributorOrderHint);

        container.addChild(contributorSectionRow);

        InfoCard contributorsCard = new InfoCard();
        List<ContributorInfo> contributors = new ArrayList<>(List.of(
                new ContributorInfo("异世界美西螈", Text.translatable("superresolution.screen.config.info.about.contributor.ysjmxy.desc").getString(), "https://github.com/ysjmxy", "/assets/super_resolution/textures/gui/contributors/mxy.png"),
                new ContributorInfo("yu", Text.translatable("superresolution.screen.config.info.about.contributor.yu.desc").getString(), "https://github.com/yu234567", "/assets/super_resolution/textures/gui/contributors/yu.png"),
                new ContributorInfo("Enaium", Text.translatable("superresolution.screen.config.info.about.contributor.enaium.desc").getString(), "https://github.com/Enaium", "/assets/super_resolution/textures/gui/contributors/Enaium.png"),
                new ContributorInfo("rrtt217", Text.translatable("superresolution.screen.config.info.about.contributor.rrtt217.desc").getString(), "https://github.com/rrtt217", "/assets/super_resolution/textures/gui/contributors/rrtt217.png"),
                new ContributorInfo("筱烷", Text.translatable("superresolution.screen.config.info.about.contributor.shiroiame.desc").getString(), "https://github.com/Shiroiame-Kusu", "/assets/super_resolution/textures/gui/contributors/Shiroiame-Kusu.png"),
                new ContributorInfo("shiromizu", Text.translatable("superresolution.screen.config.info.about.contributor.shiromizu.desc").getString(), "https://github.com/shiromizu-hui", "/assets/super_resolution/textures/gui/contributors/shiromizu.png"),
                new ContributorInfo("eastear2333", Text.translatable("superresolution.screen.config.info.about.contributor.eastear2333.desc").getString(), "https://github.com/eastear23333", "/assets/super_resolution/textures/gui/contributors/eastear2333.png"),
                new ContributorInfo("ChloePrime", Text.translatable("superresolution.screen.config.info.about.contributor.chloeprime.desc").getString(), "https://github.com/ChloePrime", "/assets/super_resolution/textures/gui/contributors/ChloePrime.png"),
                new ContributorInfo("EnderPhantomWing", Text.translatable("superresolution.screen.config.info.about.contributor.enderphantomwing.desc").getString(), "https://github.com/EnderPhantomWing", "/assets/super_resolution/textures/gui/contributors/EnderPhantomWing.png"),
                new ContributorInfo("索德列斯", Text.translatable("superresolution.screen.config.info.about.contributor.suodeliesi.desc").getString(), "", "/assets/super_resolution/textures/gui/contributors/suodeliesi.png"),
                new ContributorInfo("小狼_枫琪", Text.translatable("superresolution.screen.config.info.about.contributor.xiaolang.desc").getString(), "", "/assets/super_resolution/textures/gui/contributors/xiaolangfengqi.png"),
                new ContributorInfo("qwertyuiop", Text.translatable("superresolution.screen.config.info.about.contributor.qwertyuiop.desc").getString(), "https://github.com/moyongxin", "/assets/super_resolution/textures/gui/contributors/qwertyuiop.png"),
                new ContributorInfo("猫猫狐AR", Text.translatable("superresolution.screen.config.info.about.contributor.ar.desc").getString(), "https://github.com/Argon4W", "/assets/super_resolution/textures/gui/contributors/ar.png"),
                new ContributorInfo("辰蒙", Text.translatable("superresolution.screen.config.info.about.contributor.chenmeng.desc").getString(), "https://github.com/slmpc", "/assets/super_resolution/textures/gui/contributors/chenmeng.png"),
                new ContributorInfo("Tahnass", Text.translatable("superresolution.screen.config.info.about.contributor.tahnass.desc").getString(), "https://github.com/Tahnass", "/assets/super_resolution/textures/gui/contributors/tahnass.png"),
                new ContributorInfo("StarsShine11904", Text.translatable("superresolution.screen.config.info.about.contributor.starsshine11904.desc").getString(), "https://github.com/StarsShine11904", "/assets/super_resolution/textures/gui/contributors/StarsShine11904.png"),
                new ContributorInfo("暇じゃない暇人", Text.translatable("superresolution.screen.config.info.about.contributor.nohimazin.desc").getString(), "https://github.com/nohimazin", "/assets/super_resolution/textures/gui/contributors/nohimazin.png"),
                new ContributorInfo("HaringPro", Text.translatable("superresolution.screen.config.info.about.contributor.haringpro.desc").getString(), "https://github.com/HaringPro", "/assets/super_resolution/textures/gui/contributors/haringpro.png"),
                new ContributorInfo("GeForceLegend", Text.translatable("superresolution.screen.config.info.about.contributor.geforcelegend.desc").getString(), "https://github.com/GeForceLegend", "/assets/super_resolution/textures/gui/contributors/geforcelegend.png"),
                new ContributorInfo("Havesten", Text.translatable("superresolution.screen.config.info.about.contributor.havesten.desc").getString(), "", "/assets/super_resolution/textures/gui/contributors/Havesten.png"),
                new ContributorInfo("sssxks", Text.translatable("superresolution.screen.config.info.about.contributor.sssxks.desc").getString(), "https://github.com/sssxks", "/assets/super_resolution/textures/gui/contributors/sssxks.png")
        ));
        Collections.shuffle(contributors);
        for (ContributorInfo contributor : contributors) {
            contributorsCard.addChild(context.createContributorRow(contributor));
        }
        container.addChild(contributorsCard);

        TitlePill sponsorSection = context.createSectionPill(
                Text.translatable("superresolution.screen.config.info.about.sponsors").getString()
        );
        sponsorSection.layout().setMargin(YogaEdge.TOP, 12);
        sponsorSection.layout().setMargin(YogaEdge.BOTTOM, 6);
        container.addChild(sponsorSection);

        InfoCard sponsorsCard = new InfoCard();
        ContainerWidget sponsorsContainer = new SponsorWrappingRow();
        sponsorsContainer.layout().setWidthPercent(100);
        sponsorsContainer.layout().setMinHeight(100);
        sponsorsCard.addChild(sponsorsContainer);
        container.addChild(sponsorsCard);
        context.showSponsorLoadingState(sponsorsContainer);
        context.loadSponsors(sponsorsContainer);

        TitlePill librarySection = context.createSectionPill(
                Text.translatable("superresolution.screen.config.info.about.libraries").getString()
        );
        librarySection.layout().setMargin(YogaEdge.TOP, 12);
        librarySection.layout().setMargin(YogaEdge.BOTTOM, 6);
        container.addChild(librarySection);

        InfoCard librariesCard = new InfoCard();
        List<LibraryInfo> libraries = new ArrayList<>(List.of(
                new LibraryInfo("Architectury API", "https://github.com/architectury/architectury-api"),
                new LibraryInfo("Night Config", "https://github.com/TheElectronWill/night-config"),
                new LibraryInfo("SpongePowered Mixin", "https://github.com/SpongePowered/Mixin"),
                new LibraryInfo("NanoVG", "https://github.com/memononen/nanovg"),
                new LibraryInfo("NanoSVG", "https://github.com/memononen/nanosvg"),
                new LibraryInfo("Manifold", "https://github.com/manifold-systems/manifold"),
                new LibraryInfo("Dear ImGui", "https://github.com/ocornut/imgui"),
                new LibraryInfo("Snapdragon™ Game Super Resolution 2(1)", "https://github.com/SnapdragonStudios/snapdragon-gsr"),
                new LibraryInfo("FidelityFX Super Resolution 1.0", "https://github.com/GPUOpen-Effects/FidelityFX-FSR"),
                new LibraryInfo("FidelityFX Super Resolution 2.2", "https://github.com/GPUOpen-Effects/FidelityFX-FSR2"),
                new LibraryInfo("AMD FidelityFX™ SDK", "https://github.com/GPUOpen-LibrariesAndSDKs/FidelityFX-SDK"),
                new LibraryInfo("FidelityFX Super Resolution 2.2 (OpenGL)", "https://github.com/JuanDiegoMontoya/FidelityFX-FSR2-OpenGL"),
                new LibraryInfo("Java OpenGL Math Library(JOML)", "https://github.com/JOML-CI/JOML"),
                new LibraryInfo("RenderDoc", "https://github.com/baldurk/renderdoc"),
                new LibraryInfo("Lightweight Java Game Library 3(LWJGL3)", "https://github.com/LWJGL/lwjgl3"),
                new LibraryInfo("Glslang", "https://github.com/KhronosGroup/glslang"),
                new LibraryInfo("Intel XeSS SDK", "https://github.com/intel/xess"),
                new LibraryInfo("NVIDIA RTX DLSS SDK", "https://github.com/NVIDIA/DLSS"),
                new LibraryInfo("JCPP", "https://github.com/shevek/jcpp")

        ));
        Collections.shuffle(libraries);
        for (LibraryInfo library : libraries) {
            librariesCard.addChild(context.createLibraryRow(library));
        }
        container.addChild(librariesCard);
        TitlePill legalSection = context.createSectionPill(
                Text.translatable("superresolution.screen.config.info.about.legal_notices").getString()
        );
        legalSection.layout().setMargin(YogaEdge.TOP, 12);
        legalSection.layout().setMargin(YogaEdge.BOTTOM, 6);
        container.addChild(legalSection);

        InfoCard noticesCard = new InfoCard();
        noticesCard.layout().setGap(YogaGutter.ROW, 12);

        {
            MaterialLabel label = MaterialLabel.create()
                    .text(Text.translatable("superresolution.screen.config.info.about.gpl_statement").getString());
            label.style().wrap(true);
            noticesCard.addChild(label);
        }
        {
            MaterialLabel label = MaterialLabel.create()
                    .text(Text.translatable("superresolution.screen.config.info.about.minecraft_disclaimer").getString());
            label.style().wrap(true);
            noticesCard.addChild(label);
        }
        {
            MaterialLabel label = MaterialLabel.create()
                    .text(Text.translatable("superresolution.screen.config.info.about.nvidia_disclaimer").getString());
            label.style().wrap(true);
            noticesCard.addChild(label);
        }
        {
            MaterialLabel label = MaterialLabel.create()
                    .text(Text.translatable("superresolution.screen.config.info.about.amd_disclaimer").getString());
            label.style().wrap(true);
            noticesCard.addChild(label);
        }
        {
            MaterialLabel label = MaterialLabel.create()
                    .text(Text.translatable("superresolution.screen.config.info.about.intel_disclaimer").getString());
            label.style().wrap(true);
            noticesCard.addChild(label);
        }
        container.addChild(noticesCard);

        context.finalizeFrame(frame, container);
        return frame;
    }

}
