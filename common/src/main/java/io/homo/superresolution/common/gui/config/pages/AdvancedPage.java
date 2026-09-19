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

import io.homo.superresolution.api.platform.OperatingSystemType;
import io.homo.superresolution.api.platform.Platform;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.config.enums.InternalTextureFormat;
import io.homo.superresolution.common.gui.impl.Text;
import io.homo.superresolution.common.gui.options.*;
import io.homo.superresolution.common.upscale.interoplayer.GlVulkanInteropAlgorithm;
import io.homo.superresolution.core.gui.core.ContainerWidget;
import io.homo.superresolution.core.gui.core.frame.Frame;
import io.homo.superresolution.core.gui.core.frame.ScrollableFrame;

import java.util.function.Consumer;

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

                    builder.booleanOption(
                                    Text.translatable("superresolution.screen.config.options.label.flip_vk_gl_interop_resources_y"),
                                    SuperResolutionConfig.isFlipVkGlInteropResourcesY())
                            .setDescription(Text.translatable("superresolution.screen.config.options.tooltip.flip_vk_gl_interop_resources_y"))
                            .setDefaultValue(() -> true)
                            .setSaveConsumer(value -> {
                                SuperResolutionConfig.setFlipVkGlInteropResourcesY(value);
                                if (SuperResolution.currentAlgorithm instanceof GlVulkanInteropAlgorithm) {
                                    SuperResolution.recreateAlgorithm();
                                }
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
