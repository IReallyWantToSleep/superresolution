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

package io.homo.superresolution.common.debug.imgui;

import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.ImVec2;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiHoveredFlags;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import io.homo.superresolution.api.InitializationDescription;
import io.homo.superresolution.api.SuperResolutionAPI;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.minecraft.handler.IMinecraftRenderHandler;
import io.homo.superresolution.common.minecraft.handler.RenderHandlerManager;
import io.homo.superresolution.common.perf.PerformanceTracker;
import io.homo.superresolution.common.upscale.AlgorithmDescriptions;
import io.homo.superresolution.common.upscale.AlgorithmManager;
import io.homo.superresolution.common.upscale.fsr2.FSR2;
import io.homo.superresolution.common.workmode.SRWorkModeManager;
import io.homo.superresolution.common.workmode.SRWorkModeProvider;
import io.homo.superresolution.common.workmode.SRWorkModeState;
import io.homo.superresolution.core.graphics.impl.framebuffer.FrameBufferAttachmentType;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.opengl.buffer.GlBuffer;
import io.homo.superresolution.thirdparty.fsr2.common.Fsr2Context;
import io.homo.superresolution.thirdparty.fsr2.common.Fsr2PipelineResourceType;
import io.homo.superresolution.thirdparty.fsr2.common.Fsr2PipelineResources;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ImGuiLayer {

    private static final float PREVIEW_WIDTH = 280.0f;
    private static final float TEXTURE_GRID_SPACING = 12.0f;
    private static final String[] PERF_OPERATIONS = {"Frame", "Level Render", "Main Render", "Upscale", "GUI"};
    private static final Map<String, ImBoolean> WINDOW_OPEN = new LinkedHashMap<>();

    static {
        WINDOW_OPEN.put("General", new ImBoolean(true));
        WINDOW_OPEN.put("Work Mode State", new ImBoolean(true));
        WINDOW_OPEN.put("Performance", new ImBoolean(true));
        WINDOW_OPEN.put("Work Mode Provider", new ImBoolean(true));
        WINDOW_OPEN.put("Render Handler", new ImBoolean(true));
        WINDOW_OPEN.put("Textures", new ImBoolean(true));
        WINDOW_OPEN.put("Algorithm", new ImBoolean(true));
    }

    private final List<DebugTextureEntry> textures = new ArrayList<>();
    private final List<ViewerInstance> viewers = new ArrayList<>();
    private final Map<String, float[]> cpuPerfHistory = new LinkedHashMap<>();
    private final Map<String, float[]> gpuPerfHistory = new LinkedHashMap<>();

    public void imgui() {
        SRWorkModeProvider provider = safeGetCurrentProvider();
        IMinecraftRenderHandler handler = RenderHandlerManager.getHandler();

        textures.clear();
        collectBaseTextures();
        collectProviderTextures(provider);
        collectHandlerTextures(handler);
        collectAlgorithmTextures();

        drawTitleBar();
        drawGeneralWindow(provider, handler);

        if (SuperResolution.gameIsLoaded && Minecraft.getInstance().level != null) {
            drawWorkModeStateWindow(provider);
            drawPerformanceWindow();
            drawProviderWindow(provider);
            drawHandlerWindow(handler);
            drawTexturesWindow();
            drawAlgorithmWindow();
        }

        drawViewerWindows();
    }

    private void drawTitleBar() {
        if (!ImGui.beginMainMenuBar()) {
            return;
        }
        drawTitleBadge();
        for (Map.Entry<String, ImBoolean> entry : WINDOW_OPEN.entrySet()) {
            ImGui.sameLine();
            drawWindowToggleButton(entry.getKey(), entry.getValue());
        }
        ImGui.endMainMenuBar();
    }

    private void drawTitleBadge() {
        float badgeHeight = ImGui.getTextLineHeightWithSpacing() + 6.0f;
        float badgeWidth = ImGui.calcTextSize(" Super Resolution ").x + 4.0f;
        ImGui.getWindowDrawList().addRectFilled(
                0.0f,
                0.0f,
                badgeWidth,
                badgeHeight,
                ImGuiDebugColors.TITLE_BAR_BADGE
        );
        ImGui.text("Super Resolution");
    }

    private void drawWindowToggleButton(String title, ImBoolean open) {
        boolean enabled = open.get();
        if (enabled) {
            ImGui.pushStyleColor(ImGuiCol.Button, ImGuiDebugColors.WINDOW_BUTTON_ON);
            ImGui.pushStyleColor(ImGuiCol.ButtonHovered, ImGuiDebugColors.WINDOW_BUTTON_ON_HOVERED);
            ImGui.pushStyleColor(ImGuiCol.ButtonActive, ImGuiDebugColors.WINDOW_BUTTON_ON_ACTIVE);
        } else {
            ImGui.pushStyleColor(ImGuiCol.Button, ImGuiDebugColors.WINDOW_BUTTON_OFF);
            ImGui.pushStyleColor(ImGuiCol.ButtonHovered, ImGuiDebugColors.WINDOW_BUTTON_OFF_HOVERED);
            ImGui.pushStyleColor(ImGuiCol.ButtonActive, ImGuiDebugColors.WINDOW_BUTTON_OFF_ACTIVE);
        }
        if (ImGui.smallButton(title)) {
            open.set(!enabled);
        }
        ImGui.popStyleColor(3);
    }

    private void drawGeneralWindow(SRWorkModeProvider provider, IMinecraftRenderHandler handler) {
        if (!isWindowVisible("General")) {
            return;
        }
        beginWindow("General", 24, 24, 420, 240);
        drawCaptureButtons();
        ImGui.separator();
        ImGui.text("Provider: " + (provider == null ? "<none>" : provider.id()));
        ImGui.text("Handler: " + (handler == null ? "<none>" : handler.getClass().getSimpleName()));
        ImGui.text("Algorithm: " + getAlgorithmName());
        ImGui.text("Screen Size: %dx%d".formatted(RenderHandlerManager.getScreenWidth(), RenderHandlerManager.getScreenHeight()));
        ImGui.text("Render Size: %dx%d".formatted(RenderHandlerManager.getRenderWidth(), RenderHandlerManager.getRenderHeight()));
        ImGui.text("Scale Factor: %.3f".formatted(RenderHandlerManager.getScaleFactor()));
        ImGui.text("Frame Count: " + RenderHandlerManager.getFrameCount());
        if (!SuperResolution.gameIsLoaded || Minecraft.getInstance().level == null) {
            ImGui.separator();
            ImGui.text("Game world not ready.");
        }
        ImGui.end();
    }

    private void drawWorkModeStateWindow(SRWorkModeProvider provider) {
        if (!isWindowVisible("Work Mode State")) {
            return;
        }
        beginWindow("Work Mode State", 24, 280, 420, 220);
        SRWorkModeState state = SRWorkModeManager.getCurrentState();
        InitializationDescription desc = state.initializationDescription();
        ImGui.text("Current Provider: " + (provider == null ? "<none>" : provider.id()));
        ImGui.text("Internal Texture Format: " + state.internalTextureFormat());
        ImGui.text("Motion Vector Preprocess: " + state.motionVectorPreprocessingFunction());
        ImGui.text("Shader Pack In Use: " + state.shaderPackInUse());
        ImGui.text("Shader Pack Loading: " + state.shaderPackLoading());
        ImGui.text("Init HDR Input: " + desc.isHdrInput());
        ImGui.text("Init Auto Exposure: " + desc.isAutoExposure());
        ImGui.text("Init Motion Jittered: " + desc.isMotionJittered());
        ImGui.end();
    }

    private void drawPerformanceWindow() {
        if (!isWindowVisible("Performance")) {
            return;
        }
        beginWindow("Performance", 470, 24, 520, 320);
        boolean gpuEnabled = SuperResolutionConfig.isEnableDetailedProfiling();
        ImGui.text("Detailed GPU Profiling: " + gpuEnabled);

        for (String operation : PERF_OPERATIONS) {
            float cpuMs = nanosToMillis(PerformanceTracker.getLastResultCPU(operation));
            float[] cpuValues = updateHistory(cpuPerfHistory, operation, cpuMs);
            ImGui.text("%s CPU %.3f ms".formatted(operation, cpuMs));
            ImGui.plotLines("CPU##" + operation, cpuValues, cpuValues.length, 0, null, 0.0f, getMaxValue(cpuValues), new ImVec2(460, 48));

            if (gpuEnabled) {
                float gpuMs = nanosToMillis(PerformanceTracker.getLastResultGPU(operation));
                float[] gpuValues = updateHistory(gpuPerfHistory, operation, gpuMs);
                ImGui.text("%s GPU %.3f ms".formatted(operation, gpuMs));
                ImGui.plotHistogram("GPU##" + operation, gpuValues, gpuValues.length, 0, null, 0.0f, getMaxValue(gpuValues), new ImVec2(460, 36));
            } else {
                ImGui.text("%s GPU disabled".formatted(operation));
            }
        }
        ImGui.end();
    }

    private void drawProviderWindow(SRWorkModeProvider provider) {
        if (provider == null || !isWindowVisible("Work Mode Provider")) {
            return;
        }
        beginWindow("Work Mode Provider", 1015, 24, 420, 260);
        provider.renderImGuiDebug(new ImGuiDebugContext(
                "provider",
                textures::add,
                this::openViewer
        ));
        ImGui.end();
    }

    private void drawHandlerWindow(IMinecraftRenderHandler handler) {
        if (handler == null || !isWindowVisible("Render Handler")) {
            return;
        }
        beginWindow("Render Handler", 1015, 310, 420, 260);
        handler.renderImGuiDebug(new ImGuiDebugContext(
                "handler",
                textures::add,
                this::openViewer
        ));
        ImGui.end();
    }

    private void drawTexturesWindow() {
        if (!isWindowVisible("Textures")) {
            return;
        }
        beginWindow("Textures", 470, 370, 965, 410);
        if (textures.isEmpty()) {
            ImGui.text("No textures available.");
            ImGui.end();
            return;
        }

        textures.sort(Comparator.comparing(DebugTextureEntry::category).thenComparing(DebugTextureEntry::label));

        Map<String, List<DebugTextureEntry>> texturesByCategory = new LinkedHashMap<>();
        for (DebugTextureEntry entry : textures) {
            texturesByCategory.computeIfAbsent(entry.category(), ignored -> new ArrayList<>()).add(entry);
        }

        if (ImGui.beginTabBar("textures_categories")) {
            for (Map.Entry<String, List<DebugTextureEntry>> categoryEntry : texturesByCategory.entrySet()) {
                if (ImGui.beginTabItem(categoryEntry.getKey())) {
                    drawTextureGrid(categoryEntry.getValue());
                    ImGui.endTabItem();
                }
            }
            ImGui.endTabBar();
        }
        ImGui.end();
    }

    private void drawAlgorithmWindow() {
        if (!isWindowVisible("Algorithm")) {
            return;
        }
        beginWindow("Algorithm", 24, 525, 420, 255);
        ImGui.text("Current Algorithm: " + getAlgorithmName());
        ImGui.text("Configured Algorithm: " + SuperResolutionConfig.getUpscaleAlgorithm().getDisplayName());
        ImGui.text("Supports Jitter: " + AlgorithmManager.supportsJitter(SuperResolution.algorithmDescription));
        ImGui.text("Jitter Sequence Length: " + AlgorithmManager.getJitterSequenceLength());

        if (SuperResolution.getCurrentAlgorithm() instanceof FSR2 fsr2 && fsr2.fsr2Context != null) {
            drawFsr2Resources(fsr2.fsr2Context);
        } else {
            ImGui.text("FSR2 context unavailable.");
        }
        ImGui.end();
    }

    private void drawFsr2Resources(Fsr2Context context) {
        for (Map.Entry<Fsr2PipelineResourceType, Fsr2PipelineResources.Fsr2ResourceEntry> entry : context.resources.resources().entrySet()) {
            Object resource = entry.getValue().getResource();
            if (!(resource instanceof ITexture texture) || resource instanceof GlBuffer) {
                continue;
            }
            String resourceKey = getFsr2ResourceKey(entry.getKey());
            ImGui.text(resourceKey + ": " + entry.getValue().getDescription().label);
            ImGui.sameLine();
            if (ImGui.smallButton("Open##fsr2-" + resourceKey)) {
                openViewer(new DebugTextureEntry(
                        "fsr2:" + resourceKey,
                        entry.getValue().getDescription().label,
                        texture.handle(),
                        texture.getWidth(),
                        texture.getHeight(),
                        "fsr2",
                        resourceKey,
                        true
                ));
            }
        }
    }

    private void collectBaseTextures() {
        ImGuiDebugContext ctx = new ImGuiDebugContext("base", textures::add, this::openViewer);
        ctx.addTexture("input_color", "Input Color Texture", RenderHandlerManager.getColorTexture(), null, true);
        ctx.addTexture("input_depth", "Input Depth Texture", RenderHandlerManager.getDepthTexture(), null, true);
        if (AlgorithmManager.getMotionVectorsFrameBuffer() != null) {
            ctx.addTexture(
                    "motion_vectors",
                    "Generated Motion Vectors",
                    AlgorithmManager.getMotionVectorsFrameBuffer().getTexture(FrameBufferAttachmentType.Color),
                    null,
                    true
            );
        }
        if (SuperResolution.currentAlgorithm != null && SuperResolution.currentAlgorithm.getOutputFrameBuffer() != null) {
            ctx.addTexture(
                    "upscale_output",
                    "Upscale Output",
                    SuperResolution.currentAlgorithm.getOutputFrameBuffer().getTexture(FrameBufferAttachmentType.Color),
                    null,
                    true
            );
        }
    }

    private void collectProviderTextures(SRWorkModeProvider provider) {
        if (provider == null) {
            return;
        }
        provider.collectDebugTextures(new ImGuiDebugContext(
                "provider",
                textures::add,
                this::openViewer
        ));
    }

    private void collectHandlerTextures(IMinecraftRenderHandler handler) {
        if (handler == null) {
            return;
        }
        handler.collectDebugTextures(new ImGuiDebugContext(
                "handler",
                textures::add,
                this::openViewer
        ));
    }

    private void collectAlgorithmTextures() {
        if (SuperResolutionConfig.getUpscaleAlgorithm() != AlgorithmDescriptions.FSR2
                || !(SuperResolution.getCurrentAlgorithm() instanceof FSR2 fsr2)
                || fsr2.fsr2Context == null) {
            return;
        }
        ImGuiDebugContext ctx = new ImGuiDebugContext("fsr2", textures::add, this::openViewer);
        for (Map.Entry<Fsr2PipelineResourceType, Fsr2PipelineResources.Fsr2ResourceEntry> entry : fsr2.fsr2Context.resources.resources().entrySet()) {
            Object resource = entry.getValue().getResource();
            if (!(resource instanceof ITexture texture) || resource instanceof GlBuffer) {
                continue;
            }
            String resourceKey = getFsr2ResourceKey(entry.getKey());
            ctx.addTexture(resourceKey, entry.getValue().getDescription().label, texture, resourceKey, true);
        }
    }

    private void drawCaptureButtons() {
        if (ImGui.button("Capture")) {
            SuperResolutionAPI.debugRenderdocCapture();
        }
        ImGui.sameLine();
        if (ImGui.button("CaptureUpscale")) {
            SuperResolutionAPI.debugRenderdocCaptureUpscale();
        }
        ImGui.sameLine();
        if (ImGui.button("CaptureVulkan")) {
            SuperResolutionAPI.debugRenderdocCaptureVulkan();
        }
        ImGui.sameLine();
        if (ImGui.button("TriggerCapture")) {
            SuperResolutionAPI.debugRenderdocTriggerCapture();
        }
    }

    private void drawTextureCard(DebugTextureEntry entry) {
        ImGui.pushID(entry.id());
        ImGui.pushTextWrapPos(ImGui.getCursorScreenPosX() + PREVIEW_WIDTH);
        ImGui.textWrapped(entry.label() + " (" + entry.width() + "x" + entry.height() + ")");
        ImGui.popTextWrapPos();
        float previewHeight = entry.previewHeight(PREVIEW_WIDTH);
        if (entry.textureId() != 0L) {
            ImGui.image(entry.textureId(), PREVIEW_WIDTH, previewHeight, 0.0f, 1.0f, 1.0f, 0.0f);
            if (ImGui.isItemClicked()) {
                openViewer(entry);
            }
            if (ImGui.isItemHovered()) {
                ImGui.beginTooltip();
                ImGui.text("Click to open interactive viewer");
                if (entry.notes() != null && !entry.notes().isBlank()) {
                    ImGui.text(entry.notes());
                }
                ImGui.endTooltip();
            }
        }
        if (entry.notes() != null && !entry.notes().isBlank()) {
            ImGui.pushTextWrapPos(ImGui.getCursorScreenPosX() + PREVIEW_WIDTH);
            ImGui.textWrapped(entry.notes());
            ImGui.popTextWrapPos();
        }
        if (ImGui.smallButton("Open Viewer")) {
            openViewer(entry);
        }
        ImGui.popID();
    }

    private void drawTextureGrid(List<DebugTextureEntry> entries) {
        float availableWidth = ImGui.getContentRegionAvailX();
        int columnCount = Math.max(1, (int) ((availableWidth + TEXTURE_GRID_SPACING) / (PREVIEW_WIDTH + TEXTURE_GRID_SPACING)));
        if (columnCount <= 1) {
            for (DebugTextureEntry entry : entries) {
                drawTextureCard(entry);
            }
            return;
        }

        ImGui.columns(columnCount, "textures_grid_columns", false);
        for (DebugTextureEntry entry : entries) {
            drawTextureCard(entry);
            ImGui.nextColumn();
        }
        ImGui.columns(1);
    }

    private void openViewer(DebugTextureEntry entry) {
        if (entry == null || !entry.isValid()) {
            return;
        }
        String viewerId = entry.id() + "##viewer";
        for (ViewerInstance viewer : viewers) {
            if (viewer.state.windowId.equals(viewerId)) {
                viewer.entry = entry;
                viewer.state.open.set(true);
                viewer.state.viewReset = true;
                viewer.state.fitToWindow = true;
                return;
            }
        }
        viewers.add(new ViewerInstance(entry, new ImGuiImageViewerState(viewerId)));
    }

    private void drawViewerWindows() {
        viewers.removeIf(viewer -> !viewer.state.open.get());
        for (int i = 0; i < viewers.size(); i++) {
            ViewerInstance viewer = viewers.get(i);
            if (!viewer.entry.isValid()) {
                continue;
            }
            ImGui.setNextWindowSize(900, 720, ImGuiCond.FirstUseEver);
            ImGui.setNextWindowPos(200 + i * 30.0f, 120 + i * 30.0f, ImGuiCond.FirstUseEver);
            String title = "Texture Viewer - " + viewer.entry.label() + " (" + viewer.entry.width() + "x" + viewer.entry.height() + ")##" + viewer.state.windowId;
            if (!ImGui.begin(title, viewer.state.open, ImGuiWindowFlags.HorizontalScrollbar)) {
                ImGui.end();
                continue;
            }
            drawViewerToolbar(viewer);
            ImVec2 available = ImGui.getContentRegionAvail();
            drawViewerCanvas(viewer, new ImVec2(Math.max(available.x, 32.0f), Math.max(available.y, 32.0f)));
            ImGui.end();
        }
    }

    private void drawViewerToolbar(ViewerInstance viewer) {
        ImGui.text("Category: " + viewer.entry.category());
        ImGui.sameLine();
        ImGui.text("Zoom: %.0f%%".formatted(viewer.state.zoom * 100.0f));
        if (ImGui.smallButton("Reset##" + viewer.state.windowId)) {
            viewer.state.viewReset = true;
            viewer.state.fitToWindow = true;
        }
        ImGui.sameLine();
        if (ImGui.smallButton("Fit##" + viewer.state.windowId)) {
            viewer.state.viewReset = true;
            viewer.state.fitToWindow = true;
        }
        ImGui.sameLine();
        if (ImGui.smallButton("100%##" + viewer.state.windowId)) {
            viewer.state.zoom = 1.0f;
            viewer.state.viewReset = true;
            viewer.state.fitToWindow = false;
        }
        ImGui.sameLine();
        if (ImGui.smallButton("50%##" + viewer.state.windowId)) {
            viewer.state.zoom = 0.5f;
            viewer.state.viewReset = true;
            viewer.state.fitToWindow = false;
        }
        ImGui.sameLine();
        if (ImGui.smallButton(viewer.state.gridEnabled ? "Grid: On##" + viewer.state.windowId : "Grid: Off##" + viewer.state.windowId)) {
            viewer.state.gridEnabled = !viewer.state.gridEnabled;
        }
    }

    private void drawViewerCanvas(ViewerInstance viewer, ImVec2 canvasSize) {
        ImGuiIO io = ImGui.getIO();
        ImDrawList drawList = ImGui.getWindowDrawList();

        ImGui.invisibleButton("##TextureViewerCanvas" + viewer.state.windowId, canvasSize.x, canvasSize.y);
        ImVec2 canvasMin = ImGui.getItemRectMin();
        ImVec2 canvasMax = ImGui.getItemRectMax();
        float canvasWidth = canvasMax.x - canvasMin.x;
        float canvasHeight = canvasMax.y - canvasMin.y;

        if (viewer.state.viewReset) {
            if (viewer.state.fitToWindow) {
                float fitX = canvasWidth / Math.max(1.0f, viewer.entry.width());
                float fitY = canvasHeight / Math.max(1.0f, viewer.entry.height());
                viewer.state.zoom = clamp(Math.min(fitX, fitY), viewer.state.zoomMin, viewer.state.zoomMax);
            }
            float imageWidth = viewer.entry.width() * viewer.state.zoom;
            float imageHeight = viewer.entry.height() * viewer.state.zoom;
            viewer.state.viewOffset.set(
                    (canvasWidth - imageWidth) * 0.5f,
                    (canvasHeight - imageHeight) * 0.5f
            );
            viewer.state.viewReset = false;
        }

        if (ImGui.isItemHovered(ImGuiHoveredFlags.RectOnly)) {
            float wheel = io.getMouseWheel();
            if (wheel != 0.0f) {
                float oldZoom = viewer.state.zoom;
                float newZoom = clamp(
                        oldZoom * (1.0f + wheel * 0.10f),
                        viewer.state.zoomMin,
                        viewer.state.zoomMax
                );
                if (Math.abs(newZoom - oldZoom) > 0.0001f) {
                    float mouseLocalX = ImGui.getMousePosX() - canvasMin.x;
                    float mouseLocalY = ImGui.getMousePosY() - canvasMin.y;
                    float texelX = (mouseLocalX - viewer.state.viewOffset.x) / oldZoom;
                    float texelY = (mouseLocalY - viewer.state.viewOffset.y) / oldZoom;
                    viewer.state.zoom = newZoom;
                    viewer.state.viewOffset.x = mouseLocalX - texelX * newZoom;
                    viewer.state.viewOffset.y = mouseLocalY - texelY * newZoom;
                }
                viewer.state.fitToWindow = false;
            }
        }

        if (ImGui.isItemActive() && ImGui.isMouseDragging(0)) {
            viewer.state.viewOffset.x += io.getMouseDeltaX();
            viewer.state.viewOffset.y += io.getMouseDeltaY();
            viewer.state.fitToWindow = false;
        }

        float imageWidth = viewer.entry.width() * viewer.state.zoom;
        float imageHeight = viewer.entry.height() * viewer.state.zoom;
        float imageMinX = canvasMin.x + viewer.state.viewOffset.x;
        float imageMinY = canvasMin.y + viewer.state.viewOffset.y;
        float imageMaxX = imageMinX + imageWidth;
        float imageMaxY = imageMinY + imageHeight;

        drawList.addRect(canvasMin.x - 1.0f, canvasMin.y - 1.0f, canvasMax.x + 1.0f, canvasMax.y + 1.0f, ImGuiDebugColors.VIEWER_BORDER);
        drawList.pushClipRect(canvasMin, canvasMax, true);
        drawList.addRectFilled(canvasMin.x, canvasMin.y, canvasMax.x, canvasMax.y, ImGuiDebugColors.VIEWER_BACKGROUND);
        drawList.addImage(viewer.entry.textureId(), imageMinX, imageMinY, imageMaxX, imageMaxY, 0.0f, 1.0f, 1.0f, 0.0f);
        if (viewer.state.gridEnabled && viewer.state.zoom > 6.0f) {
            drawViewerGrid(drawList, canvasMin, canvasMax, imageMinX, imageMinY, viewer.entry.width(), viewer.entry.height(), viewer.state.zoom);
        }
        drawList.popClipRect();
    }

    private void drawViewerGrid(ImDrawList drawList, ImVec2 canvasMin, ImVec2 canvasMax, float imageMinX, float imageMinY, int textureWidth, int textureHeight, float zoom) {
        int visibleXStart = Math.max(0, (int) Math.floor((canvasMin.x - imageMinX) / zoom));
        int visibleXEnd = Math.min(textureWidth, (int) Math.ceil((canvasMax.x - imageMinX) / zoom));
        for (int px = visibleXStart; px <= visibleXEnd; px++) {
            float x = imageMinX + px * zoom;
            drawList.addLine(x, canvasMin.y, x, canvasMax.y, ImGuiDebugColors.VIEWER_GRID);
        }

        int visibleYStart = Math.max(0, (int) Math.floor((canvasMin.y - imageMinY) / zoom));
        int visibleYEnd = Math.min(textureHeight, (int) Math.ceil((canvasMax.y - imageMinY) / zoom));
        for (int py = visibleYStart; py <= visibleYEnd; py++) {
            float y = imageMinY + py * zoom;
            drawList.addLine(canvasMin.x, y, canvasMax.x, y, ImGuiDebugColors.VIEWER_GRID);
        }
    }

    private float[] updateHistory(Map<String, float[]> historyMap, String operation, float currentValue) {
        float[] values = historyMap.computeIfAbsent(operation, ignored -> new float[120]);
        System.arraycopy(values, 1, values, 0, values.length - 1);
        values[values.length - 1] = currentValue;
        return values;
    }

    private float getMaxValue(float[] values) {
        float max = 1.0f;
        for (float value : values) {
            max = Math.max(max, value);
        }
        return max * 1.1f;
    }

    private void beginWindow(String title, float x, float y, float width, float height) {
        ImBoolean open = WINDOW_OPEN.get(title);
        ImGui.setNextWindowPos(x, y, ImGuiCond.FirstUseEver);
        ImGui.setNextWindowSize(width, height, ImGuiCond.FirstUseEver);
        ImGui.begin(title, open);
    }

    private boolean isWindowVisible(String title) {
        return WINDOW_OPEN.computeIfAbsent(title, ignored -> new ImBoolean(true)).get();
    }

    private SRWorkModeProvider safeGetCurrentProvider() {
        try {
            return SRWorkModeManager.getCurrentProvider();
        } catch (IllegalStateException ignored) {
            return null;
        }
    }

    private static float nanosToMillis(long nanos) {
        return nanos / 1_000_000.0f;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String getFsr2ResourceKey(Fsr2PipelineResourceType resourceType) {
        return "resource-" + resourceType.id();
    }

    private String getAlgorithmName() {
        if (SuperResolution.algorithmDescription != null) {
            return SuperResolution.algorithmDescription.getDisplayName();
        }
        return SuperResolution.currentAlgorithm == null ? "<none>" : SuperResolution.currentAlgorithm.getClass().getSimpleName();
    }

    private static final class ViewerInstance {
        private DebugTextureEntry entry;
        private final ImGuiImageViewerState state;

        private ViewerInstance(DebugTextureEntry entry, ImGuiImageViewerState state) {
            this.entry = entry;
            this.state = state;
        }
    }
}

