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

import io.homo.superresolution.api.AbstractAlgorithm;
import io.homo.superresolution.api.QualityPreset;
import io.homo.superresolution.api.platform.OperatingSystemType;
import io.homo.superresolution.api.platform.Platform;
import io.homo.superresolution.api.registry.AlgorithmDescription;
import io.homo.superresolution.api.registry.AlgorithmRegistry;
import io.homo.superresolution.api.registry.ExtraResource;
import io.homo.superresolution.api.registry.ExtraResources;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.config.enums.CaptureMode;
import io.homo.superresolution.common.config.enums.InternalTextureFormat;
import io.homo.superresolution.common.config.special.SpecialConfig;
import io.homo.superresolution.common.config.special.SpecialConfigDescription;
import io.homo.superresolution.common.gui.download.MaterialDownloadList;
import io.homo.superresolution.common.gui.impl.OptionRequirement;
import io.homo.superresolution.common.gui.impl.Text;
import io.homo.superresolution.common.gui.options.*;
import io.homo.superresolution.common.minecraft.MinecraftWindow;
import io.homo.superresolution.common.minecraft.handler.RenderHandlerManager;
import io.homo.superresolution.common.minecraft.handler.shadercompat.ShaderCompatHandler;
import io.homo.superresolution.common.perf.PerformanceTracker;
import io.homo.superresolution.common.upscale.AlgorithmDescriptions;
import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.SuperResolutionConstants;
import io.homo.superresolution.core.SuperResolutionNative;
import io.homo.superresolution.core.graphics.GraphicsCapabilities;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.gui.*;
import io.homo.superresolution.core.gui.core.ContainerWidget;
import io.homo.superresolution.core.gui.core.UIInputState;
import io.homo.superresolution.core.gui.core.backends.interfaces.IImage;
import io.homo.superresolution.core.gui.core.backends.interfaces.IPaint;
import io.homo.superresolution.core.gui.core.backends.render.RenderContext;
import io.homo.superresolution.core.gui.core.frame.Frame;
import io.homo.superresolution.core.gui.core.frame.ScrollableFrame;
import io.homo.superresolution.core.gui.core.impl.Rectangle;
import io.homo.superresolution.core.gui.widgets.MaterialContainerWidget;
import io.homo.superresolution.core.gui.widgets.MaterialWidget;
import io.homo.superresolution.core.gui.widgets.SpacerWidget;
import io.homo.superresolution.core.gui.widgets.button.MaterialButton;
import io.homo.superresolution.core.gui.widgets.button.MaterialButtonSize;
import io.homo.superresolution.core.gui.widgets.button.MaterialButtonVariant;
import io.homo.superresolution.core.gui.widgets.chart.MaterialChart;
import io.homo.superresolution.core.gui.widgets.chart.MaterialChartDataSeries;
import io.homo.superresolution.core.gui.widgets.chart.MaterialChartType;
import io.homo.superresolution.core.gui.widgets.dialog.MaterialDialog;
import io.homo.superresolution.core.gui.widgets.label.MaterialLabel;
import io.homo.superresolution.core.gui.widgets.navigation.drawer.MaterialNavigationDrawer;
import io.homo.superresolution.core.impl.Destroyable;
import io.homo.superresolution.core.impl.Pair;
import io.homo.superresolution.core.utils.Color;
import io.homo.superresolution.core.utils.ImageLoader;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.joml.Vector2f;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
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
    private List<Destroyable> destroyables = new ArrayList<>();
    private Map<String, List<QualityPresetOption>> qualityPresetOptionsCache;

    public MaterialConfigScreen(Screen parentScreen) {
        super(Component.translatable("superresolution.screen.config.name"));
        this.parentScreen = parentScreen;
    }

    @Override
    protected void buildWidgets() {
        if (qualityPresetOptionsCache == null) {
            qualityPresetOptionsCache = new HashMap<>();
        }
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
        destroyables.forEach(Destroyable::destroy);
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
            case "debug":
                frame = createDebugFrame();
                break;
            case "info_environment":
                frame = createEnvironmentInfoFrame();
                break;
            case "info_about":
                frame = createAboutInfoFrame();
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
        ScrollableFrame frame = new ScrollableFrame();
        frame.setHorizontalScrollEnabled(false);
        frame.setVerticalScrollEnabled(true);
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
                .addItem(Text.translatable("superresolution.screen.config.section.debug").getString(), MaterialSymbols.iconBugReport(), "debug")
                .addItem(Text.translatable("superresolution.screen.config.section.experimental").getString(), MaterialSymbols.iconScience(), "experimental")
                .addSectionHeader(Text.translatable("superresolution.screen.config.section.profiling").getString())
                .addItem(Text.translatable("superresolution.screen.config.section.performance").getString(), MaterialSymbols.iconSpeed(), "performance")
                .addSectionHeader(Text.translatable("superresolution.screen.config.section.information").getString())
                .addItem(Text.translatable("superresolution.screen.config.section.environment").getString(), MaterialSymbols.iconInfo(), "info_environment")
                .addItem(Text.translatable("superresolution.screen.config.section.about").getString(), MaterialSymbols.iconInfo(), "info_about")
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

        @SuppressWarnings("unchecked")
        final SelectionListOptionEntry<QualityPresetOption>[] qualityPresetEntryRef = new SelectionListOptionEntry[1];
        final NumberSliderOptionEntry[] upscaleRatioEntryRef = new NumberSliderOptionEntry[1];
        final boolean[] syncingQualityPreset = {false};

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
                .setSaveConsumer((obj) -> {
                    AlgorithmDescription<?> algo = (AlgorithmDescription<?>) obj;
                    List<ExtraResource> lostResources = algo.getExtraResources().checkAll(SuperResolutionConstants.NATIVE_LIBRARIES_DIR);
                    if (!lostResources.isEmpty()) {
                        openLostResourceDialog(lostResources);
                        return false;
                    }
                    SuperResolutionConfig.setUpscaleAlgorithm(algo);
                    if (qualityPresetEntryRef[0] != null) {
                        qualityPresetEntryRef[0].refreshDynamicValues();
                        QualityPresetOption targetPreset = resolveQualityPresetOption(
                                qualityPresetEntryRef[0].getValues(),
                                SuperResolutionConfig.getUpscaleRatio()
                        );
                        qualityPresetEntryRef[0].setSelectedValue(targetPreset);

                        if (!isAlgorithmSupportsCustomUpscaleRatio(algo)
                                && targetPreset != null
                                && !targetPreset.custom()) {
                            syncingQualityPreset[0] = true;
                            try {
                                SuperResolutionConfig.setUpscaleRatio(targetPreset.upscaleRatio());
                                if (upscaleRatioEntryRef[0] != null) {
                                    upscaleRatioEntryRef[0].setCurrentValue(targetPreset.upscaleRatio());
                                }
                            } finally {
                                syncingQualityPreset[0] = false;
                            }
                        }
                    }
                    if (ShaderCompatHandler.dontHackMinecraftRenderingPipeline()) {
                        ShaderCompatHandler.irisApiReloadShader();
                    }
                    return true;
                })
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

        List<QualityPresetOption> initialPresetOptions = getQualityPresetOptions(SuperResolutionConfig.getUpscaleAlgorithm());
        QualityPresetOption initialPreset = resolveQualityPresetOption(
                initialPresetOptions,
                SuperResolutionConfig.getUpscaleRatio()
        );
        qualityPresetEntryRef[0] = builder.selectorOption(
                        Text.translatable("superresolution.screen.config.options.label.quality_preset"),
                        initialPreset,
                        initialPresetOptions.toArray(new QualityPresetOption[0]))
                .setNameProvider(QualityPresetOption::displayName)
                .setValuesSupplier(() -> getQualityPresetOptions(SuperResolutionConfig.getUpscaleAlgorithm()))
                .setSaveConsumer((presetOption) -> {
                    if (presetOption == null || presetOption.custom() || syncingQualityPreset[0]) {
                        return true;
                    }
                    syncingQualityPreset[0] = true;
                    try {
                        float ratio = presetOption.upscaleRatio();
                        SuperResolutionConfig.setUpscaleRatio(ratio);
                        if (upscaleRatioEntryRef[0] != null) {
                            upscaleRatioEntryRef[0].setCurrentValue(ratio);
                        }
                    } finally {
                        syncingQualityPreset[0] = false;
                    }
                    if (ShaderCompatHandler.dontHackMinecraftRenderingPipeline()) {
                        ShaderCompatHandler.irisApiReloadShader();
                    }
                    return true;
                })
                .build();

        upscaleRatioEntryRef[0] = builder.numberOption(
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
                .setEnableRequirement(() -> isAlgorithmSupportsCustomUpscaleRatio(SuperResolutionConfig.getUpscaleAlgorithm()))
                .setSaveConsumer((value) -> {
                    float targetRatio = Float.parseFloat(String.format("%.2f", value.doubleValue()));
                    SuperResolutionConfig.setUpscaleRatio(targetRatio);
                    if (qualityPresetEntryRef[0] != null && !syncingQualityPreset[0]) {
                        QualityPresetOption targetPreset = resolveQualityPresetOption(
                                qualityPresetEntryRef[0].getValues(),
                                targetRatio
                        );
                        qualityPresetEntryRef[0].setSelectedValue(targetPreset);
                    }
                    if (ShaderCompatHandler.dontHackMinecraftRenderingPipeline()) {
                        ShaderCompatHandler.irisApiReloadShader();
                    }
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

    private List<QualityPresetOption> getQualityPresetOptions(AlgorithmDescription<?> algorithmDescription) {
        if (algorithmDescription == null) {
            return List.of(createCustomQualityPresetOption(SuperResolutionConfig.getUpscaleRatio()));
        }

        Map<String, List<QualityPresetOption>> cache = getQualityPresetOptionsCache();
        List<QualityPresetOption> baseOptions = cache.computeIfAbsent(algorithmDescription.getCodeName(), codeName -> {
            List<QualityPresetOption> options = new ArrayList<>();
            for (QualityPreset preset : getAlgorithmQualityPresets(algorithmDescription)) {
                String presetName = preset.getName() == null ? preset.getCodeName() : preset.getName().getString();
                options.add(new QualityPresetOption(
                        preset.getCodeName(),
                        presetName,
                        preset.getUpscaleRatio(),
                        false
                ));
            }
            return options;
        });

        List<QualityPresetOption> options = new ArrayList<>(baseOptions);
        if (isAlgorithmSupportsCustomUpscaleRatio(algorithmDescription)) {
            options.add(createCustomQualityPresetOption(SuperResolutionConfig.getUpscaleRatio()));
        }
        return options;
    }

    private List<QualityPreset> getAlgorithmQualityPresets(AlgorithmDescription<?> algorithmDescription) {
        if (algorithmDescription == null) {
            return List.of();
        }

        if (SuperResolution.algorithmDescription != null
                && SuperResolution.algorithmDescription.equals(algorithmDescription)
                && SuperResolution.currentAlgorithm != null) {
            return SuperResolution.currentAlgorithm.getQualityPresets();
        }

        AbstractAlgorithm algorithm = null;
        try {
            algorithm = algorithmDescription.createNewInstance();
            return new ArrayList<>(algorithm.getQualityPresets());
        } catch (Exception ignored) {
            return List.of();
        } finally {
            if (algorithm != null) {
                try {
                    algorithm.destroy();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private QualityPresetOption resolveQualityPresetOption(List<QualityPresetOption> options, float ratio) {
        if (options == null || options.isEmpty()) {
            return createCustomQualityPresetOption(ratio);
        }
        for (QualityPresetOption option : options) {
            if (!option.custom() && isSameRatio(option.upscaleRatio(), ratio)) {
                return option;
            }
        }
        for (QualityPresetOption option : options) {
            if (option.custom()) {
                return option;
            }
        }
        QualityPresetOption closest = options.get(0);
        float closestDiff = Math.abs(closest.upscaleRatio() - ratio);
        for (int i = 1; i < options.size(); i++) {
            QualityPresetOption option = options.get(i);
            float diff = Math.abs(option.upscaleRatio() - ratio);
            if (diff < closestDiff) {
                closest = option;
                closestDiff = diff;
            }
        }
        return closest;
    }

    private boolean isAlgorithmSupportsCustomUpscaleRatio(AlgorithmDescription<?> algorithmDescription) {
        if (algorithmDescription == null) {
            return true;
        }

        if (SuperResolution.algorithmDescription != null
                && SuperResolution.algorithmDescription.equals(algorithmDescription)
                && SuperResolution.currentAlgorithm != null) {
            return SuperResolution.currentAlgorithm.isCustomUpscaleRatio();
        }

        AbstractAlgorithm algorithm = null;
        try {
            algorithm = algorithmDescription.createNewInstance();
            return algorithm.isCustomUpscaleRatio();
        } catch (Exception ignored) {
            return true;
        } finally {
            if (algorithm != null) {
                try {
                    algorithm.destroy();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private Map<String, List<QualityPresetOption>> getQualityPresetOptionsCache() {
        if (qualityPresetOptionsCache == null) {
            qualityPresetOptionsCache = new HashMap<>();
        }
        return qualityPresetOptionsCache;
    }

    private QualityPresetOption createCustomQualityPresetOption(float ratio) {
        return new QualityPresetOption(
                "custom",
                Text.translatable("superresolution.screen.text.custom").getString(),
                ratio,
                true
        );
    }

    private boolean isSameRatio(float left, float right) {
        return Math.abs(left - right) < 0.005f;
    }

    private void openLostResourceDialog(List<ExtraResource> resources) {
        MaterialDownloadList downloadList = MaterialDownloadList.create(
                new ExtraResources(resources),
                SuperResolutionConstants.NATIVE_LIBRARIES_DIR
        );
        downloadList.layout().setWidthPercent(100);

        MaterialDialog downloadDialog = MaterialDialog.create()
                .icon(MaterialSymbols.iconInfo())
                .headline(Text.translatable("superresolution.screen.config.dialog.download.title").getString())
                .supportingText(Text.translatable("superresolution.screen.config.dialog.download.description").getString())
                .content(downloadList)
                .addAction(Text.translatable("superresolution.screen.config.dialog.download.action.cancel").getString(), MaterialButtonVariant.Text, d -> {
                    downloadList.cancelDownload();
                })
                .addAction(Text.translatable("superresolution.screen.config.dialog.download.action.retry").getString(), MaterialButtonVariant.Text, d -> {
                    downloadList.retryDownload();
                })
                .addAction(Text.translatable("superresolution.screen.config.dialog.download.action.exit").getString(), MaterialButtonVariant.Text, d -> {
                    downloadList.cancelDownload();
                    d.dismiss();
                })
                .onDismiss(d -> {
                    downloadList.cancelDownload();
                });

        getView().showDialog(downloadDialog);
        downloadList.startDownload();
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
                        .setSaveConsumer((v) -> {
                            floatDesc.getSaveConsumer().accept(v.floatValue());
                            return true;
                        });
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

    private Frame createDebugFrame() {
        ScrollableFrame frame = createStandardScrollableFrame();
        ContainerWidget container = createStandardContainer();
        addFrameTitle(container, Text.translatable("superresolution.screen.config.section.debug"));
        OptionBuilder builder = createOptionBuilder(Text.translatable("superresolution.screen.config.category.debug"));
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
                        Text.translatable("superresolution.screen.config.options.label.enable_renderdoc"),
                        SuperResolutionConfig.isEnableRenderDoc())
                .setDefaultValue(() -> true)
                .setDescription(Text.translatable("superresolution.screen.config.options.tooltip.enable_renderdoc"))
                .setSaveConsumer(SuperResolutionConfig::setEnableRenderDoc)
                .build();
        builder.booleanOption(
                        Text.translatable("superresolution.screen.config.options.label.enable_imgui"),
                        SuperResolutionConfig.isEnableImgui())
                .setDefaultValue(() -> true)
                .setDescription(Text.translatable("superresolution.screen.config.options.tooltip.enable_imgui"))
                .setSaveConsumer(SuperResolutionConfig::setEnableImgui)
                .build();
        addOptionGroupToContainer(container, builder);
        finalizeFrame(frame, container);
        return frame;
    }

    private Frame createEnvironmentInfoFrame() {
        ScrollableFrame frame = createStandardScrollableFrame();
        ContainerWidget container = createStandardContainer();
        addFrameTitle(container, Text.translatable("superresolution.screen.config.section.environment"));

        MaterialLabel label = MaterialLabel.create()
                .text(Text.translatable("superresolution.screen.config.info.environment.base").getString())
                .fontSize(18)
                .color(MaterialScheme::secondary);
        label.layout().setMargin(YogaEdge.TOP, 8);
        label.layout().setMargin(YogaEdge.BOTTOM, 6);
        container.addChild(label);

        InfoCard envCard = new InfoCard();
        envCard.addChild(createInfoLine(Text.translatable("superresolution.screen.config.info.environment.mod_version").getString(), safeGetModVersion()));
        envCard.addChild(createInfoLine(Text.translatable("superresolution.screen.config.info.environment.native_version").getString(), safeGetNativeVersion()));
        envCard.addChild(createInfoLine(Text.translatable("superresolution.screen.config.info.environment.system").getString(), safeGetOperatingSystem()));
        container.addChild(envCard);
        MaterialLabel labelOGL = MaterialLabel.create()
                .text(Text.translatable("superresolution.screen.config.info.environment.opengl").getString())
                .fontSize(18)
                .color(MaterialScheme::secondary);
        labelOGL.layout().setMargin(YogaEdge.TOP, 8);
        labelOGL.layout().setMargin(YogaEdge.BOTTOM, 6);
        container.addChild(labelOGL);

        container.addChild(createGraphicsInfoCard(
                Text.translatable("superresolution.screen.config.info.environment.opengl").getString(),
                GraphicsCapabilities.getGLVersionString(),
                GraphicsCapabilities.getGLExtensions()
        ));
        MaterialLabel labelVK = MaterialLabel.create()
                .text(Text.translatable("superresolution.screen.config.info.environment.vulkan").getString())
                .fontSize(18)
                .color(MaterialScheme::secondary);
        labelVK.layout().setMargin(YogaEdge.TOP, 8);
        labelVK.layout().setMargin(YogaEdge.BOTTOM, 6);
        container.addChild(labelVK);

        container.addChild(createGraphicsInfoCard(
                Text.translatable("superresolution.screen.config.info.environment.vulkan").getString(),
                GraphicsCapabilities.getVulkanVersionString(),
                GraphicsCapabilities.getVulkanDeviceExtensions()
        ));

        finalizeFrame(frame, container);
        return frame;
    }

    private InfoCard createGraphicsInfoCard(String title, String version, Set<String> extensions) {
        InfoCard card = new InfoCard();
        card.addChild(createInfoLine(Text.translatable("superresolution.screen.config.info.environment.version").getString(), version));

        ContainerWidget extensionsContainer = new ContainerWidget();
        extensionsContainer.layout().setFlexDirection(YogaFlexDirection.COLUMN);
        extensionsContainer.layout().setWidthPercent(100);
        extensionsContainer.layout().setGap(YogaGutter.COLUMN, 2);
        extensionsContainer.layout().setPadding(YogaEdge.TOP, 4);

        MaterialLabel extTitle = MaterialLabel.create()
                .text(Text.translatable("superresolution.screen.config.info.environment.extensions").getString())
                .fontSize(14)
                .color(MaterialScheme::secondary);
        extensionsContainer.addChild(extTitle);

        if (extensions == null || extensions.isEmpty()) {
            MaterialLabel emptyLabel = MaterialLabel.create()
                    .text(Text.translatable("superresolution.screen.text.none").getString())
                    .fontSize(13)
                    .color(MaterialScheme::onSurfaceVariant);
            extensionsContainer.addChild(emptyLabel);
        } else {
            for (String extension : extensions) {
                MaterialLabel extLabel = MaterialLabel.create()
                        .text(extension)
                        .fontSize(12)
                        .color(MaterialScheme::onSurfaceVariant);
                extLabel.style().wrap(true);
                extLabel.layout().setWidthPercent(100);
                extensionsContainer.addChild(extLabel);
            }
        }
        card.addChild(extensionsContainer);

        return card;
    }

    private Frame createAboutInfoFrame() {
        ScrollableFrame frame = createStandardScrollableFrame();
        ContainerWidget container = createStandardContainer();
        addFrameTitle(container, Text.translatable("superresolution.screen.config.section.about"));

        MaterialLabel contributorSection = MaterialLabel.create()
                .text(Text.translatable("superresolution.screen.info.text.contributors").getString())
                .fontSize(18)
                .color(MaterialScheme::secondary);
        contributorSection.layout().setMargin(YogaEdge.BOTTOM, 6);
        container.addChild(contributorSection);

        InfoCard contributorsCard = new InfoCard();
        List<ContributorInfo> contributors = new ArrayList<>(List.of(
                new ContributorInfo("187J3X1", Text.translatable("superresolution.screen.config.info.about.contributor.187j3x1.desc").getString(), "https://github.com/187J3X1-114514", "/assets/super_resolution/textures/gui/contributors/114514.png"),
                new ContributorInfo("异世界美西螈", Text.translatable("superresolution.screen.config.info.about.contributor.ysjmxy.desc").getString(), "https://github.com/ysjmxy", "/assets/super_resolution/textures/gui/contributors/mxy.png"),
                new ContributorInfo("yu", Text.translatable("superresolution.screen.config.info.about.contributor.yu.desc").getString(), "https://github.com/yu234567", ""),
                new ContributorInfo("Enaium", Text.translatable("superresolution.screen.config.info.about.contributor.enaium.desc").getString(), "https://github.com/Enaium", "/assets/super_resolution/textures/gui/contributors/Enaium.png"),
                new ContributorInfo("rrtt217", Text.translatable("superresolution.screen.config.info.about.contributor.rrtt217.desc").getString(), "https://github.com/rrtt217", "/assets/super_resolution/textures/gui/contributors/rrtt217.png"),
                new ContributorInfo("筱烷", Text.translatable("superresolution.screen.config.info.about.contributor.shiroiame.desc").getString(), "https://github.com/Shiroiame-Kusu", "/assets/super_resolution/textures/gui/contributors/Shiroiame-Kusu.png"),
                new ContributorInfo("ChloePrime", Text.translatable("superresolution.screen.config.info.about.contributor.chloeprime.desc").getString(), "https://github.com/ChloePrime", ""),
                new ContributorInfo("EnderPhantomWing", Text.translatable("superresolution.screen.config.info.about.contributor.enderphantomwing.desc").getString(), "https://github.com/EnderPhantomWing", "/assets/super_resolution/textures/gui/contributors/EnderPhantomWing.png"),
                new ContributorInfo("索德列斯", Text.translatable("superresolution.screen.config.info.about.contributor.suodeliesi.desc").getString(), "", "/assets/super_resolution/textures/gui/contributors/suodeliesi.png"),
                new ContributorInfo("小狼_枫琪", Text.translatable("superresolution.screen.config.info.about.contributor.xiaolang.desc").getString(), "", "/assets/super_resolution/textures/gui/contributors/xiaolangfengqi.png"),
                new ContributorInfo("qwertyuiop", Text.translatable("superresolution.screen.config.info.about.contributor.qwertyuiop.desc").getString(), "https://github.com/moyongxin", "/assets/super_resolution/textures/gui/contributors/qwertyuiop.png"),
                new ContributorInfo("猫猫狐AR", Text.translatable("superresolution.screen.config.info.about.contributor.ar.desc").getString(), "https://github.com/Argon4W", "/assets/super_resolution/textures/gui/contributors/ar.png"),
                new ContributorInfo("辰蒙", Text.translatable("superresolution.screen.config.info.about.contributor.chenmeng.desc").getString(), "https://github.com/slmpc", "/assets/super_resolution/textures/gui/contributors/chenmeng.png")
        ));
        Collections.shuffle(contributors);
        for (ContributorInfo contributor : contributors) {
            contributorsCard.addChild(createContributorRow(contributor));
        }
        container.addChild(contributorsCard);

        MaterialLabel librarySection = MaterialLabel.create()
                .text(Text.translatable("superresolution.screen.config.info.about.libraries").getString())
                .fontSize(18)
                .color(MaterialScheme::secondary);
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
                new LibraryInfo("NVIDIA RTX DLSS SDK", "https://github.com/NVIDIA/DLSS")
        ));
        Collections.shuffle(libraries);
        for (LibraryInfo library : libraries) {
            librariesCard.addChild(createLibraryRow(library));
        }
        container.addChild(librariesCard);
        MaterialLabel legalSection = MaterialLabel.create()
                .text(Text.translatable("superresolution.screen.config.info.about.legal_notices").getString())
                .fontSize(18)
                .color(MaterialScheme::secondary);
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

        finalizeFrame(frame, container);
        return frame;
    }

    private ContainerWidget createInfoLine(String name, String value) {
        ContainerWidget row = new ContainerWidget();
        row.layout().setFlexDirection(YogaFlexDirection.COLUMN);
        row.layout().setWidthPercent(100);
        row.layout().setPadding(YogaEdge.VERTICAL, 4);

        MaterialLabel nameLabel = MaterialLabel.create()
                .text(name)
                .fontSize(14)
                .color(MaterialScheme::secondary);
        row.addChild(nameLabel);

        MaterialLabel valueLabel = MaterialLabel.create()
                .text(value)
                .fontSize(13)
                .color(MaterialScheme::onSurfaceVariant);
        valueLabel.style().wrap(true);
        valueLabel.layout().setWidthPercent(100);
        row.addChild(valueLabel);
        return row;
    }

    private ContainerWidget createContributorRow(ContributorInfo contributor) {
        ContainerWidget row = new ContainerWidget();
        row.layout().setFlexDirection(YogaFlexDirection.ROW);
        row.layout().setAlignItems(YogaAlign.CENTER);
        row.layout().setWidthPercent(100);
        row.layout().setPadding(YogaEdge.VERTICAL, 6);

        ContainerWidget left = new ContainerWidget();
        left.layout().setFlexDirection(YogaFlexDirection.ROW);
        left.layout().setAlignItems(YogaAlign.CENTER);
        left.layout().setFlexGrow(1f);
        left.layout().setGap(YogaGutter.COLUMN, 10);

        ContributorAvatar avatar = new ContributorAvatar(contributor/*MaterialSymbols.iconAccountCircle()*/);
        destroyables.add(avatar);
        left.addChild(avatar);

        ContainerWidget info = new ContainerWidget();
        info.layout().setFlexDirection(YogaFlexDirection.COLUMN);
        info.layout().setGap(YogaGutter.COLUMN, 2);
        info.layout().setFlexGrow(1f);

        MaterialLabel nameLabel = MaterialLabel.create()
                .text(contributor.name())
                .fontSize(14)
                .color(MaterialScheme::onSurface);
        info.addChild(nameLabel);

        MaterialLabel descLabel = MaterialLabel.create()
                .text(contributor.description())
                .fontSize(12)
                .color(MaterialScheme::onSurfaceVariant);
        descLabel.style().wrap(true);
        descLabel.layout().setWidthPercent(100);
        info.addChild(descLabel);

        left.addChild(info);
        row.addChild(left);

        MaterialButton openBtn = MaterialButton.textButton(Text.translatable("superresolution.screen.config.info.about.github").getString())
                .icon(MaterialSymbols.iconOpenInNew())
                .size(MaterialButtonSize.Small);
        boolean hasUrl = contributor.githubUrl() != null && !contributor.githubUrl().isBlank();
        openBtn.setDisabled(!hasUrl);
        openBtn.onClick(e -> openExternalLink(contributor.githubUrl()));
        row.addChild(openBtn);

        return row;
    }

    private ContainerWidget createLibraryRow(LibraryInfo library) {
        ContainerWidget row = new ContainerWidget();
        row.layout().setFlexDirection(YogaFlexDirection.ROW);
        row.layout().setAlignItems(YogaAlign.CENTER);
        row.layout().setWidthPercent(100);
        row.layout().setMinHeight(42);

        ContainerWidget info = new ContainerWidget();
        info.layout().setFlexDirection(YogaFlexDirection.COLUMN);
        info.layout().setGap(YogaGutter.COLUMN, 2);
        info.layout().setFlexGrow(1f);

        MaterialLabel nameLabel = MaterialLabel.create()
                .text(library.name())
                .fontSize(14)
                .color(MaterialScheme::onSurface);
        info.addChild(nameLabel);

        String urlText = (library.githubUrl() == null || library.githubUrl().isBlank())
                ? Text.translatable("superresolution.screen.config.info.about.github_todo").getString()
                : Component.translatable("superresolution.screen.config.info.about.github_prefix", library.githubUrl()).getString();
        MaterialLabel linkLabel = MaterialLabel.create()
                .text(urlText)
                .fontSize(11)
                .color(MaterialScheme::onSurfaceVariant);
        linkLabel.style().wrap(true);
        linkLabel.layout().setWidthPercent(100);
        info.addChild(linkLabel);

        row.addChild(info);

        MaterialButton openBtn = MaterialButton.textButton(Text.translatable("superresolution.screen.config.info.about.open").getString())
                .icon(MaterialSymbols.iconOpenInNew())
                .size(MaterialButtonSize.ExtraSmall);
        boolean hasUrl = library.githubUrl() != null && !library.githubUrl().isBlank();
        openBtn.setDisabled(!hasUrl);
        openBtn.onClick(e -> openExternalLink(library.githubUrl()));
        row.addChild(openBtn);

        return row;
    }

    private String safeGetModVersion() {
        try {
            if (Platform.currentPlatform == null) {
                return Text.translatable("superresolution.screen.config.info.unknown").getString();
            }
            return Platform.currentPlatform.getModVersionString(SuperResolution.MOD_ID);
        } catch (Throwable ignored) {
            return Text.translatable("superresolution.screen.config.info.unknown").getString();
        }
    }

    private String safeGetNativeVersion() {
        try {
            return SuperResolutionNative.getVersionInfo();
        } catch (Throwable ignored) {
            return Text.translatable("superresolution.screen.config.info.unavailable").getString();
        }
    }

    private String safeGetOperatingSystem() {
        try {
            if (Platform.currentPlatform == null) {
                return Text.translatable("superresolution.screen.config.info.unknown").getString();
            }
            return Platform.currentPlatform.getOS().getString();
        } catch (Throwable ignored) {
            return Text.translatable("superresolution.screen.config.info.unknown").getString();
        }
    }

    private void openExternalLink(String url) {
        if (url == null || url.isBlank()) {
            return;
        }
        try {
            try {
                String[] args;
                if (Platform.currentPlatform.getOS().type == OperatingSystemType.WINDOWS) {
                    args = new String[]{"rundll32", "url.dll,FileProtocolHandler", url};
                } else if (Platform.currentPlatform.getOS().type == OperatingSystemType.LINUX) {
                    args = new String[]{"xdg-open", url};
                } else {
                    return;
                }
                Runtime.getRuntime().exec(args);
            } catch (IOException privilegedactionexception) {
            }
        } catch (Exception ignored) {
        }
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

    private record QualityPresetOption(String codeName,

                                       String displayName,

                                       float upscaleRatio,

                                       boolean custom) {
    }

    private record ContributorInfo(String name,

                                   String description,

                                   String githubUrl,

                                   String avatar) {
    }

    private record LibraryInfo(String name,

                               String githubUrl) {
    }

    private static class InfoCard extends MaterialContainerWidget<InfoCard> {
        InfoCard() {

        }

        @Override
        protected void init() {
        }

        @Override
        public void layouting(RenderContext ctx) {
            getLayoutNode().setDebugName("InfoCard");
            layout().setFlexDirection(YogaFlexDirection.COLUMN);
            layout().setWidthPercent(100);
            layout().setPadding(YogaEdge.VERTICAL, 14);
            layout().setPadding(YogaEdge.HORIZONTAL, 20);
            layout().setGap(YogaGutter.COLUMN, 8);
        }

        @Override
        protected Rectangle getViewRegion() {
            return getBounds();
        }

        @Override
        protected void renderSelf(RenderContext ctx, UIInputState inputState) {
            Rectangle bounds = getBounds();
            ctx.roundedRect(
                    bounds.x,
                    bounds.y,
                    bounds.width,
                    bounds.height,
                    16,
                    scheme().surfaceContainerLow(),
                    true
            );
        }
    }

    private static class ContributorAvatar extends MaterialWidget<ContributorAvatar> {
        private ContributorInfo contributorInfo;
        private IImage guiImage;
        private ITexture rawTexture;
        private boolean loaded = false;

        ContributorAvatar(ContributorInfo contributorInfo) {
            setElementSize(36, 36);
            this.contributorInfo = contributorInfo;
        }

        @Override
        protected void init() {
        }

        @Override
        protected boolean isInteractive() {
            return false;
        }

        @Override
        public void render(RenderContext ctx, UIInputState inputState) {
            Rectangle bounds = getBounds();
            Vector2f center = bounds.getCenter();
            if (contributorInfo.avatar() != null) {
                if (!loaded) {
                    try (InputStream inputStream = getClass().getResourceAsStream(contributorInfo.avatar())) {
                        if (inputStream == null) {
                            loaded = true;
                            return;
                        }
                        rawTexture = ImageLoader.load(
                                RenderSystems.opengl().device(),
                                inputStream
                        );
                    } catch (Throwable ignored) {
                        ignored.printStackTrace();
                        loaded = true;
                        return;
                    }
                    if (rawTexture != null) {
                        guiImage = ctx.createImage(rawTexture);
                        loaded = true;
                    }
                }

                if (guiImage != null && rawTexture != null && loaded) {
                    IPaint paint = ctx.imagePattern(
                            bounds.x, bounds.y, 36, 36,
                            rawTexture.getWidth(), rawTexture.getHeight(), 0, 1.0f,
                            guiImage
                    );

                    ctx.beginPath();
                    ctx.paint(paint);
                    ctx.roundedRectComplex(
                            bounds.x,
                            bounds.y,
                            bounds.width,
                            bounds.height,
                            6f,
                            6f,
                            6f,
                            6f
                    );
                    ctx.endPath(true);
                    return;
                }
            }
            MaterialSymbols.iconAccountCircle().render(
                    ctx,
                    scheme().secondary(),
                    32,
                    center
            );
        }

        public void destroy() {
            if (rawTexture != null) {
                rawTexture.destroy();
            }
            if (guiImage != null) {
                guiImage.destroy();
            }
        }
    }
}
