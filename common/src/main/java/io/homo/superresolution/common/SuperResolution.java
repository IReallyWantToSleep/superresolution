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

package io.homo.superresolution.common;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;
import io.homo.superresolution.api.AbstractAlgorithm;
import io.homo.superresolution.api.InitializationDescription;
import io.homo.superresolution.api.SuperResolutionAPI;
import io.homo.superresolution.api.event.AlgorithmResizeEvent;
import io.homo.superresolution.api.platform.EnvironmentType;
import io.homo.superresolution.api.platform.Platform;
import io.homo.superresolution.api.registry.AlgorithmDescription;
import io.homo.superresolution.api.utils.Requirement;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.debug.imgui.ImguiMain;
import io.homo.superresolution.common.gui.ConfigScreenBuilder;
import io.homo.superresolution.common.minecraft.B3DVulkanBridge;
import io.homo.superresolution.common.minecraft.MinecraftUtils;
import io.homo.superresolution.common.minecraft.MinecraftWindow;
import io.homo.superresolution.common.minecraft.handler.RenderHandlerManager;
import io.homo.superresolution.common.upscale.AlgorithmManager;
import io.homo.superresolution.common.workmode.SRWorkModeManager;
import io.homo.superresolution.common.upscale.none.None;
import io.homo.superresolution.core.NativeLibManager;
import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.SuperResolutionConstants;
import io.homo.superresolution.core.graphics.GraphicsCapabilities;
import io.homo.superresolution.core.graphics.glslang.GlslangShaderCompiler;
import io.homo.superresolution.core.graphics.opengl.GlState;
import io.homo.superresolution.core.gui.MaterialUI;
import io.homo.superresolution.core.impl.Destroyable;
import io.homo.superresolution.core.utils.MessageBox;
import io.homo.superresolution.srapi.SuperResolutionNativeAPI;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class SuperResolution implements Destroyable {
    public static final String MOD_ID = "super_resolution";
    public static final Logger LOGGER = LoggerFactory.getLogger("SuperResolution");
    public static final List<String> INCOMPATIBLE_MODS = ImmutableList.<String>builder()
            .add("resolutioncontrol-plus-plus")
            .add("resolutioncontrol-plus")
            .add("resolutioncontrol")
            .add("renderscale")
            .build();
    private static final Requirement commonRequirement = Requirement.nothing()
            .glMajorVersion(4).glMinorVersion(1);
    public static AbstractAlgorithm currentAlgorithm;
    public static None defaultAlgorithm = new None();
    public static boolean isInit;
    public static boolean isPreInit;
    public static boolean isRenderingInitialized = false;
    public static boolean gameIsLoaded = false;
    public static boolean gameIsStarted = false;
    public static AlgorithmDescription<?> algorithmDescription;
    public static int framebufferWidth = 0;
    public static int framebufferHeight = 0;
    public static int cachedWidth;
    public static int cachedHeight;
    public static Thread renderThread;
    #if MC_VER < MC_26_2
    public static boolean isUsingVulkan = false;
    #else
    public static boolean isUsingVulkan = false;
    #endif
    // 窗口拖拽时每帧触发 resize；算法重建昂贵，去抖到尺寸稳定后执行一次。
    private static final long RESIZE_DEBOUNCE_MS = 120L;
    private static volatile boolean pendingResize = false;
    private static volatile long pendingResizeDeadlineMs = 0L;

    private static Minecraft minecraft = Minecraft.getInstance();
    private static SuperResolution instance;

    static {
        BufferedReader reader = null;
        boolean check = true;
        try {
            reader = Files.newReader(Platform.currentPlatform.getGameFolder().resolve("options.txt").toFile(), StandardCharsets.UTF_8);
        } catch (FileNotFoundException e) {
            isUsingVulkan = false;
            check = false;
        }

        if (check) {
            Map<String, String> options = new HashMap<>();

            try {
                reader.lines().forEach(line -> {
                    try {
                        Iterator<String> iterator = Splitter.on(':').limit(2).split(line).iterator();
                        options.put((String) iterator.next(), (String) iterator.next());
                    } catch (Exception var3) {
                    }
                });
            } catch (Throwable var6) {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (Throwable var5) {
                        var6.addSuppressed(var5);
                    }
                }

                throw var6;
            }

            if (options.get("preferredGraphicsBackend") != null) {
                isUsingVulkan = options.get("preferredGraphicsBackend").toLowerCase(Locale.ROOT).contains("vulkan");
            } else {
                isUsingVulkan = false;
            }
        }
    }

    public SuperResolution() {
        instance = this;
        if (minecraft == null) {
            minecraft = Minecraft.getInstance();
        }
    }

    public static void onGameLoadFinished() {
        SuperResolution.createAlgorithm();
    }

    public static void onClientStarted() {
        if (gameIsStarted) {
            SuperResolution.LOGGER.warn("似乎有什么东西在重复初始化SR");
            return;
        }
        SuperResolutionConfig.SPEC.load();
        gameIsStarted = true;
        SuperResolutionKeyMapping.registerKeyMapping();
        instance = new SuperResolution();
        SuperResolution.check();
        SuperResolution.preInit();
        SuperResolution.initRendering();
        SuperResolution.getInstance().init();
        MaterialUI.init();
        if (Platform.currentPlatform.isInstallIris() && !B3DVulkanBridge.isB3DVulkanBackend()) {
            try {
                Class.forName("net.irisshaders.iris.Iris").getMethod("reload").invoke(null);
            } catch (Exception e) {
                throw new RuntimeException(e);

            }
        }
    }

    public static void onClientStopping() {
        SuperResolution.getInstance().destroy();
    }

    public static void onClientSetup() {
        SuperResolutionKeyMapping.registerKeyMapping();
        SRWorkModeManager.onClientSetup();
    }

    public static void onClientTickEnd() {
        while (SuperResolutionKeyMapping.OPENGUI_KEYMAPPING.consumeClick()) {
            MinecraftUtils.setScreen(
                    ConfigScreenBuilder.create().buildConfigScreen(MinecraftUtils.getScreen())
            );
        }
    }

    public static void registerEvents() {
    }

    public static void preInit() {
        if (isPreInit) {
            return;
        }
        if (minecraft == null) {
            minecraft = Minecraft.getInstance();
        }
        if (Platform.currentPlatform.getEnv() == EnvironmentType.SERVER) {
            throw new RuntimeException("SuperResolution不支持安装在服务器上！");
        }
        NativeLibManager.extract(SuperResolutionConstants.NATIVE_LIBRARIES_DIR.getPath());
        NativeLibManager.load(SuperResolutionConstants.NATIVE_LIBRARIES_DIR.getPath());
        GlslangShaderCompiler.init();
        isPreInit = true;
    }

    public static SuperResolution getInstance() {
        return instance;
    }

    public static void check() {
        if (minecraft == null) {
            minecraft = Minecraft.getInstance();
        }

        boolean uiOnlyB3DVulkan = B3DVulkanBridge.isB3DVulkanBackend();
        if (!uiOnlyB3DVulkan) {
            if (!commonRequirement.check().glVersionMet()) {
                MessageBox.createError(
                        Component.translatable("superresolution.common_requirement.not_support.version").getString().formatted(
                                commonRequirement.getGlMajorVersion(),
                                commonRequirement.getGlMinorVersion(),
                                GraphicsCapabilities.getGLVersion()[0],
                                GraphicsCapabilities.getGLVersion()[1]),
                        Component.translatable("superresolution.common_requirement.not_support.msg").getString()
                );
                System.exit(1);
            }

            if (!commonRequirement.check().glExtensionsPresent()) {
                StringBuilder extensionStringBuilder = new StringBuilder();
                for (String name : commonRequirement.getMissingGlExtensions()) {
                    extensionStringBuilder.append(name).append("\n");
                }
                MessageBox.createError(Component.translatable("superresolution.common_requirement.not_support.extension").getString()
                                .formatted(extensionStringBuilder.toString()),
                        Component.translatable("superresolution.common_requirement.not_support.msg").getString()
                );
                System.exit(1);
            }
        }
        INCOMPATIBLE_MODS.forEach((mod) -> {
            List<String> installedMods = new ArrayList<>();
            if (Platform.currentPlatform.isModLoaded(mod)) {
                installedMods.add(mod);
            }
            if (!installedMods.isEmpty()) {
                MessageBox.createError(Component.translatable("superresolution.common_requirement.not_support.extension").getString()
                                .formatted(String.join("\n", installedMods)),
                        Component.translatable("superresolution.common_requirement.not_support.msg").getString()
                );
            }
        });
    }

    public static void initRendering() {
        renderThread = Thread.currentThread();
        if (B3DVulkanBridge.isB3DVulkanBackend()) {
            if (minecraft == null) {
                minecraft = Minecraft.getInstance();
            }
            if (!isPreInit) {
                return;
            }
            if (!RenderSystems.initBorrowedB3DVulkanIfAvailable()) {
                throw new RuntimeException("初始化失败");
            }
            SRWorkModeManager.bootstrapProviders();
            RenderHandlerManager.initialize();
            isRenderingInitialized = true;
            algorithmDescription = SuperResolutionConfig.getUpscaleAlgorithm();
            return;
        }
        try (GlState ignored = new GlState()) {
            RenderSystems.init();

            if (minecraft == null) {
                minecraft = Minecraft.getInstance();
            }
            if (!isPreInit) {
                return;
            }

            LOGGER.info("显卡供应商 {}", GraphicsCapabilities.detectGpuVendor().name());
            LOGGER.info("OpenGL版本 {}", GraphicsCapabilities.getGLVersionString());

            SRWorkModeManager.bootstrapProviders();
            RenderHandlerManager.initialize();
            AlgorithmManager.init();
            isRenderingInitialized = true;
            algorithmDescription = SuperResolutionConfig.getUpscaleAlgorithm();
        }
    }

    public static boolean createAlgorithm() {
        return createAlgorithm(getInitializationDescription());
    }

    public static boolean createAlgorithm(InitializationDescription desc) {
        if (B3DVulkanBridge.isB3DVulkanBackend()) {
            algorithmDescription = SuperResolutionConfig.getUpscaleAlgorithm();
            currentAlgorithm = null;
            return true;
        }
        try (GlState ignored = new GlState()) {
            if (minecraft == null) {
                minecraft = Minecraft.getInstance();
            }
            if (!isPreInit) {
                return false;
            }
            defaultAlgorithm.initialize();
            algorithmDescription = SuperResolutionConfig.getUpscaleAlgorithm();
            try {
                currentAlgorithm = algorithmDescription.createNewInstance();
                currentAlgorithm.initialize(desc);
                SuperResolution.LOGGER.info("初始化算法 {}", algorithmDescription.getDisplayName());
                return true;
            } catch (Exception e) {
                SuperResolution.LOGGER.info("初始化算法 {} 时失败 错误:", algorithmDescription.getDisplayName());
                LOGGER.error("初始化算法失败详情", e);
                if (currentAlgorithm != null) {
                    try { currentAlgorithm.destroy(); } catch (Exception ignored2) { }
                }
                currentAlgorithm = null;
            }
        }

        return false;
    }

    // Signature of the last successful (re)create, used to skip redundant Iris pipeline reloads.
    private static InitializationDescription lastAppliedDesc;
    private static AlgorithmDescription<?> lastAppliedAlgorithm;

    public static boolean recreateAlgorithm() {
        return recreateAlgorithm(getInitializationDescription());
    }

    /**
     * Recreate the algorithm only if the effective configuration actually changed. Iris fires its reload
     * hooks on every pipeline reload -- including no-op "overworld => overworld" dimension changes -- and
     * an unconditional rebuild tears down and recreates the DLSS/NGX context + interop resources each
     * time (a recreate-storm that stalls world loading, badly in HighPerformance mode). Config changes
     * use the unguarded recreateAlgorithm(), so they always apply even when the description is unchanged.
     */
    public static boolean recreateAlgorithmIfChanged() {
        InitializationDescription desc = getInitializationDescription();
        //if (currentAlgorithm != null
        //        && algorithmDescription == lastAppliedAlgorithm
        //        && desc.equals(lastAppliedDesc)) {
        //    return true;
        //}
        return recreateAlgorithm(desc);
    }

    public static boolean recreateAlgorithm(InitializationDescription desc) {
        if (B3DVulkanBridge.isB3DVulkanBackend()) {
            if (currentAlgorithm != null) {
                currentAlgorithm = null;
            }
            algorithmDescription = SuperResolutionConfig.getUpscaleAlgorithm();
            lastAppliedDesc = null;
            lastAppliedAlgorithm = null;
            return true;
        }
        try (GlState ignored = new GlState()) {
            if (minecraft == null) {
                minecraft = Minecraft.getInstance();
            }
            if (!isPreInit) {
                return false;
            }

            if (currentAlgorithm != null) {
                currentAlgorithm.destroy();
            }

            try {
                currentAlgorithm = algorithmDescription.createNewInstance();
                currentAlgorithm.initialize(desc);
                lastAppliedDesc = desc;
                lastAppliedAlgorithm = algorithmDescription;
                return true;
            } catch (Exception e) {
                LOGGER.error("初始化算法 {} 时失败：", algorithmDescription.getDisplayName(), e);
                if (currentAlgorithm != null) {
                    try { currentAlgorithm.destroy(); } catch (Exception ignored2) { }
                }
                currentAlgorithm = null;
                lastAppliedDesc = null;
                lastAppliedAlgorithm = null;
            }
        }
        return false;
    }

    public static AbstractAlgorithm getCurrentAlgorithm() {
        if (SuperResolutionConfig.isEnableUpscale() && currentAlgorithm != null) {
            return currentAlgorithm;
        }
        return defaultAlgorithm;
    }

    public static InitializationDescription getInitializationDescription() {
        return SRWorkModeManager.getCurrentState().initializationDescription();
    }

    public void init() {
        if (minecraft == null) {
            minecraft = Minecraft.getInstance();
        }

        if (isInit) {
            return;
        }
        if (Platform.currentPlatform.isDevelopmentEnvironment() && SuperResolutionConfig.isEnableImgui()) {
            new ImguiMain();
        }

        isInit = true;
        if (!B3DVulkanBridge.isB3DVulkanBackend()) {
            this.resize(MinecraftWindow.getWindowWidth(), MinecraftWindow.getWindowHeight());
        }
    }

    public void resize(int width, int height) {
        if (width == cachedWidth && height == cachedHeight && !pendingResize) {
            return;
        }
        // 立刻更新 cached 让上游比较立即等价，实际重建交给 tickResize 去抖后执行。
        cachedWidth = width;
        cachedHeight = height;
        pendingResize = true;
        pendingResizeDeadlineMs = System.currentTimeMillis() + RESIZE_DEBOUNCE_MS;
    }

    public void forceResize(int width, int height) {
        cachedWidth = width;
        cachedHeight = height;
        pendingResize = false;
        SuperResolution self = getInstance();
        if (self != null) {
            self.applyPendingResize();
        }
    }

    /** 每帧调用；尺寸稳定 RESIZE_DEBOUNCE_MS 后才真正重建算法。 */
    public static void tickResize() {
        if (!pendingResize) return;
        if (System.currentTimeMillis() < pendingResizeDeadlineMs) return;
        pendingResize = false;
        SuperResolution self = getInstance();
        if (self != null) {
            self.applyPendingResize();
        }
    }

    private void applyPendingResize() {
        if (B3DVulkanBridge.isB3DVulkanBackend()) {
            return;
        }
        int w = MinecraftWindow.getWindowWidth();
        int h = MinecraftWindow.getWindowHeight();
        w = Math.max(32,w) ;
        h = Math.max(32,h);
        if (currentAlgorithm != null && SuperResolutionConfig.isEnableUpscaleOriginal()) {
            SuperResolutionAPI.EVENT_BUS.post(
                    new AlgorithmResizeEvent(
                            currentAlgorithm,
                            RenderHandlerManager.getScreenWidth(),
                            RenderHandlerManager.getScreenHeight(),
                            RenderHandlerManager.getRenderWidth(),
                            RenderHandlerManager.getRenderHeight()
                    )
            );
            currentAlgorithm.resize(
                    w,
                    h
            );
            // 分辨率变了，时序历史无效。
            currentAlgorithm.invalidateHistory();
        }
        AlgorithmManager.resize(w, h);
    }

    public void destroy() {
        isInit = false;
        isRenderingInitialized = false;
        pendingResize = false;
        if (currentAlgorithm != null) {
            currentAlgorithm.destroy();
            currentAlgorithm = null;
        }
        if (!B3DVulkanBridge.isB3DVulkanBackend()) {
            AlgorithmManager.destroy();
        }
        SuperResolutionNativeAPI.srShutdown();
        RenderSystems.destroy();
    }
}
