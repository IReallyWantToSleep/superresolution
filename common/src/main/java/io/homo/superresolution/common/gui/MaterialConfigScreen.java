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
import io.homo.superresolution.common.config.special.SpecialConfig;
import io.homo.superresolution.common.config.special.SpecialConfigDescription;
import io.homo.superresolution.common.gui.impl.OptionRequirement;
import io.homo.superresolution.common.gui.impl.Text;
import io.homo.superresolution.common.gui.options.EnumSelectorBuilder;
import io.homo.superresolution.common.gui.options.OptionBuilder;
import io.homo.superresolution.common.gui.options.OptionCategory;
import io.homo.superresolution.common.minecraft.MinecraftWindow;
import io.homo.superresolution.common.minecraft.handler.RenderHandlerManager;
import io.homo.superresolution.common.upscale.AlgorithmDescriptions;
import io.homo.superresolution.core.gui.*;
import io.homo.superresolution.common.perf.PerformanceTracker;
import io.homo.superresolution.core.gui.core.ContainerWidget;
import io.homo.superresolution.core.gui.core.UIInputState;
import io.homo.superresolution.core.gui.core.backends.render.RenderContext;
import io.homo.superresolution.core.gui.core.frame.Frame;
import io.homo.superresolution.core.gui.core.frame.ScrollableFrame;
import io.homo.superresolution.core.impl.Pair;
import io.homo.superresolution.core.gui.widgets.SpacerWidget;
import io.homo.superresolution.core.gui.widgets.chart.MaterialChartDataSeries;
import io.homo.superresolution.core.gui.widgets.chart.MaterialChartType;
import io.homo.superresolution.core.gui.widgets.chart.MaterialChart;
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
import java.util.function.Consumer;
import java.util.function.Function;

public class MaterialConfigScreen extends NanoVGScreen<MaterialConfigScreen> {
    private final Screen parentScreen;
    private MaterialScheme materialScheme;
    private String currentContentKey = "general";
    private Map<String, Frame> contentFrames;
    private YogaNode navigationDrawerLayout;
    private YogaNode contentLayout;
    private Frame currentContentFrame;
    private MaterialNavigationDrawer drawer;

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
        navigationDrawerLayout.setFlexShrink(0);
        navigationDrawerLayout.setPadding(YogaEdge.ALL, 0);

        currentContentFrame = getOrCreateContentFrame(currentContentKey);
        contentLayout = getView().addFrame(currentContentFrame);
        contentLayout.setFlexGrow(1f);
        contentLayout.setHeightPercent(100);
        contentLayout.setPadding(YogaEdge.ALL, 0);
        SuperResolutionConfig.SPEC.load();
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parentScreen);
        }
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
        drawer.layout().setMinHeight(ctx.viewportHeight());
        view.markLayoutDirty();

        super.draw(ctx, inputState);
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
            case "algorithm":
                frame = createAlgorithmFrame();
                break;
            case "experimental":
                frame = createExperimentalFrame();
                break;
            case "appearance":
                frame = createAppearanceFrame();
                //frame = createEmptyFrame();
                break;
            case "performance":
                frame = createPerformanceFrame();
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
        Frame frame = new ScrollableFrame();
        //frame.setHorizontalScrollEnabled(false);
        //frame.setVerticalScrollEnabled(true);
        ContainerWidget container = new ContainerWidget();
        container.layout().setFlexDirection(YogaFlexDirection.COLUMN);
        container.layout().setWidthPercent(100);

        drawer = MaterialNavigationDrawer.create()
                .addHeader(Text.literal("Super Resolution").getString(), LogoRenderer.Logo)
                .addSectionHeader(Text.translatable("superresolution.screen.config.section.config").getString())
                .addItem(Text.translatable("superresolution.screen.config.section.general").getString(), MaterialSymbols.iconSettings(), "general")
                .addItem(Text.translatable("superresolution.screen.config.section.advanced").getString(), MaterialSymbols.iconTune(), "advanced")
                .addItem(Text.translatable("superresolution.screen.config.section.algorithm").getString(), MaterialSymbols.iconMemory(), "algorithm")
                .addItem(Text.translatable("superresolution.screen.config.section.appearance").getString(), MaterialSymbols.iconPalette(), "appearance")
                .addItem(Text.translatable("superresolution.screen.config.section.experimental").getString(), MaterialSymbols.iconScience(), "experimental")
                .addSectionHeader(Text.translatable("superresolution.screen.config.section.profiling").getString())
                .addItem(Text.translatable("superresolution.screen.config.section.performance").getString(), MaterialSymbols.iconSpeed(), "performance")
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

    private Frame createAppearanceFrame() {
        ScrollableFrame frame = createStandardScrollableFrame();
        ContainerWidget container = createStandardContainer();
        addFrameTitle(container, Text.translatable("superresolution.screen.config.section.appearance"));
        OptionBuilder builder = createOptionBuilder(Text.translatable("superresolution.screen.config.category.appearance"));
        builder.enumSelectorOption(
                        Text.translatable("superresolution.screen.config.options.label.theme"),
                        MaterialTheme.class,
                        SuperResolutionConfig.getTheme())
                .setDefaultValue(MaterialTheme.Light)
                .setEnumNameProvider(t -> Text.translatable("superresolution.enum.theme." + t.name().toLowerCase()).getString())
                .setSaveConsumer(value -> {
                    SuperResolutionConfig.setTheme(value);
                    MaterialUI.setScheme(MaterialScheme.from(value, Color.from("#6750A4")));
                    this.materialScheme = MaterialUI.Scheme;
                })
                .build();

        addOptionGroupToContainer(container, builder);
        finalizeFrame(frame, container);
        return frame;
    }

    private Frame createGeneralFrame() {
        ScrollableFrame frame = createStandardScrollableFrame();
        ContainerWidget container = createStandardContainer();
        addFrameTitle(container, Text.translatable("superresolution.screen.config.section.general"));

        OptionBuilder builder = createOptionBuilder(Text.translatable("superresolution.screen.config.category.general"));
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
                .setItemEnableRequirement((value) -> {
                    AlgorithmDescription<?> algorithmDescription = (AlgorithmDescription<?>) value;
                    return OptionRequirement.all(
                            () -> AlgorithmRegistry.isAlgorithmSupported(algorithmDescription),
                            () -> {
                                if (algorithmDescription.equals(AlgorithmDescriptions.DLSS) && !SuperResolutionConfig.isEnableExperimentalFeatures()) {
                                    return false;
                                }
                                if (algorithmDescription.equals(AlgorithmDescriptions.XESS) && !SuperResolutionConfig.isEnableExperimentalFeatures()) {
                                    return false;
                                }
                                return true;
                            }
                    );
                })
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

        addOptionGroupToContainer(container, builder);
        finalizeFrame(frame, container);
        return frame;
    }

    private Frame createAdvancedFrame() {
        ScrollableFrame frame = createStandardScrollableFrame();
        ContainerWidget container = createStandardContainer();
        addFrameTitle(container, Text.translatable("superresolution.screen.config.section.advanced"));

        OptionBuilder builder = createOptionBuilder(Text.translatable("superresolution.screen.config.category.advanced"));

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

        addOptionGroupToContainer(container, builder);
        finalizeFrame(frame, container);
        return frame;
    }

    private Frame createExperimentalFrame() {
        ScrollableFrame frame = createStandardScrollableFrame();
        ContainerWidget container = createStandardContainer();
        addFrameTitle(container, Text.translatable("superresolution.screen.config.section.experimental"));

        OptionBuilder builder = createOptionBuilder(Text.translatable("superresolution.screen.config.category.experimental"));

        builder.booleanOption(
                        Text.translatable("superresolution.screen.config.options.label.enable_experimental_features"),
                        SuperResolutionConfig.isEnableExperimentalFeatures())
                .setDescription(Text.translatable("superresolution.screen.config.options.tooltip.enable_experimental_features"))
                .setDefaultValue(() -> false)
                .setSaveConsumer(SuperResolutionConfig::setEnableExperimentalFeatures)
                .build();

        builder.booleanOption(
                        Text.translatable("superresolution.screen.config.options.label.generate_motion_vectors"),
                        SuperResolutionConfig.isGenerateMotionVectors())
                .setDescription(Text.translatable("superresolution.screen.config.options.tooltip.generate_motion_vectors"))
                .setDefaultValue(() -> false)
                .setSaveConsumer(SuperResolutionConfig::setGenerateMotionVectors)
                .setEnableRequirement(SuperResolutionConfig::isEnableExperimentalFeatures)
                .build();
        addOptionGroupToContainer(container, builder);
        finalizeFrame(frame, container);
        return frame;
    }

    private ScrollableFrame createStandardScrollableFrame() {
        ScrollableFrame frame = new ScrollableFrame();
        frame.setContentPadding(20, 0, 20, 0);
        frame.setVerticalScrollEnabled(true);
        frame.setHorizontalScrollEnabled(false);
        return frame;
    }

    private ContainerWidget createStandardContainer() {
        ContainerWidget container = new ContainerWidget();
        container.layout().setFlexDirection(YogaFlexDirection.COLUMN);
        container.layout().setWidthPercent(100);
        container.layout().setGap(YogaGutter.COLUMN, 15);
        container.layout().setAlignItems(YogaAlign.FLEX_START);
        return container;
    }

    private void addFrameTitle(ContainerWidget container, Text title) {
        container.addChild(SpacerWidget.vertical(20f));
        MaterialLabel titleLabel = MaterialLabel.create()
                .text(title.getString())
                .fontSize(24)
                .color(MaterialScheme::primary);
        titleLabel.layout().setMargin(YogaEdge.BOTTOM, 20);
        container.addChild(titleLabel);
    }

    private OptionBuilder createOptionBuilder(Text categoryName) {
        OptionCategory category = new OptionCategory(categoryName);
        OptionBuilder builder = new OptionBuilder(category);
        builder.setSaveRunnable(SuperResolutionConfig.SPEC::save);
        return builder;
    }

    private void addOptionGroupToContainer(ContainerWidget container, OptionBuilder builder) {
        OptionBuilder.OptionsContainer optionsContainer = builder.build();
        optionsContainer.layout().setWidthPercent(100);
        container.addChild(optionsContainer);
    }

    private void addLabeledOptionGroup(ContainerWidget container, Text groupLabel, Consumer<OptionBuilder> configurator) {
        MaterialLabel label = MaterialLabel.create()
                .text(groupLabel.getString())
                .fontSize(18)
                .color(MaterialScheme::secondary);
        label.layout().setMargin(YogaEdge.TOP, 8);
        label.layout().setMargin(YogaEdge.BOTTOM, 6);
        container.addChild(label);

        OptionBuilder builder = createOptionBuilder(groupLabel);
        configurator.accept(builder);
        addOptionGroupToContainer(container, builder);
    }

    private void finalizeFrame(ScrollableFrame frame, ContainerWidget container) {
        container.addChild(SpacerWidget.vertical(20f));
        frame.setRoot(container);
    }

    @SuppressWarnings("unchecked")
    private Frame createAlgorithmFrame() {
        ScrollableFrame frame = createStandardScrollableFrame();
        ContainerWidget container = createStandardContainer();
        addFrameTitle(container, Text.translatable("superresolution.screen.config.section.algorithm"));

        for (String key : SuperResolutionConfig.SPECIAL.description.keySet()) {
            Pair<SpecialConfig, String> specialConfigPair = SuperResolutionConfig.SPECIAL.description.get(key);
            SpecialConfig specialConfig = specialConfigPair.left();
            String displayName = specialConfigPair.right();
            Map<String, SpecialConfigDescription<?>> configDescriptions = specialConfig.getDescriptions();

            if (configDescriptions.isEmpty()) {
                continue;
            }

            addLabeledOptionGroup(container, Text.literal(displayName), builder -> {
                for (String configKey : configDescriptions.keySet()) {
                    SpecialConfigDescription<?> desc = configDescriptions.get(configKey);
                    buildSpecialConfigOption(builder, desc);
                }
            });
        }

        finalizeFrame(frame, container);
        return frame;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void buildSpecialConfigOption(OptionBuilder builder, SpecialConfigDescription<?> desc) {
        Text optionName = Text.literal(desc.getName().getString());
        Optional<Component> tooltip = desc.getTooltip();

        switch (desc.getType()) {
            case BOOLEAN: {
                SpecialConfigDescription<Boolean> boolDesc = (SpecialConfigDescription<Boolean>) desc;
                var opt = builder.booleanOption(optionName, boolDesc.getValue())
                        .setDefaultValue(() -> boolDesc.getDefaultValue())
                        .setSaveConsumer(boolDesc.getSaveConsumer());
                if (tooltip.isPresent()) {
                    opt.setDescription(Text.literal(tooltip.get().getString()));
                }
                opt.build();
                break;
            }
            case ENUM: {
                SpecialConfigDescription enumDesc = (SpecialConfigDescription) desc;
                Class enumClass = enumDesc.getClazz();
                Enum enumValue = (Enum) enumDesc.getValue();
                Enum defaultEnumValue = (Enum) enumDesc.getDefaultValue();
                EnumSelectorBuilder<?> opt = (EnumSelectorBuilder<?>) builder.enumSelectorOption(optionName, enumClass, enumValue)
                        .setDefaultValue(defaultEnumValue)
                        .setSaveConsumer(enumDesc.getSaveConsumer());
                if (enumDesc.isValueNameIsSupplier()) {
                    opt.setEnumNameProvider(e ->
                            ((Function<Object, Optional<Component>>) enumDesc.getValueNameSupplierAsObject())
                                    .apply(e).orElse(Component.empty()).getString()
                    );
                }
                if (tooltip.isPresent()) {
                    opt.setDescription(Text.literal(tooltip.get().getString()));
                }
                opt.build();
                break;
            }
            case FLOAT: {
                SpecialConfigDescription<Float> floatDesc = (SpecialConfigDescription<Float>) desc;
                var opt = builder.numberOption(
                                optionName,
                                floatDesc.getValue(),
                                floatDesc.getValueRange().right(),
                                floatDesc.getValueRange().left()
                        )
                        .setStep(0.01)
                        .setDefaultValue(() -> floatDesc.getDefaultValue())
                        .setSaveConsumer(v -> floatDesc.getSaveConsumer().accept(v.floatValue()));
                if (floatDesc.isValueNameIsSupplier()) {
                    opt.setValueFormater(v ->
                            floatDesc.getValueNameSupplierAsObject().apply(v)
                                    .map(c -> c.getString())
                                    .orElse(String.format("%.2f", v.doubleValue()))
                    );
                } else {
                    opt.setValueFormater(v -> String.format("%.2f", v.doubleValue()));
                }
                if (tooltip.isPresent()) {
                    opt.setDescription(Text.literal(tooltip.get().getString()));
                }
                opt.build();
                break;
            }
            default:
                break;
        }
    }

    private Frame createPerformanceFrame() {
        ScrollableFrame frame = createStandardScrollableFrame();
        ContainerWidget container = createStandardContainer();
        addFrameTitle(container, Text.translatable("superresolution.screen.config.section.performance"));

        boolean detailedProfiling = SuperResolutionConfig.isEnableDetailedProfiling();

        Pair<String, Text>[] operations = new Pair[]{
                Pair.of("Frame", Text.translatable("superresolution.screen.config.section.performance.chart.frame")),
                Pair.of("Main Render", Text.translatable("superresolution.screen.config.section.performance.chart.main_render")),
                Pair.of("Level Render", Text.translatable("superresolution.screen.config.section.performance.chart.level_render")),
                Pair.of("Upscale", Text.translatable("superresolution.screen.config.section.performance.chart.upscale")),
        };

        for (Pair<String, Text> operation : operations) {
            MaterialLabel sectionLabel = MaterialLabel.create()
                    .text(operation.right().getString())
                    .fontSize(18)
                    .color(MaterialScheme::secondary);
            sectionLabel.layout().setMargin(YogaEdge.TOP, 12);
            sectionLabel.layout().setMargin(YogaEdge.BOTTOM, 6);
            container.addChild(sectionLabel);

            MaterialChart cpuChart = MaterialChart.create()
                    .title(operation.right().getString())
                    .addSeries(new MaterialChartDataSeries("CPU (ms)", Color.from("#4FC3F7"), MaterialChartType.Curve, 128))
                    .addSeries(new MaterialChartDataSeries("GPU (ms)", Color.from("#BA53FF"), MaterialChartType.Curve, 128))
                    .autoRange()
                    .valueFormatter(v -> String.format("%.2f ms", v))
                    .updateCallback(chart -> {
                        long[] cpuData = PerformanceTracker.getAllResultsCPU(operation.left());
                        MaterialChartDataSeries cpuSeries = chart.getSeries(0);
                        float[] msData = new float[cpuData.length];
                        for (int i = 0; i < cpuData.length; i++) {
                            msData[i] = cpuData[i] / 1_000_000f;
                        }
                        cpuSeries.setData(msData);
                        long[] gpuData = PerformanceTracker.getAllResultsGPU(operation.left());
                        MaterialChartDataSeries gpuSeries = chart.getSeries(1);
                        msData = new float[gpuData.length];
                        for (int i = 0; i < gpuData.length; i++) {
                            msData[i] = gpuData[i] / 1_000_000f;
                        }
                        gpuSeries.setData(msData);
                    })
                    .updateInterval(0); //每帧
            cpuChart.style()
                    .showAverage(true)
                    .showGrid(true)
                    .showLegend(true);
            cpuChart.layout().setWidthPercent(100);
            cpuChart.setElementHeight(180);
            container.addChild(cpuChart);
        }
        finalizeFrame(frame, container);
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

    public void setMaterialScheme(MaterialScheme scheme) {
        this.materialScheme = scheme;
    }

    public boolean isPauseScreen() {
        return SuperResolutionConfig.isPauseGameOnGui();
    }
}
