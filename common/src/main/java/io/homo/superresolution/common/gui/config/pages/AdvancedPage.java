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

public final class AdvancedPage implements ConfigPage {
    public static final AdvancedPage INSTANCE = new AdvancedPage();

    private AdvancedPage() {
    }

    @Override
    public Frame create(ConfigPageContext context) {
        return build(context);
    }

    private static Frame build(ConfigPageContext context) {
        ScrollableFrame frame = context.createStandardScrollableFrame();
        ContainerWidget container = context.createStandardContainer();
        context.addFrameTitle(container, Text.translatable("superresolution.screen.config.section.advanced"));

        context.addLabeledOptionGroup(
                container,
                Text.translatable("superresolution.screen.config.group.advanced.graphics_backend"),
                builder -> {
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

                    builder.enumSelectorOption(
                                    Text.translatable("superresolution.screen.config.options.label.interop_sync_mode"),
                                    InteropSyncMode.class,
                                    SuperResolutionConfig.getInteropSyncMode())
                            .setDescription(Text.translatable("superresolution.screen.config.options.tooltip.interop_sync_mode"))
                            .setDefaultValue(() -> InteropSyncMode.LowLatency)
                            .setEnumNameProvider(mode -> ((InteropSyncMode) mode).toString())
                            .setItemEnableRequirement(context::getInteropSyncModeItemRequirement)
                            .setSaveConsumer((value) -> {
                                SuperResolutionConfig.setInteropSyncMode(value);
                                if (SuperResolution.currentAlgorithm instanceof GlVulkanInteropAlgorithm) {
                                    SuperResolution.recreateAlgorithm();
                                }
                                context.refreshFrameGenerationOptions();
                            })
                            .build();

                    builder.enumSelectorOption(
                                    Text.translatable("superresolution.screen.config.options.label.internal_texture_format"),
                                    InternalTextureFormat.class,
                                    SuperResolutionConfig.INTERNAL_TEXTURE_FORMAT.get())
                            .setDescription(Text.translatable("superresolution.screen.config.options.tooltip.internal_texture_format"))
                            .setDefaultValue(() -> SuperResolutionConfig.INTERNAL_TEXTURE_FORMAT.getDefault())
                            .setEnumNameProvider(format -> format.name())
                            .setSaveConsumer(SuperResolutionConfig::setInternalTextureFormat)
                            .build();

                }
        );

        context.addLabeledOptionGroup(
                container,
                Text.translatable("superresolution.screen.config.group.advanced.shader_compatibility"),
                builder -> {
                    final BooleanSwitchOptionEntry[] entryRef = new BooleanSwitchOptionEntry[1];
                    entryRef[0] = builder.booleanOption(
                                    Text.translatable("superresolution.screen.config.options.label.enable_unstable_incompatible_shader_support"),
                                    SuperResolutionConfig.isEnableUnstableIncompatibleShaderSupport())
                            .setDescription(Text.translatable("superresolution.screen.config.options.tooltip.enable_unstable_incompatible_shader_support"))
                            .setDefaultValue(() -> false)
                            //.setRequireRestartGame(true)
                            .setSaveConsumer(value -> {
                                if (value) {
                                    context.openUnstableIncompatibleShaderSupportDialog(entryRef[0]);
                                    return false;
                                }
                                SuperResolutionConfig.setEnableUnstableIncompatibleShaderSupport(false);
                                return true;
                            })
                            .build();
                }
        );

        context.addLabeledOptionGroup(
                container,
                Text.translatable("superresolution.screen.config.group.advanced.diagnostics"),
                builder -> builder.booleanOption(
                                Text.translatable("superresolution.screen.config.options.label.enable_detailed_profiling"),
                                SuperResolutionConfig.isEnableDetailedProfiling())
                        .setDescription(Text.translatable("superresolution.screen.config.options.tooltip.enable_detailed_profiling"))
                        .setDefaultValue(() -> false)
                        .setSaveConsumer((Consumer<Boolean>) value -> {
                            SuperResolutionConfig.setEnableDetailedProfiling(value);
                            // The performance page decides which charts exist when it is
                            // built, and getOrCreateContentFrame caches every page for the
                            // life of the screen, so a page visited before this toggle
                            // would keep its old row set until the screen was reopened.
                            // Dropping it here makes the next visit rebuild. Switching
                            // away already detaches the frame from the view, so the
                            // replacement cannot end up double-attached.
                            context.invalidateContentFrame("performance");
                        })
                        .build()
        );

        if (Platform.currentPlatform.getOS().type == OperatingSystemType.WINDOWS) {
            context.addLabeledOptionGroup(
                    container,
                    Text.translatable("superresolution.screen.config.group.advanced.optiscaler"),
                    builder -> {
                        builder.booleanOption(
                                        Text.translatable("superresolution.screen.config.options.label.enable_optiscaler"),
                                        SuperResolutionConfig.isEnableOptiScaler())
                                .setDescription(Text.translatable("superresolution.screen.config.options.tooltip.enable_optiscaler"))
                                .setDefaultValue(() -> false)
                                .setRequireRestartGame(true)
                                .setSaveConsumer(SuperResolutionConfig::setEnableOptiScaler)
                                .build();

                        builder.fileSelectorOption(
                                        Text.translatable("superresolution.screen.config.options.label.optiscaler_dll"),
                                        SuperResolutionConfig.getOptiScalerDllPath())
                                .setDescription(Text.translatable("superresolution.screen.config.options.tooltip.optiscaler_dll"))
                                .setDialogTitle(Text.translatable("superresolution.screen.config.file.dialog.select_optiscaler_dll"))
                                .setFilterPatterns("*.dll")
                                .setFilterDescription(Text.translatable("superresolution.screen.config.file.filter.dll"))
                                .setDefaultValue(() -> "")
                                .setRequireRestartGame(true)
                                .setSaveConsumer(SuperResolutionConfig::setOptiScalerDllPath)
                                .build();
                    }
            );
        }

        context.finalizeFrame(frame, container);
        return frame;
    }

}
