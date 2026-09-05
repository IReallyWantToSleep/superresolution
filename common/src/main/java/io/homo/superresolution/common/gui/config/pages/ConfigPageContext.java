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
import io.homo.superresolution.core.gui.core.frame.ScrollableFrameWithScrollBar;
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

import io.homo.superresolution.common.gui.LogoRenderer;
import io.homo.superresolution.common.gui.SponsorService;
import io.homo.superresolution.core.gui.core.view.View;
import java.util.function.BooleanSupplier;
public class ConfigPageContext {
    public static final String ABOUT_MODRINTH_URL = "https://modrinth.com/mod/superresolution";
    public static final String ABOUT_GITHUB_URL = "https://github.com/187J3X1-114514/superresolution";
    public static final String ABOUT_WEBSITE_URL = "https://sr.187j3x1-114514.org/";
    public static final String ABOUT_WIKI_URL = "https://sr.187j3x1-114514.org/docs";
    public static final float FRAME_TITLE_PILL_FONT_SIZE = 24f * 0.8f;
    public static final float GROUP_TITLE_PILL_FONT_SIZE = 18f * 0.7f;
    public static final float FRAME_TITLE_PILL_MIN_HEIGHT = 40f;
    public static final float GROUP_TITLE_PILL_MIN_HEIGHT = 30f;
    public static final float FRAME_TITLE_PILL_HORIZONTAL_PADDING = 16f;
    public static final float GROUP_TITLE_PILL_HORIZONTAL_PADDING = 9f;
    #if MC_VER >= MC_1_21_11 && MC_VER < MC_26_2 || MC_VER >= MC_1_21 && MC_VER < MC_1_21_2 || MC_VER == MC_1_20_1 || MC_VER == MC_26_2
    public static final boolean CURRENT_VERSION_SUPPORTS_VULKAN_PRESENTATION = true;
    #else
    public static final boolean CURRENT_VERSION_SUPPORTS_VULKAN_PRESENTATION = false;
    #endif

    private final View view;
    private MaterialScheme materialScheme;
    private final Consumer<MaterialScheme> schemeSetter;
    private final Runnable restartRequiredCallback;
    private final Consumer<String> invalidatePageCallback;
    private final BooleanSupplier activeScreen;
    public Map<String, List<QualityPresetOption>> qualityPresetOptionsCache = new HashMap<>();
    public SelectionListOptionEntry<FrameGenerationMode> frameGenerationEntry;
    public final List<Destroyable> destroyables = new ArrayList<>();
    private boolean sponsorRequestStarted;
    private long sponsorRequestGeneration;
    private CompletableFuture<SponsorService.Result> sponsorRequest;

    public ConfigPageContext(View view, MaterialScheme materialScheme, Consumer<MaterialScheme> schemeSetter,
                             Runnable restartRequiredCallback, Consumer<String> invalidatePageCallback,
                             BooleanSupplier activeScreen) {
        this.view = view;
        this.materialScheme = materialScheme;
        this.schemeSetter = schemeSetter;
        this.restartRequiredCallback = restartRequiredCallback;
        this.invalidatePageCallback = invalidatePageCallback;
        this.activeScreen = activeScreen;
    }

    public View getView() {
        return view;
    }

    public MaterialScheme materialScheme() {
        return materialScheme;
    }

    public void setMaterialScheme(MaterialScheme scheme) {
        materialScheme = scheme;
        schemeSetter.accept(scheme);
    }

    public void invalidateContentFrame(String key) {
        invalidatePageCallback.accept(key);
    }

    public void destroy() {
        sponsorRequestGeneration++;
        if (sponsorRequest != null) {
            sponsorRequest.cancel(true);
        }
        destroyables.forEach(Destroyable::destroy);
    }




    public void openUnstableIncompatibleShaderSupportDialog(BooleanSwitchOptionEntry entry) {
        MaterialDialog dialog = MaterialDialog.create()
                .icon(MaterialSymbols.iconWarning())
                .scrimDismiss(false)
                .headline(Text.translatable("superresolution.screen.config.dialog.unstable_incompatible_shader_support.title").getString())
                .supportingText(Text.translatable("superresolution.screen.config.dialog.unstable_incompatible_shader_support.message").getString())
                .addAction(Text.translatable("superresolution.screen.config.dialog.unstable_incompatible_shader_support.action.cancel").getString(), MaterialButtonVariant.Filled, dialog1->{
                    SuperResolutionConfig.setEnableUnstableIncompatibleShaderSupport(false);
                    SuperResolutionConfig.SPEC.save();
                    entry.setCurrentValue(false);
                    dialog1.dismiss();
                })
                .addAction(Text.translatable("superresolution.screen.config.dialog.unstable_incompatible_shader_support.action.confirm").getString(), MaterialButtonVariant.Text, dialog1 -> {
                    SuperResolutionConfig.setEnableUnstableIncompatibleShaderSupport(true);
                    SuperResolutionConfig.SPEC.save();
                    entry.setCurrentValue(true);
                    dialog1.dismiss();
                });
        getView().showDialog(dialog);
    }

    public void openCreateAlgorithmFailedDialog(AlgorithmDescription<?> description) {
        MaterialDialog dialog = MaterialDialog.create()
                .icon(MaterialSymbols.iconError())
                .headline(Text.translatable("superresolution.screen.config.dialog.create_algorithm_failed.title").getString())
                .supportingText(Text.translatable("superresolution.screen.config.dialog.create_algorithm_failed.message").getString().formatted(description.displayName))
                .addAction(Text.translatable("superresolution.screen.config.dialog.create_algorithm_failed.action.confirm").getString(), MaterialButtonVariant.Tonal, MaterialDialog::dismiss);
        getView().showDialog(dialog);
    }

    public void openRestartRequiredDialog() {
        MaterialDialog dialog = MaterialDialog.create()
                .icon(MaterialSymbols.iconRestartAlt())
                .headline(Text.translatable("superresolution.screen.config.dialog.restart_required.title").getString())
                .supportingText(Text.translatable("superresolution.screen.config.dialog.restart_required.message").getString())
                .addAction(Text.translatable("superresolution.screen.config.dialog.restart_required.action.confirm").getString(), MaterialButtonVariant.Tonal, MaterialDialog::dismiss);
        getView().showDialog(dialog);
    }

    public boolean isExperimentalAlgorithm(AlgorithmDescription<?> algorithmDescription){
        return algorithmDescription.equals(AlgorithmDescriptions.FSR4_D3D12) ||
                algorithmDescription.equals(AlgorithmDescriptions.ANIME4K);
    }

    /**
     * The selectable low latency entries: the "none" sentinel plus every group that at least
     * one registered backend belongs to. Concrete backends are never listed; the negotiator
     * picks one inside the selected group at runtime.
     */
    public List<BackendGroup> lowLatencyGroups() {
        List<BackendGroup> groups = new ArrayList<>();
        groups.add(LowLatencyGroups.NONE);
        for (LowLatencyDescription description : LowLatencyRegistry.getDescriptions().values()) {
            BackendGroup group = description.getGroup();
            if (group != null && !groups.contains(group)) {
                groups.add(group);
            }
        }
        return groups;
    }

    public BackendGroup lowLatencyGroupById(String id) {
        for (BackendGroup group : lowLatencyGroups()) {
            if (group.getId().equals(id)) {
                return group;
            }
        }
        return LowLatencyGroups.NONE;
    }

    public OptionRequirement lowLatencyOptionDisplayRequirement(LowLatencyDescription description) {
        return () -> SuperResolutionConfig.getLowLatencyMode().equals(description.getId())
                || FrameGeneration.activeLowLatencyBackendId().equals(description.getId());
    }

    public OptionRequirement frameGenerationOptionDisplayRequirement(FrameGenerationDescription description) {
        return () -> FrameGeneration.isFrameGenerationEnabled()
                && (SuperResolutionConfig.getFrameGenerationProvider().equals(description.getId())
                || FrameGeneration.activeId().equals(description.getId()));
    }

    public OptionRequirement getLowLatencyGroupItemRequirement(BackendGroup group) {
        if (group == null) {
            return OptionRequirement.all();
        }
        if (group.equals(LowLatencyGroups.NONE)) {
            return () -> !FrameGeneration.isFrameGenerationEnabled();
        }
        return () -> LowLatency.isAvailable() && lowLatencyGroupHasUsableBackend(group);
    }

    public boolean lowLatencyGroupHasUsableBackend(BackendGroup group) {
        for (LowLatencyDescription description : LowLatencyRegistry.getDescriptions().values()) {
            if (group.equals(description.getGroup())
                    && LowLatencyRegistry.isSupported(description)
                    && description.isAvailable()
                    && description.dependenciesSatisfied()) {
                return true;
            }
        }
        return false;
    }

    public boolean isReflexConfigured() {
        return "superresolution:nv_reflex".equals(SuperResolutionConfig.getLowLatencyMode())
                && SuperResolutionConfig.getNVIDIAReflexMode() != NVIDIAReflexMode.OFF;
    }

    /**
     * Frame generation entries shown to the user: the automatic entry plus one entry per
     * algorithm group. Concrete backends registered inside a group stay hidden.
     */
    public List<FrameGenerationDescription> frameGenerationProviderEntries() {
        List<FrameGenerationDescription> entries = new ArrayList<>();
        for (FrameGenerationDescription description : FrameGenerationRegistry.getDescriptions().values()) {
            if (description.isAutomatic()
                    && (description.getGroup() == null || frameGenerationGroupHasUsableBackend(description.getGroup()))) {
                entries.add(description);
            }
        }
        return entries;
    }

    public OptionRequirement getFrameGenerationProviderItemRequirement(FrameGenerationDescription description) {
        if (description == null) {
            return OptionRequirement.all();
        }
        BackendGroup group = description.getGroup();
        // The "any group" entry is always selectable; it resolves to whatever came up.
        if (group == null) {
            return OptionRequirement.all();
        }
        return () -> frameGenerationGroupHasUsableBackend(group);
    }

    public boolean frameGenerationGroupHasUsableBackend(BackendGroup group) {
        for (FrameGenerationDescription description : FrameGenerationRegistry.getDescriptions().values()) {
            if (!description.isAutomatic()
                    && group.equals(description.getGroup())
                    && description.getRequirement().check().support()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasAvailableFrameGenerationBackend() {
        FrameGeneration.mode();
        return FrameGenerationRegistry.getDescriptions().values().stream()
                .anyMatch(description -> !description.isAutomatic()
                        && FrameGenerationRegistry.isSupported(description));
    }

    public OptionRequirement getInteropSyncModeItemRequirement(InteropSyncMode mode) {
        if (mode == InteropSyncMode.HighPerformance) {
            return () -> !isReflexConfigured();
        }
        return OptionRequirement.all();
    }

    public void refreshFrameGenerationOptions() {
        if (frameGenerationEntry == null) {
            return;
        }
        frameGenerationEntry.refreshDynamicValues();
        frameGenerationEntry.setSelectedValue(FrameGeneration.displayedMode());
    }


    public List<QualityPresetOption> getQualityPresetOptions(AlgorithmDescription<?> algorithmDescription) {
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

    public List<QualityPreset> getAlgorithmQualityPresets(AlgorithmDescription<?> algorithmDescription) {
        if (algorithmDescription == null) {
            return List.of();
        }
        return new ArrayList<>(algorithmDescription.getQualityPresets());
    }

    public QualityPresetOption resolveQualityPresetOption(List<QualityPresetOption> options, float ratio) {
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

    public boolean isAlgorithmSupportsCustomUpscaleRatio(AlgorithmDescription<?> algorithmDescription) {
        if (algorithmDescription == null) {
            return true;
        }
        return algorithmDescription.isCustomUpscaleRatio();
    }

    public Map<String, List<QualityPresetOption>> getQualityPresetOptionsCache() {
        if (qualityPresetOptionsCache == null) {
            qualityPresetOptionsCache = new HashMap<>();
        }
        return qualityPresetOptionsCache;
    }

    public QualityPresetOption createCustomQualityPresetOption(float ratio) {
        return new QualityPresetOption(
                "custom",
                Text.translatable("superresolution.screen.text.custom").getString(),
                ratio,
                true
        );
    }

    public boolean isSameRatio(float left, float right) {
        return Math.abs(left - right) < 0.005f;
    }

    public Pair<MaterialResourcesList, MaterialDialog> createLocalResourceSelector(List<ExtraResource> resources) {
        MaterialResourcesList resourcesList = MaterialResourcesList.createFileChoose(
                new ExtraResources(resources),
                SuperResolutionConstants.NATIVE_LIBRARIES_DIR
        );
        resourcesList.layout().setWidthPercent(100);

        MaterialDialog dialog = MaterialDialog.create()
                .icon(MaterialSymbols.iconInfo())
                .headline(Text.translatable("superresolution.screen.config.dialog.local_resource.title").getString())
                .content(resourcesList)
                .supportingText(Text.translatable("superresolution.screen.config.dialog.local_resource.description").getString());

        dialog.style().minWidth(400f);
        dialog.style().maxWidth(700f);
        dialog.scrimDismiss(false);

        #if ENABLE_AUTO_DOWNLOAD == 1
        dialog.addAction(
                Text.translatable("superresolution.screen.config.dialog.download.action.auto_download").getString(),
                MaterialButtonVariant.Filled,
                d -> {
                    d.dismiss();
                    d.onDismiss(foo -> {
                        Pair<MaterialResourcesList, MaterialDialog> selector = createOnlineResourceSelector(resources);
                        getView().showDialog(selector.right());
                    });
                }
        );
        #endif

        if (Platform.currentPlatform.getOS().type.equals(OperatingSystemType.WINDOWS)) {
            dialog.addAction(
                    Text.translatable("superresolution.screen.config.dialog.local_resource.action.download_dlss_windows").getString(),
                    MaterialButtonVariant.Outlined,
                    d -> openExternalLink("https://raw.githubusercontent.com/NVIDIA/DLSS/refs/heads/main/lib/Windows_x86_64/rel/nvngx_dlss.dll")
            );

            dialog.addAction(
                    Text.translatable("superresolution.screen.config.dialog.local_resource.action.download_xess_windows").getString(),
                    MaterialButtonVariant.Outlined,
                    d -> openExternalLink("https://raw.githubusercontent.com/intel/xess/refs/heads/main/bin/libxess.dll")
            );
        }

        dialog.addAction(
                Text.translatable("superresolution.screen.config.dialog.local_resource.action.done").getString(),
                MaterialButtonVariant.Text,
                MaterialDialog::dismiss
        );

        return Pair.of(resourcesList, dialog);
    }

    public Pair<MaterialResourcesList, MaterialDialog> createOnlineResourceSelector(List<ExtraResource> resources) {
        MaterialResourcesList downloadList = MaterialResourcesList.createDownload(
                new ExtraResources(resources),
                SuperResolutionConstants.NATIVE_LIBRARIES_DIR
        );
        downloadList.layout().setWidthPercent(100);

        MaterialDialog downloadDialog = MaterialDialog.create()
                .icon(MaterialSymbols.iconInfo())
                .headline(Text.translatable("superresolution.screen.config.dialog.download.title").getString())
                .supportingText(Text.translatable("superresolution.screen.config.dialog.download.description").getString())
                .content(downloadList);

        downloadDialog.style().minWidth(400f);
        downloadDialog.style().maxWidth(700f);
        downloadDialog.scrimDismiss(false);

        downloadDialog.addAction(
                Text.translatable("superresolution.screen.config.dialog.download.action.manual_select").getString(),
                MaterialButtonVariant.Filled,
                d -> {
                    d.dismiss();
                    d.onDismiss(foo -> {
                        downloadList.cancelDownload();
                        Pair<MaterialResourcesList, MaterialDialog> selector = createLocalResourceSelector(resources);
                        getView().showDialog(selector.right());
                    });
                }
        );

        downloadDialog.addAction(
                Text.translatable("superresolution.screen.config.dialog.download.action.cancel").getString(),
                MaterialButtonVariant.Tonal,
                d -> downloadList.cancelDownload()
        );

        downloadDialog.addAction(
                Text.translatable("superresolution.screen.config.dialog.download.action.retry").getString(),
                MaterialButtonVariant.Tonal,
                d -> downloadList.retryDownload()
        );

        downloadDialog.addAction(
                Text.translatable("superresolution.screen.config.dialog.download.action.exit").getString(),
                MaterialButtonVariant.Text,
                d -> {
                    downloadList.cancelDownload();
                    d.dismiss();
                }
        );

        downloadDialog.onDismiss(d -> downloadList.cancelDownload());

        return Pair.of(downloadList, downloadDialog);
    }

    public void openLostResourceDialog(List<ExtraResource> resources) {
        #if ENABLE_AUTO_DOWNLOAD == 1
        Pair<MaterialResourcesList,MaterialDialog> selector = createOnlineResourceSelector(resources);
        getView().showDialog(selector.right());
        #else
        Pair<MaterialResourcesList,MaterialDialog> selector = createLocalResourceSelector(resources);
        getView().showDialog(selector.right());

        #endif
    }



    public ScrollableFrame createStandardScrollableFrame() {
        ScrollableFrame frame = new ScrollableFrameWithScrollBar();
        frame.setContentPadding(20, 0, 20, 0);
        frame.setVerticalScrollEnabled(true);
        frame.setHorizontalScrollEnabled(false);
        return frame;
    }

    public ContainerWidget createStandardContainer() {
        ContainerWidget container = new ContainerWidget();
        container.layout().setFlexDirection(YogaFlexDirection.COLUMN);
        container.layout().setWidthPercent(100);
        container.layout().setGap(YogaGutter.COLUMN, 15);
        container.layout().setAlignItems(YogaAlign.FLEX_START);
        return container;
    }

    public void addFrameTitle(ContainerWidget container, Text title) {
        container.addChild(SpacerWidget.vertical(20f));
        TitlePill titlePill = createTitlePill(
                title.getString(),
                FRAME_TITLE_PILL_FONT_SIZE,
                FRAME_TITLE_PILL_MIN_HEIGHT,
                FRAME_TITLE_PILL_HORIZONTAL_PADDING,
                12
        );
        titlePill.layout().setMargin(YogaEdge.BOTTOM, 20);
        container.addChild(titlePill);
    }

    public OptionBuilder createOptionBuilder(Text categoryName) {
        OptionCategory category = new OptionCategory(categoryName);
        OptionBuilder builder = new OptionBuilder(category);
        builder.setSaveRunnable(SuperResolutionConfig.SPEC::save);
        builder.setRestartRequiredCallback(restartRequiredCallback);
        return builder;
    }

    public void addOptionGroupToContainer(ContainerWidget container, OptionBuilder builder) {
        OptionBuilder.OptionsContainer optionsContainer = builder.build();
        optionsContainer.layout().setWidthPercent(100);
        container.addChild(optionsContainer);
    }

    public void addLabeledOptionGroup(ContainerWidget container, Text groupLabel, Consumer<OptionBuilder> configurator) {
        TitlePill groupPill = createTitlePill(
                groupLabel.getString(),
                GROUP_TITLE_PILL_FONT_SIZE,
                GROUP_TITLE_PILL_MIN_HEIGHT,
                GROUP_TITLE_PILL_HORIZONTAL_PADDING,
                -1
        );
        groupPill.layout().setMargin(YogaEdge.TOP, 8);
        groupPill.layout().setMargin(YogaEdge.BOTTOM, 3);
        container.addChild(groupPill);

        OptionBuilder builder = createOptionBuilder(groupLabel);
        configurator.accept(builder);
        addOptionGroupToContainer(container, builder);
    }

    public TitlePill createSectionPill(String text) {
        return createTitlePill(
                text,
                GROUP_TITLE_PILL_FONT_SIZE,
                GROUP_TITLE_PILL_MIN_HEIGHT,
                GROUP_TITLE_PILL_HORIZONTAL_PADDING,
                -1
        );
    }

    public TitlePill createTitlePill(
            String text,
            float fontSize,
            float minHeight,
            float horizontalPadding,
            float radius
    ) {
        return new TitlePill(text, fontSize, minHeight, horizontalPadding, radius);
    }

    public void finalizeFrame(ScrollableFrame frame, ContainerWidget container) {
        container.addChild(SpacerWidget.vertical(20f));
        frame.setRoot(container);
    }


    public void buildSpecialConfigOption(OptionBuilder builder, SpecialConfigDescription<?> desc) {
        buildSpecialConfigOption(builder, desc, null, null, null);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void buildSpecialConfigOption(
            OptionBuilder builder,
            SpecialConfigDescription<?> desc,
            @Nullable OptionRequirement enableRequirement,
            @Nullable OptionRequirement displayRequirement,
            @Nullable Runnable afterSave
    ) {
        Text optionName = Text.literal(desc.getName().getString());
        Optional<Component> tooltip = desc.getTooltip();

        switch (desc.getType()) {
            case BOOLEAN: {
                SpecialConfigDescription<Boolean> boolDesc = (SpecialConfigDescription<Boolean>) desc;
                var opt = builder.booleanOption(optionName, boolDesc.getValue())
                        .setDefaultValue(() -> boolDesc.getDefaultValue())
                        .setSaveConsumer(value -> {
                            boolDesc.getSaveConsumer().accept(value);
                            runAfterSave(afterSave);
                        });
                if (tooltip.isPresent()) {
                    opt.setDescription(Text.literal(tooltip.get().getString()));
                }
                if (enableRequirement != null) {
                    opt.setEnableRequirement(enableRequirement);
                }
                if (displayRequirement != null) {
                    opt.setDisplayRequirement(displayRequirement);
                }
                opt.setRequireRestartGame(boolDesc.isRequiresRestartGame());
                opt.build();
                break;
            }
            case ENUM: {
                SpecialConfigDescription enumDesc = (SpecialConfigDescription) desc;
                Class enumClass = enumDesc.getClazz();
                Enum enumValue = (Enum) enumDesc.getValue();
                Enum defaultEnumValue = (Enum) enumDesc.getDefaultValue();
                Consumer<Object> enumSaveConsumer = value -> {
                    enumDesc.getSaveConsumerAsObject().accept(value);
                    runAfterSave(afterSave);
                };
                EnumSelectorBuilder<?> opt = (EnumSelectorBuilder<?>) builder.enumSelectorOption(optionName, enumClass, enumValue)
                        .setDefaultValue(defaultEnumValue)
                        .setSaveConsumer(enumSaveConsumer);
                if (enumDesc.isValueNameIsSupplier()) {
                    opt.setEnumNameProvider(e ->
                            ((Function<Object, Optional<Component>>) enumDesc.getValueNameSupplierAsObject())
                                    .apply(e).orElse(Component.empty()).getString()
                    );
                }
                opt.setItemEnableRequirement(item ->
                        () -> enumDesc.getItemEnableRequirementAsObject().test(item));
                if (tooltip.isPresent()) {
                    opt.setDescription(Text.literal(tooltip.get().getString()));
                }
                if (enableRequirement != null) {
                    opt.setEnableRequirement(enableRequirement);
                }
                if (displayRequirement != null) {
                    opt.setDisplayRequirement(displayRequirement);
                }
                opt.setRequireRestartGame(enumDesc.isRequiresRestartGame());
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
                            runAfterSave(afterSave);
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
                if (enableRequirement != null) {
                    opt.setEnableRequirement(enableRequirement);
                }
                if (displayRequirement != null) {
                    opt.setDisplayRequirement(displayRequirement);
                }
                opt.setRequireRestartGame(floatDesc.isRequiresRestartGame());
                opt.build();
                break;
            }
            default:
                break;
        }
    }

    private static void runAfterSave(@Nullable Runnable afterSave) {
        if (afterSave != null) {
            afterSave.run();
        }
    }




    public InfoCard createGraphicsInfoCard(String title, String version, Set<String> extensions) {
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


    public MaterialLabel createSponsorStateLabel(String key) {
        MaterialLabel label = MaterialLabel.create()
                .text(Text.translatable(key).getString())
                .fontSize(13)
                .color(MaterialScheme::onSurfaceVariant);
        label.style().sizeToContent(true);
        return label;
    }

    public void loadSponsors(ContainerWidget container) {
        if (sponsorRequestStarted) {
            return;
        }
        sponsorRequestStarted = true;
        long generation = ++sponsorRequestGeneration;
        sponsorRequest = SponsorService.fetchAsync();
        sponsorRequest.thenAccept(result -> Minecraft.getInstance().execute(() -> {
            if (generation != sponsorRequestGeneration || !activeScreen.getAsBoolean()) {
                return;
            }
            if (!result.success()) {
                showSponsorErrorState(container);
            } else if (result.sponsors().isEmpty()) {
                showSponsorMessageState(container, "superresolution.screen.config.info.about.sponsors.empty");
            } else {
                for (var child : new ArrayList<>(container.getChildren())) {
                    container.removeChild(child);
                }
                container.layout().setFlexDirection(YogaFlexDirection.ROW);
                container.layout().setWrap(YogaWrap.WRAP);
                container.layout().setGap(YogaGutter.ALL, 8);
                container.layout().setAlignItems(YogaAlign.CENTER);
                container.layout().setJustifyContent(YogaJustify.SPACE_BETWEEN);
                for (SponsorService.Sponsor sponsor : result.sponsors()) {
                    container.addChild(new SponsorChip(sponsor));
                }
                view.markLayoutDirty();
            }
        }));
    }

    public void applySponsorMessageStateLayout(ContainerWidget container) {
        for (var child : new ArrayList<>(container.getChildren())) {
            container.removeChild(child);
        }
        container.layout().setFlexDirection(YogaFlexDirection.COLUMN);
        container.layout().setWrap(YogaWrap.NO_WRAP);
        container.layout().setGap(YogaGutter.ALL, 8);
        container.layout().setAlignItems(YogaAlign.CENTER);
        container.layout().setJustifyContent(YogaJustify.CENTER);
    }

    public void showSponsorLoadingState(ContainerWidget container) {
        applySponsorMessageStateLayout(container);
        MaterialCircularProgressIndicator indicator = new MaterialCircularProgressIndicator()
                .setIndeterminate(true)
                .setShape(MaterialProgressShape.FLAT);
        indicator.setElementWidth(MaterialCircularProgressIndicator.SIZE_FLAT_DEFAULT);
        indicator.setElementHeight(MaterialCircularProgressIndicator.SIZE_FLAT_DEFAULT);
        container.addChild(indicator);
        container.addChild(createSponsorStateLabel("superresolution.screen.config.info.about.sponsors.loading"));
        view.markLayoutDirty();
    }

    public void showSponsorMessageState(ContainerWidget container, String key) {
        applySponsorMessageStateLayout(container);
        container.addChild(createSponsorStateLabel(key));
        view.markLayoutDirty();
    }

    public void showSponsorErrorState(ContainerWidget container) {
        applySponsorMessageStateLayout(container);
        container.addChild(createSponsorStateLabel("superresolution.screen.config.info.about.sponsors.error"));
        MaterialButton retryButton = MaterialButton.tonal(
                        Text.translatable("superresolution.screen.config.info.about.sponsors.retry").getString())
                .icon(MaterialSymbols.iconRefresh())
                .size(MaterialButtonSize.Small);
        retryButton.onClick(e -> {
            sponsorRequestStarted = false;
            showSponsorLoadingState(container);
            loadSponsors(container);
        });
        container.addChild(retryButton);
        view.markLayoutDirty();
    }

    public static class SponsorWrappingRow extends ContainerWidget {
        private static final float CHIP_GAP = 8f;

        @Override
        public void layouting(RenderContext ctx) {
            super.layouting(ctx);
            if (layout().getFlexDirection() != YogaFlexDirection.ROW) {
                return;
            }
            int lastLine = -1;
            for (var child : getChildren()) {
                lastLine = Math.max(lastLine, child.getLayoutNode().getLineIndex());
            }
            if (lastLine < 0) {
                return;
            }
            float cursor = Float.POSITIVE_INFINITY;
            for (var child : getChildren()) {
                var node = child.getLayoutNode();
                if (node.getLineIndex() == lastLine) {
                    cursor = Math.min(cursor, node.getLayoutX());
                }
            }
            if (!Float.isFinite(cursor)) {
                return;
            }
            for (var child : getChildren()) {
                var node = child.getLayoutNode();
                if (node.getLineIndex() == lastLine) {
                    node.setLayoutPosition(cursor, YogaPhysicalEdge.LEFT);
                    cursor += node.getLayoutWidth() + CHIP_GAP;
                }
            }
        }
    }

    public InfoCard createAboutBrandCard() {
        InfoCard card = new InfoCard();
        card.layout().setAlignItems(YogaAlign.CENTER);
        card.layout().setJustifyContent(YogaJustify.CENTER);

        ContainerWidget row = new ContainerWidget();
        row.layout().setFlexDirection(YogaFlexDirection.ROW);
        row.layout().setWidthPercent(100);
        row.layout().setAlignItems(YogaAlign.CENTER);
        row.layout().setJustifyContent(YogaJustify.SPACE_BETWEEN);
        row.layout().setGap(YogaGutter.COLUMN, 12);

        ContainerWidget brandColumn = new ContainerWidget();
        brandColumn.layout().setFlexDirection(YogaFlexDirection.COLUMN);
        brandColumn.layout().setWidthPercent(60);
        brandColumn.layout().setAlignItems(YogaAlign.CENTER);
        brandColumn.layout().setJustifyContent(YogaJustify.CENTER);
        brandColumn.layout().setGap(YogaGutter.COLUMN, 8);

        StaticLogoWidget logoWidget = new StaticLogoWidget(100f);
        brandColumn.addChild(logoWidget);

        MaterialLabel nameLabel = MaterialLabel.create()
                .text("Super Resolution")
                .fontSize(20)
                .lineHeight(20)
                .weight(700)
                .color(MaterialScheme::onSurface);
        nameLabel.style().sizeToContent(true);
        brandColumn.addChild(nameLabel);

        MaterialLabel versionLabel = MaterialLabel.create()
                .text(safeGetModVersion())
                .fontSize(8)
                .lineHeight(8)
                .weight(400)
                .color(MaterialScheme::onSurfaceVariant);
        versionLabel.style().sizeToContent(true);
        brandColumn.addChild(versionLabel);
        if (Platform.currentPlatform.isDevelopmentEnvironment()) {
            MaterialLabel devEnvLabel = MaterialLabel.create()
                    .text("Development Environment")
                    .fontSize(8)
                    .lineHeight(8)
                    .weight(400)
                    .color(MaterialScheme::onSurfaceVariant);
            devEnvLabel.style().sizeToContent(true);
            brandColumn.addChild(devEnvLabel);
        }

        ContainerWidget actionColumn = new ContainerWidget();
        actionColumn.layout().setFlexDirection(YogaFlexDirection.COLUMN);
        actionColumn.layout().setWidthPercent(40);
        actionColumn.layout().setAlignItems(YogaAlign.CENTER);
        actionColumn.layout().setJustifyContent(YogaJustify.CENTER);
        actionColumn.layout().setGap(YogaGutter.ROW, 10);

        MaterialButton modrinthButton = MaterialButton.tonal("Modrinth")
                .icon(MaterialSymbols.iconOpenInNew())
                .size(MaterialButtonSize.Small);
        modrinthButton.onClick(e -> openExternalLink(ABOUT_MODRINTH_URL));
        actionColumn.addChild(modrinthButton);

        MaterialButton githubButton = MaterialButton.tonal("Github")
                .icon(MaterialSymbols.iconOpenInNew())
                .size(MaterialButtonSize.Small);
        githubButton.onClick(e -> openExternalLink(ABOUT_GITHUB_URL));
        actionColumn.addChild(githubButton);

        MaterialButton websiteButton = MaterialButton.tonal(Text.translatable("superresolution.screen.info.link.official_website").getString())
                .icon(MaterialSymbols.iconOpenInNew())
                .size(MaterialButtonSize.Small);
        websiteButton.onClick(e -> openExternalLink(ABOUT_WEBSITE_URL));
        actionColumn.addChild(websiteButton);

        MaterialButton wikiButton = MaterialButton.tonal(Text.translatable("superresolution.screen.info.link.wiki").getString())
                .icon(MaterialSymbols.iconOpenInNew())
                .size(MaterialButtonSize.Small);
        wikiButton.onClick(e -> openExternalLink(ABOUT_WIKI_URL));
        actionColumn.addChild(wikiButton);

        row.addChild(brandColumn);
        row.addChild(actionColumn);
        card.addChild(row);
        card.layout().setMargin(YogaEdge.BOTTOM, 6);
        card.layout().setHeight(256);
        return card;
    }

    public ContainerWidget createInfoLine(String name, String value) {
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

    public ContainerWidget createContributorRow(ContributorInfo contributor) {
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
                .weight(700)
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

    public ContainerWidget createLibraryRow(LibraryInfo library) {
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
                .weight(700)
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

    public String safeGetModVersion() {
        try {
            if (Platform.currentPlatform == null) {
                return Text.translatable("superresolution.screen.config.info.unknown").getString();
            }
            return Platform.currentPlatform.getModVersionString(SuperResolution.MOD_ID);
        } catch (Throwable ignored) {
            return Text.translatable("superresolution.screen.config.info.unknown").getString();
        }
    }

    public String safeGetNativeVersion() {
        try {
            return SuperResolutionNative.getVersionInfo();
        } catch (Throwable ignored) {
            return Text.translatable("superresolution.screen.config.info.unavailable").getString();
        }
    }

    public String safeGetOperatingSystem() {
        try {
            if (Platform.currentPlatform == null) {
                return Text.translatable("superresolution.screen.config.info.unknown").getString();
            }
            return Platform.currentPlatform.getOS().getString();
        } catch (Throwable ignored) {
            return Text.translatable("superresolution.screen.config.info.unknown").getString();
        }
    }

    public void openExternalLink(String url) {
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


    public record QualityPresetOption(String codeName,

                                       String displayName,

                                       float upscaleRatio,

                                       boolean custom) {
    }

    public record ContributorInfo(String name,

                                   String description,

                                   String githubUrl,

                                   String avatar) {
    }

    public record LibraryInfo(String name,

                               String githubUrl) {
    }

    public static class TitlePill extends MaterialWidget<TitlePill> {
        private final String text;
        private final float fontSize;
        private final float minHeight;
        private final float horizontalPadding;
        private final float radius;

        TitlePill(String text, float fontSize, float minHeight, float horizontalPadding, float radius) {
            this.text = text == null ? "" : text;
            this.fontSize = fontSize;
            this.minHeight = minHeight;
            this.horizontalPadding = horizontalPadding;
            this.radius = radius;
            getLayoutNode().setDebugName("TitlePill");
            setElementSize(horizontalPadding * 2f, minHeight);
        }

        @Override
        protected void init() {
        }

        @Override
        public void layouting(RenderContext ctx) {
            float textWidth = ctx.measureTextWidth(text, fontSize, fontSize + 1f, 700);
            setElementSize((horizontalPadding * 2f) + textWidth, minHeight);
        }

        @Override
        protected boolean isInteractive() {
            return false;
        }

        @Override
        public void render(RenderContext ctx, UIInputState inputState) {
            Rectangle bounds = getBounds();
            ctx.roundedRect(
                    bounds.x,
                    bounds.y,
                    bounds.width,
                    bounds.height,
                    radius < 0 ? bounds.height / 2f : radius,
                    scheme().surfaceContainerLow(),
                    true
            );

            ctx.drawAlignedText(
                    ctx.font(),
                    fontSize,
                    text,
                    bounds.x + horizontalPadding,
                    bounds.getCenterY(),
                    Math.max(0f, bounds.width - (horizontalPadding * 2f)),
                    bounds.height,
                    700,
                    scheme().onSurface(),
                    TextAlign.of(TextAlignType.ALIGN_LEFT, TextAlignType.ALIGN_MIDDLE),
                    false
            );
        }
    }

    public static class StaticLogoWidget extends MaterialWidget<StaticLogoWidget> {
        private final float logoSize;

        StaticLogoWidget(float logoSize) {
            this.logoSize = logoSize;
            setElementSize(logoSize, logoSize);
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
            LogoRenderer.Logo.render(
                    ctx,
                    scheme().primary(),
                    logoSize,
                    getBounds().getCenter()
            );
        }
    }

    public static class InfoCard extends MaterialContainerWidget<InfoCard> {
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
            MaterialElevation.draw(
                    ctx,
                    1,
                    bounds.x,
                    bounds.y,
                    bounds.width,
                    bounds.height,
                    16
            );
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

    public static class ContributorAvatar extends MaterialWidget<ContributorAvatar> {
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
                                RenderSystems.current().device(),
                                inputStream
                        );
                    } catch (Throwable ignored) {
                        SuperResolution.LOGGER.error("Failed to load configuration screen image", ignored);
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
