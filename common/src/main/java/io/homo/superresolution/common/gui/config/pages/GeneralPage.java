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

import io.homo.superresolution.api.QualityPreset;
import io.homo.superresolution.api.platform.OperatingSystemType;
import io.homo.superresolution.api.platform.Platform;
import io.homo.superresolution.api.registry.AlgorithmDescription;
import io.homo.superresolution.api.registry.AlgorithmRegistry;
import io.homo.superresolution.api.registry.BackendGroup;
import io.homo.superresolution.api.registry.LowLatencyGroups;
import io.homo.superresolution.api.registry.ExtraResource;
import io.homo.superresolution.api.registry.ExtraResources;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.config.enums.CaptureMode;
import io.homo.superresolution.common.config.enums.InternalTextureFormat;
import io.homo.superresolution.common.config.enums.InteropSyncMode;
import io.homo.superresolution.common.config.special.SpecialConfig;
import io.homo.superresolution.common.framegeneration.FrameGeneration;
import io.homo.superresolution.common.framegeneration.FrameGenerationDescriptions;
import io.homo.superresolution.api.registry.FrameGenerationDescription;
import io.homo.superresolution.api.registry.FrameGenerationRegistry;
import io.homo.superresolution.common.framegeneration.FrameGenerationMode;
import io.homo.superresolution.common.lowlatency.LowLatency;
import io.homo.superresolution.common.lowlatency.nv.NVIDIAReflexMode;
import io.homo.superresolution.api.registry.LowLatencyDescription;
import io.homo.superresolution.api.registry.LowLatencyRegistry;
import io.homo.superresolution.common.config.special.SpecialConfigDescription;
import io.homo.superresolution.common.gui.download.MaterialResourcesList;
import io.homo.superresolution.common.gui.impl.OptionRequirement;
import io.homo.superresolution.common.gui.impl.Text;
import io.homo.superresolution.common.gui.options.*;
import io.homo.superresolution.common.minecraft.B3DVulkanBridge;
import io.homo.superresolution.common.minecraft.MinecraftUtils;
import io.homo.superresolution.common.minecraft.MinecraftWindow;
import io.homo.superresolution.common.minecraft.handler.RenderHandlerManager;
import io.homo.superresolution.common.presentation.PresentationBackendManager;
import io.homo.superresolution.common.presentation.api.PresentationBackendType;
import io.homo.superresolution.common.perf.PerformanceTracker;
import io.homo.superresolution.common.upscale.AlgorithmDescriptions;
import io.homo.superresolution.common.upscale.interoplayer.GlVulkanInteropAlgorithm;
import io.homo.superresolution.common.workmode.SRWorkModeManager;
import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.SuperResolutionConstants;
import io.homo.superresolution.core.SuperResolutionNative;
import io.homo.superresolution.core.graphics.GraphicsCapabilities;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.gui.*;
import io.homo.superresolution.core.gui.core.ContainerWidget;
import io.homo.superresolution.core.gui.core.UIInputState;
import io.homo.superresolution.core.gui.core.animator.TimeInterpolator;
import io.homo.superresolution.core.gui.core.backends.interfaces.IImage;
import io.homo.superresolution.core.gui.core.backends.interfaces.IPaint;
import io.homo.superresolution.core.gui.core.backends.interfaces.TextAlign;
import io.homo.superresolution.core.gui.core.backends.interfaces.TextAlignType;
import io.homo.superresolution.core.gui.core.backends.render.RenderContext;
import io.homo.superresolution.core.gui.core.frame.Frame;
import io.homo.superresolution.core.gui.core.frame.ScrollableFrame;
import io.homo.superresolution.core.gui.core.impl.Rectangle;
import io.homo.superresolution.core.gui.core.impl.Tooltip;
import io.homo.superresolution.core.gui.widgets.MaterialContainerWidget;
import io.homo.superresolution.core.gui.widgets.MaterialWidget;
import io.homo.superresolution.core.gui.widgets.SpacerWidget;
import io.homo.superresolution.core.gui.widgets.button.MaterialButton;
import io.homo.superresolution.core.gui.widgets.button.MaterialButtonSize;
import io.homo.superresolution.core.gui.widgets.button.MaterialButtonVariant;
import io.homo.superresolution.core.gui.widgets.chart.MaterialChart;
import io.homo.superresolution.core.gui.widgets.chart.MaterialChartDataSeries;
import io.homo.superresolution.core.gui.widgets.chart.MaterialChartType;
import io.homo.superresolution.common.gui.widgets.SponsorChip;
import io.homo.superresolution.core.gui.widgets.dialog.MaterialDialog;
import io.homo.superresolution.core.gui.widgets.label.MaterialLabel;
import io.homo.superresolution.core.gui.widgets.navigation.drawer.MaterialNavigationDrawer;
import io.homo.superresolution.core.gui.widgets.progress.MaterialCircularProgressIndicator;
import io.homo.superresolution.core.gui.widgets.progress.MaterialProgressShape;
import io.homo.superresolution.core.impl.Destroyable;
import io.homo.superresolution.core.impl.Pair;
import io.homo.superresolution.core.utils.Color;
import io.homo.superresolution.core.utils.ImageLoader;
import io.homo.superresolution.core.utils.MouseCursor;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

import io.homo.superresolution.common.gui.SponsorService;
import io.homo.superresolution.common.gui.LogoRenderer;
import io.homo.superresolution.common.gui.config.pages.ConfigPageContext.QualityPresetOption;
import io.homo.superresolution.common.gui.config.pages.ConfigPageContext.TitlePill;
import io.homo.superresolution.common.gui.config.pages.ConfigPageContext.InfoCard;
import io.homo.superresolution.common.gui.config.pages.ConfigPageContext.ContributorInfo;
import io.homo.superresolution.common.gui.config.pages.ConfigPageContext.LibraryInfo;
import io.homo.superresolution.common.gui.config.pages.ConfigPageContext.SponsorWrappingRow;
import static io.homo.superresolution.common.gui.config.pages.ConfigPageContext.CURRENT_VERSION_SUPPORTS_VULKAN_PRESENTATION;

public final class GeneralPage implements ConfigPage {
    public static final GeneralPage INSTANCE = new GeneralPage();

    private GeneralPage() {
    }

    @Override
    public Frame create(ConfigPageContext context) {
        return build(context);
    }

    private static Frame build(ConfigPageContext context) {
        ScrollableFrame frame = context.createStandardScrollableFrame();
        ContainerWidget container = context.createStandardContainer();
        context.addFrameTitle(container, Text.translatable("superresolution.screen.config.section.general"));

        context.addLabeledOptionGroup(
                container,
                Text.translatable("superresolution.screen.config.category.super_resolution"),
                builder -> {
                    final SelectionListOptionEntry<QualityPresetOption>[] qualityPresetEntryRef = new SelectionListOptionEntry[1];
            final SelectionListOptionEntry[] algoSelectRef = new SelectionListOptionEntry[1];

            final NumberSliderOptionEntry[] upscaleRatioEntryRef = new NumberSliderOptionEntry[1];
            final boolean[] syncingQualityPreset = {false};

            builder.hintOption(Text.literal("b3d_vulkan_unavailable"))
                    .setIcon(MaterialSymbols.iconWarning())
                    .setTitle(Text.translatable("superresolution.screen.config.hint.b3d_vulkan_unavailable.title").getString())
                    .setText(Text.translatable("superresolution.screen.config.hint.b3d_vulkan_unavailable.text").getString())
                    .setDisplayRequirement(OptionRequirement.isTrue(B3DVulkanBridge::isB3DVulkanBackend))
                    .build();
            if (!CURRENT_VERSION_SUPPORTS_VULKAN_PRESENTATION) {
                builder.hintOption(Text.literal("vulkan_presentation_unavailable"))
                        .setIcon(MaterialSymbols.iconWarning())
                        .setTitle(Text.translatable("superresolution.screen.config.hint.vulkan_presentation_unavailable.title").getString())
                        .setText(Text.translatable("superresolution.screen.config.hint.vulkan_presentation_unavailable.text").getString()
                                .formatted(Platform.currentPlatform.getMinecraftVersion()))
                        .build();
            }
            builder.hintOption(Text.literal("tip114514"))
                    .setIcon(MaterialSymbols.iconWarning())
                    .setTitle(Text.translatable("superresolution.screen.config.hint.performance_warning.title").getString())
                    .setText(Text.translatable("superresolution.screen.config.hint.performance_warning.text").getString())
                    .setDisplayRequirement(OptionRequirement.isTrue(() -> !SRWorkModeManager.getCurrentState().shaderPackInUse()))
                    .build();
            builder.hintOption(Text.literal("frame_generation_only_warning"))
                    .setIcon(MaterialSymbols.iconWarning())
                    .setTitle(Text.translatable("superresolution.screen.config.hint.frame_generation_only_warning.title").getString())
                    .setText(Text.translatable("superresolution.screen.config.hint.frame_generation_only_warning.text").getString())
                    .setDisplayRequirement(OptionRequirement.isTrue(() ->
                            SRWorkModeManager.getCurrentState().supportsFrameGeneration()
                                    && AlgorithmDescriptions.NONE.equals(SuperResolutionConfig.getUpscaleAlgorithm())))
                    .build();
            builder.hintOption(Text.literal("shader_compat_warning"))
                    .setIcon(MaterialSymbols.iconWarning())
                    .setTitle(Text.translatable("superresolution.screen.config.hint.shader_compat_warning.title").getString())
                    .setText(Text.translatable(
                            SuperResolutionConfig.isUnstableIncompatibleShaderSupportEnabledAtStartup()
                                    ? "superresolution.screen.config.hint.shader_compat_warning.text.compat"
                                    : "superresolution.screen.config.hint.shader_compat_warning.text.disabled"
                    ).getString())
                    .setDisplayRequirement(OptionRequirement.isTrue(() ->
                            !SRWorkModeManager.isCurrentMode(SRWorkModeManager.SHADER_COMPAT) &&
                            SRWorkModeManager.getCurrentState().shaderPackInUse()
                    ))
                    .build();

            builder.booleanOption(
                            Text.translatable("superresolution.screen.config.options.label.enable_upscale"),
                            SuperResolutionConfig.isEnableUpscale())
                    .setDescription(Text.translatable("superresolution.screen.config.options.tooltip.enable_upscale"))
                    .setDefaultValue(() -> true)
                    .setEnableRequirement(SRWorkModeManager::hasAvailableWorkMode)
                    .setSaveConsumer(SuperResolutionConfig::setEnableUpscale)
                    .build();

            algoSelectRef[0] = builder.selectorOption(
                            Text.translatable("superresolution.screen.config.options.label.algo_type"),
                            SuperResolutionConfig.getUpscaleAlgorithm(),
                            AlgorithmRegistry.getAlgorithmMap().values().toArray())
                    .setValuesSupplier(() -> AlgorithmRegistry.getAlgorithmMap().values().stream()
                            .filter(algorithmDescription -> AlgorithmDescriptions.NONE.equals(algorithmDescription)
                                    || !SuperResolutionConfig.isAutoHideShaderpackDisabledAlgorithms()
                                    || !SRWorkModeManager.getCurrentState().disabledAlgorithms()
                                    .contains(algorithmDescription.getCodeName()))
                            .map(algorithmDescription -> (Object) algorithmDescription)
                            .toList())
                    .setNameProvider(algo -> ((AlgorithmDescription<?>) algo).getBriefName())
                    .setDefaultValue(SuperResolutionConfig::getDefaultAlgorithm)
                    .setSaveConsumer((obj) -> {
                        AlgorithmDescription<?> algo = (AlgorithmDescription<?>) obj;
                        List<ExtraResource> lostResources = algo.getExtraResources().checkAll(SuperResolutionConstants.NATIVE_LIBRARIES_DIR);
                        if (!lostResources.isEmpty()) {
                            context.openLostResourceDialog(lostResources);
                            return false;
                        }
                        if (!SuperResolutionConfig.setUpscaleAlgorithm(algo)) {
                            context.openCreateAlgorithmFailedDialog(algo);
                            algoSelectRef[0].setSelectedValue(SuperResolutionConfig.getUpscaleAlgorithm());
                        }
                        if (qualityPresetEntryRef[0] != null) {
                            qualityPresetEntryRef[0].refreshDynamicValues();
                            QualityPresetOption targetPreset = context.resolveQualityPresetOption(
                                    qualityPresetEntryRef[0].getValues(),
                                    SuperResolutionConfig.getUpscaleRatio()
                            );
                            qualityPresetEntryRef[0].setSelectedValue(targetPreset);

                            if (!context.isAlgorithmSupportsCustomUpscaleRatio(algo)
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
                        if (SRWorkModeManager.isCurrentMode(SRWorkModeManager.SHADER_COMPAT)) {
                            SRWorkModeManager.reloadShaderPack();
                        }
                        return true;
                    })
                    .setItemEnableRequirement((value) -> {
                        AlgorithmDescription<?> algorithmDescription = (AlgorithmDescription<?>) value;
                        return OptionRequirement.all(
                                () -> AlgorithmRegistry.isAlgorithmSupported(algorithmDescription),
                                () -> {
                                    if (context.isExperimentalAlgorithm(algorithmDescription)) return SuperResolutionConfig.isEnableExperimentalAlgorithms();
                                    return true;

                                },
                                () -> !SRWorkModeManager.getCurrentState().disabledAlgorithms().contains(algorithmDescription.getCodeName()),
                                () -> !AlgorithmDescriptions.NONE.equals(algorithmDescription)
                                        || SRWorkModeManager.getCurrentState().supportsFrameGeneration()
                        );
                    })
                    .setMenuItemTooltipSupplier((algo)->{
                        AlgorithmDescription<?> algorithmDescription = (AlgorithmDescription<?>) algo;
                        var result = algorithmDescription.getRequirement().check();
                        StringBuilder sb = new StringBuilder();
                        sb.append(algorithmDescription.getDisplayName());
                        if (SRWorkModeManager.getCurrentState().disabledAlgorithms().contains(algorithmDescription.getCodeName())) {
                            sb.append("\n");
                            sb.append(Text.translatable("superresolution.screen.config.options.tooltip.algo.disabled_by_shaderpack").getString());
                        }
                        if (AlgorithmDescriptions.NONE.equals(algorithmDescription)
                                && !SRWorkModeManager.getCurrentState().supportsFrameGeneration()) {
                            sb.append("\n");
                            sb.append(Text.translatable("superresolution.screen.config.options.tooltip.algo.none_requires_frame_generation_only").getString());
                        }
                        if (context.isExperimentalAlgorithm(algorithmDescription) && SuperResolutionConfig.isEnableExperimentalAlgorithms()){
                            sb.append("\n");
                            sb.append(Text.translatable("superresolution.screen.config.options.tooltip.algo.experimental_warning").getString());
                            if (!result.support()) sb.append("\n");
                        } else if(context.isExperimentalAlgorithm(algorithmDescription) && !SuperResolutionConfig.isEnableExperimentalAlgorithms()){
                            sb.append("\n");
                            sb.append(Text.translatable("superresolution.screen.config.options.tooltip.algo.experimental_disabled_hint").getString());
                            if (!result.support()) sb.append("\n");
                        }
                        if (!result.support()){
                            sb.append("\n");
                            sb.append(Text.translatable("superresolution.screen.config.options.tooltip.algo.unsupported_reason_header").getString());
                            if (!result.glVersionMet()){
                                sb.append("\n");
                                sb.append(Text.translatable("superresolution.screen.config.options.tooltip.algo.reason.opengl_version").getString());
                            }
                            if (!result.glExtensionsPresent()){
                                sb.append("\n");
                                sb.append(Text.translatable("superresolution.screen.config.options.tooltip.algo.reason.opengl_extension").getString());
                            }
                            if (!result.osSupported()){
                                sb.append("\n");
                                sb.append(Text.translatable("superresolution.screen.config.options.tooltip.algo.reason.os_unsupported").getString());
                            }
                            if (!result.vulkanAvailable()){
                                sb.append("\n");
                                sb.append(Text.translatable("superresolution.screen.config.options.tooltip.algo.reason.vulkan_unavailable").getString());
                                if (SuperResolutionConfig.isSkipInitVulkan()){
                                    sb.append(Text.translatable("superresolution.screen.config.options.tooltip.algo.reason.vulkan_skip_init_hint").getString());
                                }else {
                                    sb.append(Text.translatable("superresolution.screen.config.options.tooltip.algo.reason.vulkan_restart_hint").getString());
                                }
                            }
                            if (!result.vulkanVersionMet()){
                                sb.append("\n");
                                sb.append(Text.translatable("superresolution.screen.config.options.tooltip.algo.reason.vulkan_version").getString());
                            }
                            if (!result.vulkanDeviceExtensionsMet()){
                                sb.append("\n");
                                sb.append(Text.translatable("superresolution.screen.config.options.tooltip.algo.reason.vulkan_extension").getString());
                            }
                            if (!result.environmentValid()){
                                sb.append("\n");
                                sb.append(Text.translatable("superresolution.screen.config.options.tooltip.algo.reason.dev_env_only").getString());
                            }
                            if (!result.additionalConditionsMet()){
                                sb.append("\n");
                                sb.append(Text.translatable("superresolution.screen.config.options.tooltip.algo.reason.other").getString());
                            }
                        }
                        return Optional.of(Tooltip.withContext(sb.toString()));
                    })
                    .build();

            List<QualityPresetOption> initialPresetOptions = context.getQualityPresetOptions(SuperResolutionConfig.getUpscaleAlgorithm());
            QualityPresetOption initialPreset = context.resolveQualityPresetOption(
                    initialPresetOptions,
                    SuperResolutionConfig.getUpscaleRatio()
            );
            qualityPresetEntryRef[0] = builder.selectorOption(
                            Text.translatable("superresolution.screen.config.options.label.quality_preset"),
                            initialPreset,
                            initialPresetOptions.toArray(new QualityPresetOption[0]))
                    .setNameProvider(QualityPresetOption::displayName)
                    .setValuesSupplier(() -> context.getQualityPresetOptions(SuperResolutionConfig.getUpscaleAlgorithm()))
                    .setEnableRequirement(() -> !AlgorithmDescriptions.NONE.equals(SuperResolutionConfig.getUpscaleAlgorithm()))
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
                        if (SRWorkModeManager.isCurrentMode(SRWorkModeManager.SHADER_COMPAT)) {
                            SRWorkModeManager.reloadShaderPack();
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
                    .setValueFormater(v -> String.format(Locale.ROOT,"%.2f", v.doubleValue()))
                    .setDefaultValue(() -> 1.7)
                    .setDescriptionsSupplier(
                            (value -> Optional.of(
                                    new Text[]{
                                            Text.literal(
                                                    String.format(
                                                            Locale.ROOT,
                                                            Text.translatable("superresolution.screen.config.options.tooltip.upscale_ratio").getString(),
                                                            String.format(Locale.ROOT,"%.0f", RenderHandlerManager.getScreenWidth() / value.floatValue()),
                                                            String.format(Locale.ROOT,"%.0f", RenderHandlerManager.getScreenHeight() / value.floatValue()),
                                                            String.format(Locale.ROOT,"%.2f", ((1 / value.floatValue()) * 100)) + "%"
                                                    )
                                            )
                                    }
                            ))
                    )
                    .setEnableRequirement(() -> context.isAlgorithmSupportsCustomUpscaleRatio(SuperResolutionConfig.getUpscaleAlgorithm())
                            && !AlgorithmDescriptions.NONE.equals(SuperResolutionConfig.getUpscaleAlgorithm()))
                    .setTooltipSupplier((t)->{
                        if (AlgorithmDescriptions.NONE.equals(SuperResolutionConfig.getUpscaleAlgorithm())){
                            return Optional.of(Tooltip.withContext(Text.translatable("superresolution.screen.config.options.tooltip.upscale_ratio.frame_generation_only").getString()));
                        }
                        if (!context.isAlgorithmSupportsCustomUpscaleRatio(SuperResolutionConfig.getUpscaleAlgorithm())){
                            return Optional.of(Tooltip.withContext(Text.translatable("superresolution.screen.config.options.tooltip.upscale_ratio.custom_unsupported").getString()));
                        }else {
                            return Optional.of(Tooltip.empty());
                        }
                    })
                    .setSaveConsumer((value) -> {
                        float targetRatio = Float.parseFloat(String.format("%.2f", value.doubleValue()));
                        SuperResolutionConfig.setUpscaleRatio(targetRatio);
                        if (qualityPresetEntryRef[0] != null && !syncingQualityPreset[0]) {
                            QualityPresetOption targetPreset = context.resolveQualityPresetOption(
                                    qualityPresetEntryRef[0].getValues(),
                                    targetRatio
                            );
                            qualityPresetEntryRef[0].setSelectedValue(targetPreset);
                        }
                        if (SRWorkModeManager.isCurrentMode(SRWorkModeManager.SHADER_COMPAT)) {
                            SRWorkModeManager.reloadShaderPack();
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
                }
        );

        if (CURRENT_VERSION_SUPPORTS_VULKAN_PRESENTATION) {
            PresentationBackendType configuredPresentationBackend = SuperResolutionConfig.getPresentationBackend();
            PresentationBackendType displayedPresentationBackend =
                    configuredPresentationBackend == PresentationBackendType.VULKAN
                            ? PresentationBackendType.VULKAN
                            : PresentationBackendType.OPENGL;
            context.addLabeledOptionGroup(
                container,
                Text.translatable("superresolution.screen.config.category.presentation"),
                builder -> builder.selectorOption(
                                Text.translatable("superresolution.screen.config.options.label.presentation_backend"),
                                displayedPresentationBackend,
                                new PresentationBackendType[]{
                                        PresentationBackendType.OPENGL,
                                        PresentationBackendType.VULKAN
                                })
                        .setNameProvider(backend -> Text.translatable(
                                "superresolution.enum.presentationbackendtype." + backend.name().toLowerCase(Locale.ROOT)
                        ).getString())
                        .setDefaultValue(() -> PresentationBackendType.OPENGL)
                        .setRequireRestartGame(true)
                        .setDescription(Text.translatable(
                                "superresolution.screen.config.options.tooltip.presentation_backend"
                        ))
                        .setItemEnableRequirement(backend -> backend == PresentationBackendType.OPENGL
                                ? () -> true
                                : OptionRequirement.isFalse(SuperResolutionConfig::isSkipInitVulkan))
                        .setMenuItemTooltipSupplier(backend -> backend == PresentationBackendType.VULKAN
                                && SuperResolutionConfig.isSkipInitVulkan()
                                ? Optional.of(Tooltip.withContext(Text.translatable(
                                        "superresolution.screen.config.options.tooltip.presentation_backend.vulkan_unavailable"
                                ).getString()))
                                : Optional.empty())
                        .setTooltipSupplier(value -> Optional.of(Tooltip.withContext(
                                Text.translatable(
                                        value == PresentationBackendType.VULKAN
                                                && SuperResolutionConfig.isSkipInitVulkan()
                                                ? "superresolution.screen.config.options.tooltip.presentation_backend.vulkan_unavailable"
                                                : "superresolution.screen.config.options.tooltip.presentation_backend"
                                ).getString()
                        )))
                        .setSaveConsumer(SuperResolutionConfig::setPresentationBackend)
                        .build()
        );


        context.addLabeledOptionGroup(
                container,
                Text.translatable("superresolution.screen.config.category.low_latency"),
                builder -> {
                    BackendGroup currentGroup = context.lowLatencyGroupById(SuperResolutionConfig.getLowLatencyMode());
                    List<BackendGroup> groups = context.lowLatencyGroups();

                    builder.selectorOption(
                                    Text.translatable("superresolution.screen.config.options.label.low_latency_mode"),
                                    currentGroup,
                                    groups.toArray(new BackendGroup[0]))
                            .setDefaultValue(() -> LowLatencyGroups.NONE)
                            .setNameProvider(g -> g.getDisplayName().getString())
                            .setValuesSupplier(context::lowLatencyGroups)
                            .setDescription(Text.translatable("superresolution.screen.config.options.tooltip.low_latency_mode"))
                            .setEnableRequirement(() -> PresentationBackendManager.isVulkanPresentationRequested())
                            .setTooltipSupplier(value -> Optional.of(Tooltip.withContext(
                                    Text.translatable(
                                            PresentationBackendManager.isVulkanPresentationRequested()
                                                    ? "superresolution.screen.config.options.tooltip.low_latency_mode"
                                                    : "superresolution.screen.config.options.tooltip.low_latency_mode.vulkan_presentation_required"
                                    ).getString()
                            )))
                            .setItemEnableRequirement(context::getLowLatencyGroupItemRequirement)
                            .setSaveConsumer((Consumer<BackendGroup>) group -> {
                                SuperResolutionConfig.setLowLatencyMode(group.getId());
                                LowLatency.setMode(group.getId());
                                context.refreshFrameGenerationOptions();
                            })
                            .build();

                    for (LowLatencyDescription description : LowLatencyRegistry.getDescriptions().values()) {
                        for (SpecialConfigDescription<?> option : description.getOptionDescriptions()) {
                            context.buildSpecialConfigOption(
                                    builder,
                                    option,
                                    null,
                                    context.lowLatencyOptionDisplayRequirement(description),
                                    context::refreshFrameGenerationOptions
                            );
                        }
                    }
                }
        );

        if (context.hasAvailableFrameGenerationBackend()) {
            context.addLabeledOptionGroup(
                    container,
                    Text.translatable("superresolution.screen.config.category.frame_generation"), builder -> {
                        // Via FrameGeneration so its static initializer has populated the
                        // registry before the list below is read.
                        FrameGenerationDescription currentProvider = FrameGeneration.mode();
                        List<FrameGenerationDescription> providerEntries = context.frameGenerationProviderEntries();

                        builder.selectorOption(
                                        Text.translatable("superresolution.screen.config.options.label.frame_generation_provider"),
                                        currentProvider,
                                        providerEntries.toArray(new FrameGenerationDescription[0]))
                                .setDescription(Text.translatable("superresolution.screen.config.options.tooltip.frame_generation_provider"))
                                .setDefaultValue(() -> FrameGenerationRegistry.getDescriptionById(FrameGenerationDescriptions.AUTO_ID))
                                .setNameProvider(d -> d.getDisplayName().getString())
                                .setValuesSupplier(context::frameGenerationProviderEntries)
                                .setItemEnableRequirement(context::getFrameGenerationProviderItemRequirement)
                                .setSaveConsumer((Consumer<FrameGenerationDescription>) description ->
                                        SuperResolutionConfig.setFrameGenerationProvider(description.getId()))
                                .build();

                        FrameGenerationMode[] modes = FrameGeneration.availableModes();
                        context.frameGenerationEntry = builder.selectorOption(
                                        Text.translatable("superresolution.screen.config.options.frame_generation"),
                                        FrameGeneration.displayedMode(),
                                        modes
                                )
                                .setDefaultValue(() -> FrameGenerationMode.OFF)
                                .setNameProvider(mode -> Text.translatable(mode.translationKey()).getString())
                                .setDescription(Text.translatable("superresolution.screen.config.options.frame_generation.tooltip"))
                                .setEnableRequirement(FrameGeneration::isSupported)
                                .setValuesSupplier(() -> Arrays.asList(FrameGeneration.availableModes()))
                                .setSaveConsumer(FrameGeneration::setFrameGenerationMode)
                                .build();

                        for (FrameGenerationDescription description : FrameGenerationRegistry.getDescriptions().values()) {
                            for (SpecialConfigDescription<?> option : description.getOptionDescriptions()) {
                                context.buildSpecialConfigOption(
                                        builder,
                                        option,
                                        null,
                                        context.frameGenerationOptionDisplayRequirement(description),
                                        null
                                );
                            }
                        }
                    }
            );
        }
        }


        context.addLabeledOptionGroup(
                container,
                Text.translatable("superresolution.screen.config.category.other"),
                builder -> {
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
                    builder.booleanOption(
                                    Text.translatable("superresolution.screen.config.options.label.auto_hide_shaderpack_disabled_algorithms"),
                                    SuperResolutionConfig.isAutoHideShaderpackDisabledAlgorithms())
                            .setDefaultValue(() -> false)
                            .setSaveConsumer(SuperResolutionConfig::setAutoHideShaderpackDisabledAlgorithms)
                            .build();
                }
        );
        context.finalizeFrame(frame, container);
        return frame;
    }

}
