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
import io.homo.superresolution.common.config.enums.InteropSyncMode;
import io.homo.superresolution.common.config.special.SpecialConfig;
import io.homo.superresolution.common.config.special.SpecialConfigDescription;
import io.homo.superresolution.common.gui.download.MaterialResourcesList;
import io.homo.superresolution.common.gui.impl.OptionRequirement;
import io.homo.superresolution.common.gui.impl.Text;
import io.homo.superresolution.common.gui.options.*;
import io.homo.superresolution.common.minecraft.MinecraftUtils;
import io.homo.superresolution.common.minecraft.MinecraftWindow;
import io.homo.superresolution.common.minecraft.handler.RenderHandlerManager;
import io.homo.superresolution.common.perf.PerformanceTracker;
import io.homo.superresolution.common.upscale.AlgorithmDescriptions;
import io.homo.superresolution.common.upscale.SRApiAlgorithm;
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
import io.homo.superresolution.core.gui.widgets.dialog.MaterialDialog;
import io.homo.superresolution.core.gui.widgets.label.MaterialLabel;
import io.homo.superresolution.core.gui.widgets.navigation.drawer.MaterialNavigationDrawer;
import io.homo.superresolution.core.impl.Destroyable;
import io.homo.superresolution.core.impl.Pair;
import io.homo.superresolution.core.utils.Color;
import io.homo.superresolution.core.utils.ImageLoader;
import io.homo.superresolution.core.utils.MouseCursor;
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
    private static final String ABOUT_MODRINTH_URL = "https://modrinth.com/mod/superresolution";
    private static final String ABOUT_GITHUB_URL = "https://github.com/187J3X1-114514/superresolution";
    private static final long CONTENT_TRANSITION_FADE_OUT_DURATION_MS = 150L;
    private static final long CONTENT_TRANSITION_FADE_IN_DURATION_MS = 150L;
    private static final long CONTENT_TRANSITION_TOTAL_DURATION_MS =
            CONTENT_TRANSITION_FADE_OUT_DURATION_MS + CONTENT_TRANSITION_FADE_IN_DURATION_MS;
    private static final float CONTENT_TRANSITION_OFFSET_RATIO = 0.06f;
    private static final float CONTENT_TRANSITION_OFFSET_MIN = 16f;
    private static final float CONTENT_TRANSITION_OFFSET_MAX = 60f;
    private static final float FRAME_TITLE_PILL_FONT_SIZE = 24f * 0.8f;
    private static final float GROUP_TITLE_PILL_FONT_SIZE = 18f * 0.7f;
    private static final float FRAME_TITLE_PILL_MIN_HEIGHT = 40f;
    private static final float GROUP_TITLE_PILL_MIN_HEIGHT = 30f;
    private static final float FRAME_TITLE_PILL_HORIZONTAL_PADDING = 16f;
    private static final float GROUP_TITLE_PILL_HORIZONTAL_PADDING = 9f;

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
    private boolean contentTransitionRunning;
    private Frame outgoingContentFrame;
    private long contentTransitionStartMs;
    private float contentTransitionOffsetY;

    public MaterialConfigScreen(Screen parentScreen) {
        super(Component.translatable("superresolution.screen.config.name"));
        this.parentScreen = parentScreen;
    }

    @Override
    protected void buildWidgets() {
        clearContentTransitionState();

        if (qualityPresetOptionsCache == null) {
            qualityPresetOptionsCache = new HashMap<>();
        }
        MaterialUI.setScheme(MaterialScheme.from(SuperResolutionConfig.getTheme(), SuperResolutionConfig.getThemeColor(),
                SuperResolutionConfig.getThemeSchemeVariant(), SuperResolutionConfig.getThemeContrastLevel()));
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
        clearContentTransitionState();
        destroyables.forEach(Destroyable::destroy);
        MinecraftUtils.setScreen(parentScreen);
        MouseCursor.ARROW.use();
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

        updateContentTransition();

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

        if (currentContentFrame == null || contentLayout == null) {
            currentContentKey = key;
            currentContentFrame = getOrCreateContentFrame(key);
            contentLayout = getView().addFrame(currentContentFrame);
            contentLayout.setFlexGrow(1f);
            contentLayout.setHeightPercent(100);
            contentLayout.setPadding(YogaEdge.ALL, 0);
            view.markLayoutDirty();
            return;
        }

        interruptContentTransition();

        getView().calculateLayout();

        Frame previousFrame = currentContentFrame;
        YogaNode previousLayout = contentLayout;
        float previousX = previousLayout.getLayoutX();
        float previousY = previousLayout.getLayoutY();
        float previousWidth = previousLayout.getLayoutWidth();
        float previousHeight = previousLayout.getLayoutHeight();

        currentContentKey = key;
        currentContentFrame = getOrCreateContentFrame(key);
        contentLayout = getView().addFrame(currentContentFrame);
        contentLayout.setFlexGrow(1f);
        contentLayout.setHeightPercent(100);
        contentLayout.setPadding(YogaEdge.ALL, 0);

        previousLayout.setPositionType(YogaPositionType.ABSOLUTE);
        previousLayout.setPosition(YogaEdge.LEFT, previousX);
        previousLayout.setPosition(YogaEdge.TOP, previousY);
        previousLayout.setWidth(previousWidth);
        previousLayout.setHeight(previousHeight);
        previousLayout.setFlexGrow(0f);
        previousLayout.setFlexShrink(0f);

        outgoingContentFrame = previousFrame;
        contentTransitionRunning = true;
        contentTransitionStartMs = System.currentTimeMillis();
        contentTransitionOffsetY = calculateContentEnterOffset(previousHeight);

        getView().setFrameRenderAlpha(outgoingContentFrame, 1f);
        getView().setFrameRenderOffsetY(outgoingContentFrame, 0f);
        getView().setFrameRenderAlpha(currentContentFrame, 0f);
        getView().setFrameRenderOffsetY(currentContentFrame, contentTransitionOffsetY);

        view.markLayoutDirty();
    }

    private void interruptContentTransition() {
        if (!contentTransitionRunning) {
            return;
        }

        if (currentContentFrame != null) {
            getView().resetFrameRenderState(currentContentFrame);
        }
        if (outgoingContentFrame != null) {
            getView().resetFrameRenderState(outgoingContentFrame);
            getView().removeFrame(outgoingContentFrame);
        }

        clearContentTransitionState();
    }

    private void updateContentTransition() {
        if (!contentTransitionRunning) {
            return;
        }

        if (currentContentFrame == null || outgoingContentFrame == null) {
            finishContentTransition();
            return;
        }

        float elapsedMs = System.currentTimeMillis() - contentTransitionStartMs;

        float progress = clamp(elapsedMs / CONTENT_TRANSITION_TOTAL_DURATION_MS, 0f, 1f);

        float spatialEased = TimeInterpolator.easeOutQuint().interpolation(progress);

        float outAlphaProgress = clamp(progress / 0.35f, 0f, 1f);
        float outAlpha = 1f - outAlphaProgress;
        float outOffsetY = -contentTransitionOffsetY * spatialEased * 0.5f;

        float inAlphaProgress = clamp((progress - 0.30f) / 0.70f, 0f, 1f);
        float inAlphaEased = TimeInterpolator.easeOutCirc().interpolation(inAlphaProgress);
        float inOffsetY = contentTransitionOffsetY * (1f - spatialEased);

        getView().setFrameRenderAlpha(outgoingContentFrame, outAlpha);
        getView().setFrameRenderOffsetY(outgoingContentFrame, outOffsetY);

        getView().setFrameRenderAlpha(currentContentFrame, inAlphaEased);
        getView().setFrameRenderOffsetY(currentContentFrame, inOffsetY);

        if (progress >= 1f) {
            finishContentTransition();
        }
    }

    private void finishContentTransition() {
        if (currentContentFrame != null) {
            getView().resetFrameRenderState(currentContentFrame);
        }
        if (outgoingContentFrame != null) {
            getView().resetFrameRenderState(outgoingContentFrame);
            getView().removeFrame(outgoingContentFrame);
        }
        clearContentTransitionState();
        view.markLayoutDirty();
    }

    private void clearContentTransitionState() {
        contentTransitionRunning = false;
        outgoingContentFrame = null;
        contentTransitionStartMs = 0L;
        contentTransitionOffsetY = 0f;
    }

    private float calculateContentEnterOffset(float height) {
        float base = Math.max(0f, height) * CONTENT_TRANSITION_OFFSET_RATIO;
        return clamp(base, CONTENT_TRANSITION_OFFSET_MIN, CONTENT_TRANSITION_OFFSET_MAX);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
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
                .addDivider()
                .addSectionHeader(Text.translatable("superresolution.screen.config.section.profiling").getString())
                .addItem(Text.translatable("superresolution.screen.config.section.performance").getString(), MaterialSymbols.iconSpeed(), "performance")
                .addDivider()
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
                    MaterialUI.setScheme(MaterialScheme.from(value, SuperResolutionConfig.getThemeColor(),
                            SuperResolutionConfig.getThemeSchemeVariant(), SuperResolutionConfig.getThemeContrastLevel()));
                    this.materialScheme = MaterialUI.Scheme;
                })
                .build();
        builder.colorSelectOption(
                        Text.translatable("superresolution.screen.config.options.label.theme_color"),
                        SuperResolutionConfig.getThemeColor())
                .setDefaultValue(() -> Color.from("#78DC77"))
                .setValueChangeListener(value -> {
                    MaterialUI.setScheme(MaterialScheme.from(SuperResolutionConfig.getTheme(), value,
                            SuperResolutionConfig.getThemeSchemeVariant(), SuperResolutionConfig.getThemeContrastLevel()));
                    this.materialScheme = MaterialUI.Scheme;
                })
                .setSaveConsumer(value -> {
                    SuperResolutionConfig.setThemeColor(value);
                    MaterialUI.setScheme(MaterialScheme.from(SuperResolutionConfig.getTheme(), value,
                            SuperResolutionConfig.getThemeSchemeVariant(), SuperResolutionConfig.getThemeContrastLevel()));
                    this.materialScheme = MaterialUI.Scheme;
                })
                .build();
        builder.enumSelectorOption(
                        Text.translatable("superresolution.screen.config.options.label.theme_scheme_variant"),
                        SchemeVariant.class,
                        SuperResolutionConfig.getThemeSchemeVariant())
                .setDefaultValue(SchemeVariant.CONTENT)
                .setEnumNameProvider(v -> Text.translatable("superresolution.enum.schemevarinat." + v.name().toLowerCase()).getString())
                .setSaveConsumer(value -> {
                    SuperResolutionConfig.setThemeSchemeVariant(value);
                    MaterialUI.setScheme(MaterialScheme.from(SuperResolutionConfig.getTheme(), SuperResolutionConfig.getThemeColor(),
                            value, SuperResolutionConfig.getThemeContrastLevel()));
                    this.materialScheme = MaterialUI.Scheme;
                })
                .build();
        builder.numberOption(
                        Text.translatable("superresolution.screen.config.options.label.theme_contrast_level"),
                        SuperResolutionConfig.getThemeContrastLevel(),
                        1.0f,
                        -1.0f)
                .setStep(0.2)
                .setValueFormater((value) -> String.format("%.0f", value.doubleValue() * 100) + "%")
                .setDescription(Text.translatable("superresolution.screen.config.options.tooltip.theme_contrast_level"))
                .setDefaultValue(() -> 0.0f)
                .setValueChangeListener(value -> {
                    MaterialUI.setScheme(MaterialScheme.from(SuperResolutionConfig.getTheme(), SuperResolutionConfig.getThemeColor(),
                            SuperResolutionConfig.getThemeSchemeVariant(), value.doubleValue()));
                    this.materialScheme = MaterialUI.Scheme;
                })
                .setSaveConsumer(value -> {
                    SuperResolutionConfig.setThemeContrastLevel(value.floatValue());
                    MaterialUI.setScheme(MaterialScheme.from(SuperResolutionConfig.getTheme(), SuperResolutionConfig.getThemeColor(),
                            SuperResolutionConfig.getThemeSchemeVariant(), value.doubleValue()));
                    this.materialScheme = MaterialUI.Scheme;
                })
                .build();

        addOptionGroupToContainer(container, builder);
        finalizeFrame(frame, container);
        return frame;
    }

    private void openCreateAlgorithmFailedDialog(AlgorithmDescription<?> description) {
        MaterialDialog dialog = MaterialDialog.create()
                .icon(MaterialSymbols.iconError())
                .headline(Text.translatable("superresolution.screen.config.dialog.create_algorithm_failed.title").getString())
                .supportingText(Text.translatable("superresolution.screen.config.dialog.create_algorithm_failed.message").getString().formatted(description.displayName))
                .addAction(Text.translatable("superresolution.screen.config.dialog.create_algorithm_failed.action.confirm").getString(), MaterialButtonVariant.Tonal, MaterialDialog::dismiss);
        getView().showDialog(dialog);
    }

    private boolean isExperimentalAlgorithm(AlgorithmDescription<?> algorithmDescription){
        return algorithmDescription.equals(AlgorithmDescriptions.DLSS) ||
                algorithmDescription.equals(AlgorithmDescriptions.XESS) ||
                algorithmDescription.equals(AlgorithmDescriptions.ANIME4K);
    }

    private Frame createGeneralFrame() {
        ScrollableFrame frame = createStandardScrollableFrame();
        ContainerWidget container = createStandardContainer();
        addFrameTitle(container, Text.translatable("superresolution.screen.config.section.general"));

        @SuppressWarnings("unchecked")
        final SelectionListOptionEntry<QualityPresetOption>[] qualityPresetEntryRef = new SelectionListOptionEntry[1];
        final SelectionListOptionEntry[] algoSelectRef = new SelectionListOptionEntry[1];

        final NumberSliderOptionEntry[] upscaleRatioEntryRef = new NumberSliderOptionEntry[1];
        final boolean[] syncingQualityPreset = {false};

        OptionBuilder builder = createOptionBuilder(Text.translatable("superresolution.screen.config.category.general"));
        builder.hintOption(Text.literal("tip114514"))
                .setIcon(MaterialSymbols.iconWarning())
                .setTitle(Text.translatable("superresolution.screen.config.hint.performance_warning.title").getString())
                .setText(Text.translatable("superresolution.screen.config.hint.performance_warning.text").getString())
                .setDisplayRequirement(OptionRequirement.isTrue(() -> SuperResolutionConfig.isEnableUpscaleOriginal() && !SRWorkModeManager.getCurrentState().shaderPackInUse() && !SuperResolutionConfig.isDisableUpscaleOnVanilla()))
                .build();
        builder.hintOption(Text.literal("shader_compat_warning"))
                .setIcon(MaterialSymbols.iconWarning())
                .setTitle(Text.translatable("superresolution.screen.config.hint.shader_compat_warning.title").getString())
                .setText(Text.translatable("superresolution.screen.config.hint.shader_compat_warning.text").getString())
                .setDisplayRequirement(OptionRequirement.isTrue(() -> !SRWorkModeManager.isCurrentMode(SRWorkModeManager.SHADER_COMPAT) &&
                        SuperResolutionConfig.isEnableUpscaleOriginal() &&
                        SRWorkModeManager.getCurrentState().shaderPackInUse()))
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

        algoSelectRef[0] = builder.selectorOption(
                        Text.translatable("superresolution.screen.config.options.label.algo_type"),
                        SuperResolutionConfig.getUpscaleAlgorithm(),
                        AlgorithmRegistry.getAlgorithmMap().values().toArray())
                .setNameProvider(algo -> ((AlgorithmDescription<?>) algo).getBriefName())
                .setDefaultValue(() -> AlgorithmDescriptions.FSR1)
                .setSaveConsumer((obj) -> {
                    AlgorithmDescription<?> algo = (AlgorithmDescription<?>) obj;
                    List<ExtraResource> lostResources = algo.getExtraResources().checkAll(SuperResolutionConstants.NATIVE_LIBRARIES_DIR);
                    if (!lostResources.isEmpty()) {
                        openLostResourceDialog(lostResources);
                        return false;
                    }
                    if (!SuperResolutionConfig.setUpscaleAlgorithm(algo)) {
                        openCreateAlgorithmFailedDialog(algo);
                        algoSelectRef[0].setSelectedValue(SuperResolutionConfig.getUpscaleAlgorithm());
                    }
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
                                if (isExperimentalAlgorithm(algorithmDescription)) return SuperResolutionConfig.isEnableExperimentalFeatures();
                                return true;

                            }
                    );
                })
                .setMenuItemTooltipSupplier((algo)->{
                    AlgorithmDescription<?> algorithmDescription = (AlgorithmDescription<?>) algo;
                    var result = algorithmDescription.getRequirement().check();
                    StringBuilder sb = new StringBuilder();
                    sb.append(algorithmDescription.getDisplayName());
                    if (isExperimentalAlgorithm(algorithmDescription) && SuperResolutionConfig.isEnableExperimentalFeatures()){
                        sb.append("\n");
                        sb.append(Text.translatable("superresolution.screen.config.options.tooltip.algo.experimental_warning").getString());
                        if (!result.support()) sb.append("\n");
                    } else if(isExperimentalAlgorithm(algorithmDescription) && !SuperResolutionConfig.isEnableExperimentalFeatures()){
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
                .setTooltipSupplier((t)->{
                    if (!isAlgorithmSupportsCustomUpscaleRatio(SuperResolutionConfig.getUpscaleAlgorithm())){
                        return Optional.of(Tooltip.withContext(Text.translatable("superresolution.screen.config.options.tooltip.upscale_ratio.custom_unsupported").getString()));
                    }else {
                        return Optional.of(Tooltip.empty());
                    }
                })
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

    private Pair<MaterialResourcesList, MaterialDialog> createLocalResourceSelector(List<ExtraResource> resources) {
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
                        selector.left().startDownload();
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

    private Pair<MaterialResourcesList, MaterialDialog> createOnlineResourceSelector(List<ExtraResource> resources) {
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

    private void openLostResourceDialog(List<ExtraResource> resources) {
        #if ENABLE_AUTO_DOWNLOAD == 1
        Pair<MaterialResourcesList,MaterialDialog> selector = createOnlineResourceSelector(resources);
        getView().showDialog(selector.right());
        selector.left().startDownload();
        #else
        Pair<MaterialResourcesList,MaterialDialog> selector = createLocalResourceSelector(resources);
        getView().showDialog(selector.right());

        #endif
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
        builder.enumSelectorOption(
                        Text.translatable("superresolution.screen.config.options.label.interop_sync_mode"),
                        InteropSyncMode.class,
                        SuperResolutionConfig.getInteropSyncMode())
                .setDescription(Text.translatable("superresolution.screen.config.options.tooltip.interop_sync_mode"))
                .setDefaultValue(InteropSyncMode.LowLatency)
                .setEnumNameProvider(mode -> ((InteropSyncMode) mode).toString())
                .setSaveConsumer((value) -> {
                    SuperResolutionConfig.setInteropSyncMode(value);
                    if (SuperResolution.currentAlgorithm instanceof SRApiAlgorithm) {
                        SuperResolution.recreateAlgorithm();
                    }
                })
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
                .setEnableRequirement(() -> false)
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

    private TitlePill createSectionPill(String text) {
        return createTitlePill(
                text,
                GROUP_TITLE_PILL_FONT_SIZE,
                GROUP_TITLE_PILL_MIN_HEIGHT,
                GROUP_TITLE_PILL_HORIZONTAL_PADDING,
                -1
        );
    }

    private TitlePill createTitlePill(
            String text,
            float fontSize,
            float minHeight,
            float horizontalPadding,
            float radius
    ) {
        return new TitlePill(text, fontSize, minHeight, horizontalPadding, radius);
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
                Pair.of("GUI", Text.translatable("superresolution.screen.config.section.performance.chart.gui")),
        };

        for (Pair<String, Text> operation : operations) {
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
                    .updateInterval(0);
            cpuChart.style()
                    .showAverage(true)
                    .showGrid(true)
                    .showLegend(true);
            cpuChart.layout().setWidthPercent(100);
            cpuChart.setElementHeight(180);
            cpuChart.layout().setMargin(YogaEdge.BOTTOM, 8);
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

        TitlePill label = createSectionPill(
                Text.translatable("superresolution.screen.config.info.environment.base").getString()
        );
        label.layout().setMargin(YogaEdge.TOP, 8);
        label.layout().setMargin(YogaEdge.BOTTOM, 6);
        container.addChild(label);

        InfoCard envCard = new InfoCard();
        envCard.addChild(createInfoLine(Text.translatable("superresolution.screen.config.info.environment.mod_version").getString(), safeGetModVersion()));
        envCard.addChild(createInfoLine(Text.translatable("superresolution.screen.config.info.environment.native_version").getString(), safeGetNativeVersion()));
        envCard.addChild(createInfoLine(Text.translatable("superresolution.screen.config.info.environment.system").getString(), safeGetOperatingSystem()));
        container.addChild(envCard);
        TitlePill labelOGL = createSectionPill(
                Text.translatable("superresolution.screen.config.info.environment.opengl").getString()
        );
        labelOGL.layout().setMargin(YogaEdge.TOP, 8);
        labelOGL.layout().setMargin(YogaEdge.BOTTOM, 6);
        container.addChild(labelOGL);

        container.addChild(createGraphicsInfoCard(
                Text.translatable("superresolution.screen.config.info.environment.opengl").getString(),
                GraphicsCapabilities.getGLVersionString(),
                GraphicsCapabilities.getGLExtensions()
        ));
        TitlePill labelVK = createSectionPill(
                Text.translatable("superresolution.screen.config.info.environment.vulkan").getString()
        );
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
        container.addChild(createAboutBrandCard());

        ContainerWidget contributorSectionRow = new ContainerWidget();
        contributorSectionRow.layout().setFlexDirection(YogaFlexDirection.ROW);
        contributorSectionRow.layout().setWidthPercent(100);
        contributorSectionRow.layout().setAlignItems(YogaAlign.CENTER);
        contributorSectionRow.layout().setJustifyContent(YogaJustify.SPACE_BETWEEN);
        contributorSectionRow.layout().setMargin(YogaEdge.BOTTOM, 6);

        TitlePill contributorSection = createSectionPill(
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
                new ContributorInfo("187J3X1", Text.translatable("superresolution.screen.config.info.about.contributor.187j3x1.desc").getString(), "https://github.com/187J3X1-114514", "/assets/super_resolution/textures/gui/contributors/114514.png"),
                new ContributorInfo("异世界美西螈", Text.translatable("superresolution.screen.config.info.about.contributor.ysjmxy.desc").getString(), "https://github.com/ysjmxy", "/assets/super_resolution/textures/gui/contributors/mxy.png"),
                new ContributorInfo("yu", Text.translatable("superresolution.screen.config.info.about.contributor.yu.desc").getString(), "https://github.com/yu234567", "/assets/super_resolution/textures/gui/contributors/yu.png"),
                new ContributorInfo("Enaium", Text.translatable("superresolution.screen.config.info.about.contributor.enaium.desc").getString(), "https://github.com/Enaium", "/assets/super_resolution/textures/gui/contributors/Enaium.png"),
                new ContributorInfo("rrtt217", Text.translatable("superresolution.screen.config.info.about.contributor.rrtt217.desc").getString(), "https://github.com/rrtt217", "/assets/super_resolution/textures/gui/contributors/rrtt217.png"),
                new ContributorInfo("筱烷", Text.translatable("superresolution.screen.config.info.about.contributor.shiroiame.desc").getString(), "https://github.com/Shiroiame-Kusu", "/assets/super_resolution/textures/gui/contributors/Shiroiame-Kusu.png"),
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
                new ContributorInfo("GeForceLegend", Text.translatable("superresolution.screen.config.info.about.contributor.geforcelegend.desc").getString(), "https://github.com/GeForceLegend", "/assets/super_resolution/textures/gui/contributors/geforcelegend.png")
        ));
        Collections.shuffle(contributors);
        for (ContributorInfo contributor : contributors) {
            contributorsCard.addChild(createContributorRow(contributor));
        }
        container.addChild(contributorsCard);

        TitlePill librarySection = createSectionPill(
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
            librariesCard.addChild(createLibraryRow(library));
        }
        container.addChild(librariesCard);
        TitlePill legalSection = createSectionPill(
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

        finalizeFrame(frame, container);
        return frame;
    }

    private InfoCard createAboutBrandCard() {
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

        row.addChild(brandColumn);
        row.addChild(actionColumn);
        card.addChild(row);
        card.layout().setMargin(YogaEdge.BOTTOM, 6);
        card.layout().setHeight(256);
        return card;
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

    private static class TitlePill extends MaterialWidget<TitlePill> {
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

    private static class StaticLogoWidget extends MaterialWidget<StaticLogoWidget> {
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
                                RenderSystems.current().device(),
                                inputStream
                        );
                    } catch (Throwable ignored) {
                        SuperResolution.LOGGER.error("加载配置界面图像失败", ignored);
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
