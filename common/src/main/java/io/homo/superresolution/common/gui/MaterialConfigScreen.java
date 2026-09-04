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
import net.minecraft.client.gui.screens.Screen;
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

import io.homo.superresolution.common.gui.config.pages.*;
public class MaterialConfigScreen extends NanoVGScreen<MaterialConfigScreen> {
    private static final String ABOUT_MODRINTH_URL = "https://modrinth.com/mod/superresolution";
    private static final String ABOUT_GITHUB_URL = "https://github.com/187J3X1-114514/superresolution";
    private static final String ABOUT_WEBSITE_URL = "https://sr.187j3x1-114514.org/";
    private static final String ABOUT_WIKI_URL = "https://sr.187j3x1-114514.org/docs";
    private static final long CONTENT_TRANSITION_FADE_OUT_DURATION_MS = 120L;
    private static final long CONTENT_TRANSITION_FADE_IN_DURATION_MS = 120L;
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
    #if MC_VER >= MC_1_21_11 && MC_VER < MC_26_2 || MC_VER >= MC_1_21 && MC_VER < MC_1_21_2 || MC_VER == MC_1_20_1 || MC_VER == MC_26_2
    private static final boolean CURRENT_VERSION_SUPPORTS_VULKAN_PRESENTATION = true;
    #else
    private static final boolean CURRENT_VERSION_SUPPORTS_VULKAN_PRESENTATION = false;
    #endif

    private final Screen parentScreen;
    private MaterialScheme materialScheme;
    private String currentContentKey = "general";
    private Map<String, Frame> contentFrames;
    private YogaNode navigationDrawerLayout;
    private YogaNode contentLayout;
    private Frame currentContentFrame;
    private MaterialNavigationDrawer drawer;
    private boolean contentTransitionRunning;
    private Frame outgoingContentFrame;
    private long contentTransitionStartMs;
    private float contentTransitionOffsetY;
    private ConfigPageContext pageContext;

    public MaterialConfigScreen(Screen parentScreen) {
        super(Component.translatable("superresolution.screen.config.name"));
        this.parentScreen = parentScreen;
    }

    @Override
    protected void buildWidgets() {
        clearContentTransitionState();

        MaterialUI.setScheme(MaterialScheme.from(SuperResolutionConfig.getTheme(), SuperResolutionConfig.getThemeColor(),
                SuperResolutionConfig.getThemeSchemeVariant(), SuperResolutionConfig.getThemeContrastLevel()));
        materialScheme = MaterialUI.Scheme;
        pageContext = new ConfigPageContext(
                getView(),
                materialScheme,
                scheme -> this.materialScheme = scheme,
                this::openRestartRequiredDialog,
                this::invalidateContentFrame,
                () -> MinecraftUtils.getScreen() == this
        );
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
        if (pageContext != null) {
            pageContext.destroy();
        }
        MinecraftUtils.setScreen(parentScreen);
        MouseCursor.ARROW.use();
    }

    #if MC_VER > MC_1_21_8
    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_INSERT) {
            MinecraftUtils.setScreen(new WidgetShowcaseScreen(this));
            return true;
        }
        return super.keyPressed(event);
    }
    #else
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_INSERT) {
            MinecraftUtils.setScreen(new WidgetShowcaseScreen(this));
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    #endif

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
        float widthPercent = 0.185f;
        drawerWidth = Math.max(drawerWidth, ctx.viewportWidth() * widthPercent);
        if (drawerWidth > 0) {
            navigationDrawerLayout.setWidth(drawerWidth);
            view.markLayoutDirty();
        }
        drawer.layout().setMinHeight(ctx.viewportHeight());
        view.markLayoutDirty();

        updateContentTransition();

        super.draw(ctx, inputState);
    }

    /**
     * Forces {@code key}'s page to be rebuilt the next time it is displayed. A page that
     * is currently on screen keeps rendering its existing instance until it is switched
     * away from, at which point the view detaches it, so dropping the cache entry here
     * cannot leave two instances attached.
     */
    private void invalidateContentFrame(String key) {
        if (contentFrames != null) {
            contentFrames.remove(key);
        }
    }

    private Frame getOrCreateContentFrame(String key) {
        if (contentFrames.containsKey(key)) {
            return contentFrames.get(key);
        }
        ConfigPage page;
        switch (key) {
            case "general": page = GeneralPage.INSTANCE; break;
            case "advanced": page = AdvancedPage.INSTANCE; break;
            case "algorithm": page = AlgorithmPage.INSTANCE; break;
            case "experimental": page = ExperimentalPage.INSTANCE; break;
            case "appearance": page = AppearancePage.INSTANCE; break;
            case "performance": page = PerformancePage.INSTANCE; break;
            case "debug": page = DebugPage.INSTANCE; break;
            case "info_environment": page = EnvironmentInfoPage.INSTANCE; break;
            case "info_about": page = AboutInfoPage.INSTANCE; break;
            default: page = EmptyPage.INSTANCE;
        }
        Frame frame = page.create(pageContext);
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
        drawer.layout().setHeightPercent(100f);
        container.addChild(drawer);

        frame.setRoot(container);
        return frame;
    }
    private void openRestartRequiredDialog() {
        pageContext.openRestartRequiredDialog();
    }

    public void setMaterialScheme(MaterialScheme scheme) {
        this.materialScheme = scheme;
    }

    public boolean isPauseScreen() {
        return SuperResolutionConfig.isPauseGameOnGui();
    }
}
