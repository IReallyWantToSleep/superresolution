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

import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.gui.impl.Text;
import io.homo.superresolution.common.perf.PerformanceTracker;
import io.homo.superresolution.core.gui.core.ContainerWidget;
import io.homo.superresolution.core.gui.core.frame.Frame;
import io.homo.superresolution.core.gui.core.frame.ScrollableFrame;
import io.homo.superresolution.core.gui.widgets.chart.MaterialChart;
import io.homo.superresolution.core.gui.widgets.chart.MaterialChartDataSeries;
import io.homo.superresolution.core.gui.widgets.chart.MaterialChartType;
import io.homo.superresolution.core.impl.Pair;
import io.homo.superresolution.core.utils.Color;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.*;

import java.util.*;

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
