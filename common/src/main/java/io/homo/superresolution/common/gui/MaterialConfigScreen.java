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

import io.homo.superresolution.api.registry.AlgorithmDescription;
import io.homo.superresolution.api.registry.AlgorithmRegistry;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.config.enums.CaptureMode;
import io.homo.superresolution.common.config.enums.InternalTextureFormat;
import io.homo.superresolution.common.gui.impl.Text;
import io.homo.superresolution.common.gui.options.OptionBuilder;
import io.homo.superresolution.common.gui.options.OptionCategory;
import io.homo.superresolution.common.minecraft.MinecraftWindow;
import io.homo.superresolution.common.minecraft.handler.RenderHandlerManager;
import io.homo.superresolution.common.upscale.AlgorithmDescriptions;
import io.homo.superresolution.core.gui.*;
import io.homo.superresolution.core.gui.core.ContainerWidget;
import io.homo.superresolution.core.gui.core.UIInputState;
import io.homo.superresolution.core.gui.core.backends.render.RenderContext;
import io.homo.superresolution.core.gui.core.frame.Frame;
import io.homo.superresolution.core.gui.core.frame.ScrollableFrame;
import io.homo.superresolution.core.gui.widgets.SpacerWidget;
import io.homo.superresolution.core.gui.widgets.label.MaterialLabel;
import io.homo.superresolution.core.gui.widgets.navigation.drawer.MaterialNavigationDrawer;
import io.homo.superresolution.core.utils.Color;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.joml.Vector2f;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class MaterialConfigScreen extends NanoVGScreen<MaterialConfigScreen> {
    private MaterialScheme materialScheme;
    private String currentContentKey = "general";
    private Map<String, Frame> contentFrames;
    private YogaNode navigationDrawerLayout;
    private YogaNode contentLayout;
    private Frame currentContentFrame;
    private MaterialNavigationDrawer drawer;
    private final Screen parentScreen;

    public MaterialConfigScreen(Screen parentScreen) {
        super(Component.translatable("superresolution.screen.config.name"));
        this.parentScreen = parentScreen;
    }

    @Override
    protected void buildWidgets() {
        MaterialUI.setScheme(MaterialScheme.from(SuperResolutionConfig.getTheme(), Color.from("#6750A4")));
        materialScheme = MaterialUI.Scheme;
        contentFrames = new HashMap<>();
        currentContentKey = "general";

        getView().removeFrame(getDefaultFrame());

        Frame navigationDrawerFrame = createNavigationDrawerFrame();
        navigationDrawerLayout = getView().addFrame(navigationDrawerFrame);
        navigationDrawerLayout.setMinHeightPercent(100);
        navigationDrawerLayout.setFlexShrink(0);
        navigationDrawerLayout.setPadding(YogaEdge.ALL, 0);

        currentContentFrame = getOrCreateContentFrame(currentContentKey);
        contentLayout = getView().addFrame(currentContentFrame);
        contentLayout.setFlexGrow(1f);
        contentLayout.setHeightPercent(100);
        contentLayout.setPadding(YogaEdge.ALL, 0);
        SuperResolutionConfig.SPEC.load();
    }

    private Frame getOrCreateContentFrame(String key) {
        if (contentFrames.containsKey(key)) {
            return contentFrames.get(key);
        }
        Frame frame;
        switch (key) {
            case "general":
                frame = createGeneralFrame();
                break;
            case "advanced":
                frame = createAdvancedFrame();
                break;
            default:
                frame = createEmptyFrame();
        }
        contentFrames.put(key, frame);
        return frame;
    }

    private void switchContentFrame(String key) {
        if (key.equals(currentContentKey)) {
            return;
        }
        if (currentContentFrame != null) {
            getView().removeFrame(currentContentFrame);
        }
        currentContentKey = key;
        currentContentFrame = getOrCreateContentFrame(key);
        contentLayout = getView().addFrame(currentContentFrame);
        contentLayout.setFlexGrow(1f);
        contentLayout.setHeightPercent(100);
        contentLayout.setPadding(YogaEdge.ALL, 0);
        view.markLayoutDirty();
    }

    private Frame createNavigationDrawerFrame() {
        Frame frame = new Frame();
        //frame.setHorizontalScrollEnabled(false);
        //frame.setVerticalScrollEnabled(true);
        ContainerWidget container = new ContainerWidget();
        container.layout().setFlexDirection(YogaFlexDirection.COLUMN);
        container.layout().setWidthPercent(100);

        drawer = MaterialNavigationDrawer.create()
                .addHeader("Super Resolution", LogoRenderer.Logo)
                .addSectionHeader(Text.translatable("superresolution.screen.config.section.config").getString())
                .addItem(Text.translatable("superresolution.screen.config.section.general").getString(), MaterialSymbols.iconSettings(), "general")
                .addItem(Text.translatable("superresolution.screen.config.section.advanced").getString(), MaterialSymbols.iconTune(), "advanced")
                .onItemSelected(item -> {
                    String key = String.valueOf(item.getValue());
                    switchContentFrame(key);
                })
                .setSelectedByValue("general");
        drawer.layout().setWidthPercent(100);
        drawer.layout().setHeightPercent(100);
        container.addChild(drawer);

        frame.setRoot(container);
        return frame;
    }

    private Frame createGeneralFrame() {
        ScrollableFrame frame = new ScrollableFrame();
        frame.setContentPadding(20, 0, 20, 0);
        frame.setVerticalScrollEnabled(true);
        frame.setHorizontalScrollEnabled(false);

        ContainerWidget container = new ContainerWidget();
        container.layout().setFlexDirection(YogaFlexDirection.COLUMN);
        container.layout().setWidthPercent(100);
        container.layout().setGap(YogaGutter.COLUMN, 15);
        container.layout().setAlignItems(YogaAlign.FLEX_START);

        SpacerWidget spacerTop = SpacerWidget.vertical(20f);
        container.addChild(spacerTop);

        MaterialLabel title = MaterialLabel.create()
                .text(Text.translatable("superresolution.screen.config.section.general").getString())
                .fontSize(24)
                .color(MaterialScheme::primary);
        title.layout().setMargin(YogaEdge.BOTTOM, 20);
        container.addChild(title);

        OptionCategory category = new OptionCategory(Text.translatable("superresolution.screen.config.category.general"));
        OptionBuilder builder = new OptionBuilder(category);
        builder.setSaveRunnable(SuperResolutionConfig.SPEC::save);

        builder.enumSelectorOption(
                        Text.translatable("superresolution.screen.config.options.label.theme"),
                        MaterialTheme.class,
                        SuperResolutionConfig.getTheme())
                .setDefaultValue(MaterialTheme.Light)
                .setEnumNameProvider(Enum::name)
                .setSaveConsumer(value -> {
                    SuperResolutionConfig.setTheme(value);
                    MaterialUI.setScheme(MaterialScheme.from(value, Color.from("#6750A4")));
                    this.materialScheme = MaterialUI.Scheme;
                })
                .build();

        builder.booleanOption(
                        Text.translatable("superresolution.screen.config.options.label.enable_upscale"),
                        SuperResolutionConfig.isEnableUpscaleOriginal())
                .setDescription(Text.translatable("superresolution.screen.config.options.tooltip.enable_upscale"))
                .setDefaultValue(() -> true)
                .setSaveConsumer(SuperResolutionConfig::setEnableUpscale)
                .build();

        builder.booleanOption(
                        Text.translatable("superresolution.screen.config.options.label.disable_upscale_on_vanilla"),
                        SuperResolutionConfig.isDisableUpscaleOnVanilla())
                .setDescription(Text.translatable("superresolution.screen.config.options.tooltip.disable_upscale_on_vanilla"))
                .setDefaultValue(() -> false)
                .setSaveConsumer(SuperResolutionConfig::setDisableUpscaleOnVanilla)
                .build();
        builder.selectorOption(
                        Text.translatable("superresolution.screen.config.options.label.algo_type"),
                        SuperResolutionConfig.getUpscaleAlgorithm(),
                        AlgorithmRegistry.getAlgorithmMap().values().toArray())
                .setNameProvider(algo -> ((AlgorithmDescription<?>) algo).getBriefName())
                .setDefaultValue(() -> AlgorithmDescriptions.SGSR1)
                .setSaveConsumer(algo -> SuperResolutionConfig.setUpscaleAlgorithm((AlgorithmDescription<?>) algo))
                .build();
        builder.numberOption(
                        Text.translatable("superresolution.screen.config.options.label.upscale_ratio"),
                        SuperResolutionConfig.getUpscaleRatio(),
                        3.0,
                        SuperResolutionConfig.getMinUpscaleRatio())
                .setStep(0.01)
                .setValueFormater(v -> String.format("%.2f", v.doubleValue()))
                .setDefaultValue(() -> 1.7)
                .setDescriptionsSupplier(
                        (value -> Optional.of(new Text[]{Text.literal(Text.translatable("superresolution.screen.config.options.tooltip.upscale_ratio").getString().formatted(
                                String.format("%.0f", RenderHandlerManager.getScreenWidth() / value.floatValue()),
                                String.format("%.0f", RenderHandlerManager.getScreenHeight() / value.floatValue()),
                                String.format("%.2f", ((1 / value.floatValue()) * 100)) + "%"
                        ))}))
                )
                .setSaveConsumer((value) -> {
                    SuperResolutionConfig.setUpscaleRatio(value.floatValue());
                })
                .build();

        builder.numberOption(
                        Text.translatable("superresolution.screen.config.options.label.sharpness"),
                        SuperResolutionConfig.getSharpness(),
                        1.0,
                        0.0)
                .setStep(0.01)
                .setValueFormater(v -> String.format("%.2f", v.doubleValue()))
                .setDefaultValue(() -> 0.55)
                .setValueFormater(v -> String.format("%.2f", v.doubleValue()))
                .setDescription(Text.translatable("superresolution.screen.config.options.tooltip.sharpness"))
                .setSaveConsumer((value) -> {
                    SuperResolutionConfig.setSharpness(value.floatValue());
                })
                .build();
        builder.booleanOption(
                        Text.translatable("superresolution.screen.config.options.label.generate_motion_vectors"),
                        SuperResolutionConfig.isGenerateMotionVectors())
                .setDescription(Text.translatable("superresolution.screen.config.options.tooltip.generate_motion_vectors"))
                .setDefaultValue(() -> false)
                .setSaveConsumer(SuperResolutionConfig::setGenerateMotionVectors)
                .build();

        builder.enumSelectorOption(
                        Text.translatable("superresolution.screen.config.options.label.capture_mode"),
                        CaptureMode.class,
                        SuperResolutionConfig.getCaptureMode())
                .setDefaultValue(CaptureMode.A)
                .setEnumNameProvider(mode -> mode.name())
                .setSaveConsumer(SuperResolutionConfig::setCaptureMode)
                .build();
        builder.booleanOption(
                        Text.translatable("superresolution.screen.config.options.label.pause_game_on_gui"),
                        SuperResolutionConfig.isPauseGameOnGui())
                .setDefaultValue(() -> false)
                .setSaveConsumer(SuperResolutionConfig::setPauseGameOnGui)
                .build();
        OptionBuilder.OptionsContainer optionsContainer = builder.build();
        optionsContainer.layout().setWidthPercent(100);
        container.addChild(optionsContainer);

        SpacerWidget spacerBottom = SpacerWidget.vertical(20f);
        container.addChild(spacerBottom);

        frame.setRoot(container);
        return frame;
    }

    private Frame createAdvancedFrame() {
        ScrollableFrame frame = new ScrollableFrame();
        frame.setContentPadding(20, 0, 20, 0);
        frame.setVerticalScrollEnabled(true);
        frame.setHorizontalScrollEnabled(false);

        ContainerWidget container = new ContainerWidget();
        container.layout().setFlexDirection(YogaFlexDirection.COLUMN);
        container.layout().setWidthPercent(100);
        container.layout().setGap(YogaGutter.COLUMN, 15);
        container.layout().setAlignItems(YogaAlign.FLEX_START);

        SpacerWidget spacerTop = SpacerWidget.vertical(20f);
        container.addChild(spacerTop);

        MaterialLabel title = MaterialLabel.create()
                .text(Text.translatable("superresolution.screen.config.section.advanced").getString())
                .fontSize(24)
                .color(MaterialScheme::primary);
        title.layout().setMargin(YogaEdge.BOTTOM, 20);
        container.addChild(title);

        OptionCategory category = new OptionCategory(Text.translatable("superresolution.screen.config.category.advanced"));
        OptionBuilder builder = new OptionBuilder(category);
        builder.setSaveRunnable(SuperResolutionConfig.SPEC::save);

        builder.booleanOption(
                        Text.translatable("superresolution.screen.config.options.label.skip_init_vulkan"),
                        SuperResolutionConfig.isSkipInitVulkan())
                .setDescription(Text.translatable("superresolution.screen.config.options.tooltip.skip_init_vulkan"))
                .setDefaultValue(() -> false)
                .setSaveConsumer(SuperResolutionConfig::setSkipInitVulkan)
                .build();

        builder.booleanOption(
                        Text.translatable("superresolution.screen.config.options.label.enable_compat_shader_compiler"),
                        SuperResolutionConfig.isEnableCompatShaderCompiler())
                .setDescription(Text.translatable("superresolution.screen.config.options.tooltip.enable_compat_shader_compiler"))
                .setDefaultValue(() -> false)
                .setSaveConsumer(SuperResolutionConfig::setEnableCompatShaderCompiler)
                .build();

        builder.booleanOption(
                        Text.translatable("superresolution.screen.config.options.label.enable_detailed_profiling"),
                        SuperResolutionConfig.isEnableDetailedProfiling())
                .setDescription(Text.translatable("superresolution.screen.config.options.tooltip.enable_detailed_profiling"))
                .setDefaultValue(() -> false)
                .setSaveConsumer(SuperResolutionConfig::setEnableDetailedProfiling)
                .build();

        builder.enumSelectorOption(
                        Text.translatable("superresolution.screen.config.options.label.internal_texture_format"),
                        InternalTextureFormat.class,
                        SuperResolutionConfig.INTERNAL_TEXTURE_FORMAT.get())
                .setDescription(Text.translatable("superresolution.screen.config.options.tooltip.internal_texture_format"))
                .setDefaultValue(SuperResolutionConfig.INTERNAL_TEXTURE_FORMAT.getDefault())
                .setEnumNameProvider(format -> format.name())
                .setSaveConsumer(SuperResolutionConfig::setInternalTextureFormat)
                .build();

        builder.booleanOption(
                        Text.translatable("superresolution.screen.config.options.label.force_disable_shader_compat"),
                        SuperResolutionConfig.isForceDisableShaderCompat())
                .setDescription(Text.translatable("superresolution.screen.config.options.tooltip.force_disable_shader_compat"))
                .setDefaultValue(() -> false)
                .setSaveConsumer(SuperResolutionConfig::setForceDisableShaderCompat)
                .build();

        OptionBuilder.OptionsContainer optionsContainer = builder.build();
        optionsContainer.layout().setWidthPercent(100);
        container.addChild(optionsContainer);

        SpacerWidget spacerBottom = SpacerWidget.vertical(20f);
        container.addChild(spacerBottom);

        frame.setRoot(container);
        return frame;
    }

    private Frame createEmptyFrame() {
        ScrollableFrame frame = new ScrollableFrame();
        ContainerWidget container = new ContainerWidget();
        container.layout().setFlexDirection(YogaFlexDirection.COLUMN);
        container.layout().setWidthPercent(100);
        frame.setRoot(container);
        return frame;
    }

    @Override
    public void draw(RenderContext ctx, UIInputState inputState) {
        if (Minecraft.getInstance().level == null) {
            Vector2f screenSize = MinecraftWindow.getWindowSize();
            ctx.rect(
                    0,
                    0,
                    screenSize.x,
                    screenSize.y,
                    materialScheme.background(),
                    true);
        }

        float drawerWidth = drawer.getPreferredWidth(ctx);
        if (drawerWidth > 0) {
            navigationDrawerLayout.setWidth(drawerWidth);
            view.markLayoutDirty();
        }

        super.draw(ctx, inputState);
    }

    public void setMaterialScheme(MaterialScheme scheme) {
        this.materialScheme = scheme;
    }

    public boolean isPauseScreen() {
        return SuperResolutionConfig.isPauseGameOnGui();
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parentScreen);
        }
    }
}
