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

public final class PerformancePage implements ConfigPage {
    public static final PerformancePage INSTANCE = new PerformancePage();

    private PerformancePage() {
    }

    @Override
    public Frame create(ConfigPageContext context) {
        return build(context);
    }

    private static Frame build(ConfigPageContext context) {
        ScrollableFrame frame = context.createStandardScrollableFrame();
        ContainerWidget container = context.createStandardContainer();
        context.addFrameTitle(container, Text.translatable("superresolution.screen.config.section.performance"));

        boolean detailedProfiling = SuperResolutionConfig.isEnableDetailedProfiling();

        List<Pair<String, Text>> operationList = new ArrayList<>(List.of(
                Pair.of("Frame", Text.translatable("superresolution.screen.config.section.performance.chart.frame")),
                Pair.of("Reflex Sleep", Text.translatable("superresolution.screen.config.section.performance.chart.reflex_sleep")),
                Pair.of("Main Render", Text.translatable("superresolution.screen.config.section.performance.chart.main_render")),
                Pair.of("Level Render", Text.translatable("superresolution.screen.config.section.performance.chart.level_render")),
                Pair.of("Upscale", Text.translatable("superresolution.screen.config.section.performance.chart.upscale")),
                Pair.of("GUI", Text.translatable("superresolution.screen.config.section.performance.chart.gui"))
        ));
        if (detailedProfiling) {
            // Per-stage GPU rows. These carry no useful data without detailed profiling -
            // the VK ones have no CPU series at all, since no push/pop pair wraps them -
            // so they are left out entirely rather than drawn as flat lines.
            operationList.addAll(List.of(
                    Pair.of(PerformanceTracker.GL_INPUT_CONVERT,
                            Text.translatable("superresolution.screen.config.section.performance.chart.gl_input_convert")),
                    Pair.of(PerformanceTracker.GL_INTEROP_FLIP,
                            Text.translatable("superresolution.screen.config.section.performance.chart.gl_interop_flip")),
                    Pair.of(PerformanceTracker.GL_CAPTURE_FLIP,
                            Text.translatable("superresolution.screen.config.section.performance.chart.gl_capture_flip")),
                    Pair.of(PerformanceTracker.VK_UPSCALE,
                            Text.translatable("superresolution.screen.config.section.performance.chart.vk_upscale")),
                    Pair.of(PerformanceTracker.VK_FRAME_GEN,
                            Text.translatable("superresolution.screen.config.section.performance.chart.vk_frame_gen")),
                    Pair.of(PerformanceTracker.VK_PRESENT_BLIT,
                            Text.translatable("superresolution.screen.config.section.performance.chart.vk_present_blit"))
            ));
        }
            Pair<String, Text>[] operations = operationList.toArray(new Pair[0]);

        for (Pair<String, Text> operation : operations) {
            MaterialChart cpuChart = MaterialChart.create()
                    .title(operation.right().getString())
                    .addSeries(new MaterialChartDataSeries("CPU (ms)", Color.from("#4FC3F7"), MaterialChartType.Line, 256))
                    .addSeries(new MaterialChartDataSeries("GPU (ms)", Color.from("#BA53FF"), MaterialChartType.Line, 256))
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
                    .showLegend(true)
                    .dataLineWidth(1f);
            cpuChart.layout().setWidthPercent(100);
            cpuChart.setElementHeight(180);
            cpuChart.layout().setMargin(YogaEdge.BOTTOM, 8);
            container.addChild(cpuChart);
        }
        context.finalizeFrame(frame, container);
        return frame;
    }

}
