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

import com.google.common.collect.ImmutableList;
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
import io.homo.superresolution.common.framegeneration.FrameGeneration;
import io.homo.superresolution.common.gui.ConfigScreenBuilder;
import io.homo.superresolution.common.lowlatency.LowLatency;
import io.homo.superresolution.common.minecraft.B3DVulkanBridge;
import io.homo.superresolution.common.minecraft.MinecraftUtils;
import io.homo.superresolution.common.minecraft.handler.RenderHandlerManager;
import io.homo.superresolution.common.optiscaler.OptiScalerLoader;
import io.homo.superresolution.common.presentation.DLSSNRPostProcessor;
import io.homo.superresolution.common.presentation.vulkan.VulkanPresentationFeature;
import io.homo.superresolution.common.presentation.vulkan.VulkanPresentationWindow;
import io.homo.superresolution.common.presentation.window.PresentationWindowState;
import io.homo.superresolution.common.upscale.AlgorithmManager;
import io.homo.superresolution.common.workmode.SRWorkModeManager;
import io.homo.superresolution.common.upscale.algo.none.None;
import io.homo.superresolution.core.NativeLibManager;
import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.SuperResolutionConstants;
import io.homo.superresolution.core.graphics.GraphicsCapabilities;
import io.homo.superresolution.core.graphics.glslang.GlslangShaderCompiler;
import io.homo.superresolution.core.graphics.opengl.GlState;
import io.homo.superresolution.core.gui.MaterialUI;
import io.homo.superresolution.core.impl.Destroyable;
import io.homo.superresolution.core.ngx.NgxInitializer;
import io.homo.superresolution.core.streamline.Streamline;
import io.homo.superresolution.core.utils.MessageBox;
import io.homo.superresolution.srapi.SuperResolutionNativeAPI;
import io.homo.superresolution.srapi.SRReturnCode;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

#if MC_VER > MC_26_1_2
import java.io.*;
import com.google.common.io.Files;
import com.google.common.base.*;
import java.nio.charset.StandardCharsets;
#endif

public final class SuperResolution implements Destroyable {
    public static final String MOD_ID = "super_resolution";
    public static final Logger LOGGER = LoggerFactory.getLogger("SuperResolution");
    public static final List<String> INCOMPATIBLE_MODS = ImmutableList.<String>builder()
            .add("resolutioncontrol-plus-plus")
            .add("resolutioncontrol-plus")
            .add("resolutioncontrol")
            .add("renderscale")
            .add("gpu_booster")
            .add("gpu_tape")
            .build();
    private static final Requirement commonRequirement = Requirement.nothing()
            .glMajorVersion(4).glMinorVersion(1);
    public static AbstractAlgorithm currentAlgorithm;
    private static final ArrayList<AbstractAlgorithm> retainedAlgorithmOwners = new ArrayList<>(4);
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
    // Guards the interop OpenGL context + Vulkan device teardown so it runs once per
    // shutdown, whether triggered early (no presentation) or deferred to destroy() TAIL.
    private static boolean graphicsBackendDestroyed = false;

    private static Minecraft minecraft = Minecraft.getInstance();
    private static SuperResolution instance;

    static {
        #if MC_VER > MC_26_1_2
        BufferedReader reader = null;
        boolean check = true;
        try {
            reader = Files.newReader(Platform.currentPlatform.getGameFolder().resolve("options.txt").toFile(), StandardCharsets.UTF_8);
        } catch (FileNotFoundException e) {
            check = false;
        }

        if (check) {
            Map<String, String> options = new HashMap<>();

            try {
                reader.lines().forEach(line -> {
                    try {
                        Iterator<String> iterator = Splitter.on(':').limit(2).split(line).iterator();
                        options.put(iterator.next(), iterator.next());
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
        #endif
    }

    public SuperResolution() {
        instance = this;
        if (minecraft == null) {
            minecraft = Minecraft.getInstance();
        }

        OptiScalerLoader.loadConfiguredDll();
        if (VulkanPresentationFeature.shouldInitializeStreamline() && !Streamline.prepareEarly()) {
            LOGGER.warn("Streamline is unavailable; falling back to non-Streamline backends.");
        }
    }

    public static void onGameLoadFinished() {
        SuperResolution.createAlgorithm();
    }

    public static void onClientStarted() {
        if (gameIsStarted) {
            SuperResolution.LOGGER.warn("Super Resolution appears to be initialized more than once");
            return;
        }
        SuperResolutionConfig.SPEC.load();
        gameIsStarted = true;
        instance = new SuperResolution();
        SuperResolution.check();
        SuperResolution.preInit();
        Streamline.prepareEarly();
        SuperResolution.initRendering();
        SuperResolution.getInstance().init();
        MaterialUI.init();
        FrameGeneration.initialize();
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

    public static void onClientStopped() {
        SuperResolution.getInstance().destroyGraphicsBackend();
    }

    public static void onClientSetup() {
        SuperResolutionConfig.SPEC.load();
        SuperResolutionConfig.freezeStartupOptions();
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
            throw new RuntimeException("SuperResolution does not support installation on a dedicated server!");
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
                throw new RuntimeException("Initialization failed");
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

            LOGGER.info("GPU vendor: {}", GraphicsCapabilities.detectGpuVendor().name());
            LOGGER.info("OpenGL version: {}", GraphicsCapabilities.getGLVersionString());

            SRWorkModeManager.bootstrapProviders();
            RenderHandlerManager.initialize();
            AlgorithmManager.init();
            isRenderingInitialized = true;
            algorithmDescription = SuperResolutionConfig.getUpscaleAlgorithm();
            DLSSNRPostProcessor.initialize();
        }
    }

    public static boolean createAlgorithm() {
        return createAlgorithm(getInitializationDescription());
    }

    public static boolean createAlgorithm(InitializationDescription desc) {
        if (B3DVulkanBridge.isB3DVulkanBackend()) {
            Throwable retainedFailure = retryRetainedAlgorithmOwners(null);
            if (retainedFailure != null) {
                LOGGER.error(
                        "Could not release a previously failed algorithm before " +
                                "switching to the borrowed Vulkan backend",
                        retainedFailure);
                rethrowAlgorithmError(retainedFailure);
                return false;
            }
            reserveAlgorithmOwnerCapacity();
            AbstractAlgorithm previous = currentAlgorithm;
            if (previous != null) {
                try {
                    previous.destroy();
                    currentAlgorithm = null;
                } catch (Throwable failure) {
                    retainAlgorithmOwner(previous);
                    currentAlgorithm = null;
                    LOGGER.error(
                            "Failed to destroy the current algorithm before " +
                                    "switching to the borrowed Vulkan backend",
                            failure);
                    rethrowAlgorithmError(failure);
                    return false;
                }
            }
            algorithmDescription = SuperResolutionConfig.getUpscaleAlgorithm();
            currentAlgorithm = null;
            return true;
        }
        AbstractAlgorithm previous = currentAlgorithm;
        AbstractAlgorithm candidate = null;
        AlgorithmDescription<?> attemptedDescription = null;
        try (GlState ignored = new GlState()) {
            if (minecraft == null) {
                minecraft = Minecraft.getInstance();
            }
            if (!isPreInit) {
                return false;
            }
            Throwable retainedFailure = retryRetainedAlgorithmOwners(null);
            if (retainedFailure != null) {
                LOGGER.error(
                        "Could not release a previously failed algorithm; " +
                                "refusing to create another one",
                        retainedFailure);
                rethrowAlgorithmError(retainedFailure);
                return false;
            }
            reserveAlgorithmOwnerCapacity();
            defaultAlgorithm.initialize();
            attemptedDescription = SuperResolutionConfig.getUpscaleAlgorithm();
            algorithmDescription = attemptedDescription;
            previous = currentAlgorithm;
            candidate = algorithmDescription.createNewInstance();
            candidate.initialize(desc);
            currentAlgorithm = candidate;
            // 算法创建时已按当前尺寸初始化，同步尺寸缓存避免渲染路径上重复重建。
            cachedWidth = RenderHandlerManager.getScreenWidth();
            cachedHeight = RenderHandlerManager.getScreenHeight();
            SuperResolution.LOGGER.info("Initializing algorithm {}", algorithmDescription.getDisplayName());
            return true;
        } catch (Throwable failure) {
            if (currentAlgorithm == candidate) {
                currentAlgorithm = previous;
            }
            failure = destroyOrRetainAlgorithm(candidate, failure);
            String displayName = attemptedDescription != null
                    ? attemptedDescription.getDisplayName()
                    : "unknown";
            SuperResolution.LOGGER.info("Failed to initialize algorithm {}:", displayName);
            LOGGER.error("Algorithm initialization failure details", failure);
            rethrowAlgorithmError(failure);
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
            Throwable retainedFailure = retryRetainedAlgorithmOwners(null);
            if (retainedFailure != null) {
                LOGGER.error(
                        "Could not release a previously failed algorithm before " +
                                "rebuilding the borrowed Vulkan backend",
                        retainedFailure);
                rethrowAlgorithmError(retainedFailure);
                return false;
            }
            reserveAlgorithmOwnerCapacity();
            AbstractAlgorithm previous = currentAlgorithm;
            if (previous != null) {
                try {
                    previous.destroy();
                } catch (Throwable failure) {
                    retainAlgorithmOwner(previous);
                    currentAlgorithm = null;
                    lastAppliedDesc = null;
                    lastAppliedAlgorithm = null;
                    LOGGER.error(
                            "Failed to destroy the current algorithm before " +
                                    "rebuilding the borrowed Vulkan backend",
                            failure);
                    rethrowAlgorithmError(failure);
                    return false;
                }
            }
            currentAlgorithm = null;
            algorithmDescription = SuperResolutionConfig.getUpscaleAlgorithm();
            lastAppliedDesc = null;
            lastAppliedAlgorithm = null;
            return true;
        }
        AbstractAlgorithm candidate = null;
        AlgorithmDescription<?> attemptedDescription = null;
        try (GlState ignored = new GlState()) {
            if (minecraft == null) {
                minecraft = Minecraft.getInstance();
            }
            if (!isPreInit) {
                return false;
            }

            Throwable retainedFailure = retryRetainedAlgorithmOwners(null);
            if (retainedFailure != null) {
                LOGGER.error(
                        "Could not release a previously failed algorithm; " +
                                "refusing to rebuild",
                        retainedFailure);
                rethrowAlgorithmError(retainedFailure);
                return false;
            }
            reserveAlgorithmOwnerCapacity();

            if (currentAlgorithm != null) {
                VulkanPresentationWindow.flushCapturedFrame();
                FrameGeneration.invalidateHistory();
                AbstractAlgorithm previous = currentAlgorithm;
                try {
                    previous.destroy();
                    currentAlgorithm = null;
                } catch (Throwable failure) {
                    retainAlgorithmOwner(previous);
                    currentAlgorithm = null;
                    lastAppliedDesc = null;
                    lastAppliedAlgorithm = null;
                    LOGGER.error(
                            "Failed to destroy the current algorithm; " +
                                    "the owner was retained for a later retry",
                            failure);
                    rethrowAlgorithmError(failure);
                    return false;
                }
                LowLatency.onDestructiveRebuild();
            }

            attemptedDescription = SuperResolutionConfig.getUpscaleAlgorithm();
            algorithmDescription = attemptedDescription;
            candidate = algorithmDescription.createNewInstance();
            candidate.initialize(desc);
            currentAlgorithm = candidate;
            cachedWidth = RenderHandlerManager.getScreenWidth();
            cachedHeight = RenderHandlerManager.getScreenHeight();
            lastAppliedDesc = desc;
            lastAppliedAlgorithm = algorithmDescription;
            return true;
        } catch (Throwable failure) {
            if (currentAlgorithm == candidate) {
                currentAlgorithm = null;
            }
            failure = destroyOrRetainAlgorithm(candidate, failure);
            String displayName = attemptedDescription != null
                    ? attemptedDescription.getDisplayName()
                    : "unknown";
            LOGGER.error("Failed to initialize algorithm {}:", displayName, failure);
            lastAppliedDesc = null;
            lastAppliedAlgorithm = null;
            rethrowAlgorithmError(failure);
        }
        return false;
    }

    public static void retainAlgorithmForDestroyRetry(
            AbstractAlgorithm algorithm,
            Throwable failure) {
        if (algorithm == null) {
            return;
        }
        retainAlgorithmOwner(algorithm);
        LOGGER.error(
                "Algorithm destruction failed; retaining its owner for a later retry",
                failure);
    }

    private static Throwable destroyOrRetainAlgorithm(
            AbstractAlgorithm algorithm,
            Throwable failure) {
        if (algorithm == null) {
            return failure;
        }
        try {
            algorithm.destroy();
        } catch (Throwable cleanupFailure) {
            retainAlgorithmOwner(algorithm);
            failure = appendAlgorithmFailure(failure, cleanupFailure);
        }
        return failure;
    }

    private static void retainAlgorithmOwner(AbstractAlgorithm algorithm) {
        for (int index = 0; index < retainedAlgorithmOwners.size(); index++) {
            if (retainedAlgorithmOwners.get(index) == algorithm) {
                return;
            }
        }
        retainedAlgorithmOwners.add(algorithm);
    }

    private static void reserveAlgorithmOwnerCapacity() {
        retainedAlgorithmOwners.ensureCapacity(Math.addExact(
                retainedAlgorithmOwners.size(),
                1));
    }

    private static Throwable retryRetainedAlgorithmOwners(Throwable failure) {
        Iterator<AbstractAlgorithm> iterator = retainedAlgorithmOwners.iterator();
        while (iterator.hasNext()) {
            AbstractAlgorithm retained = iterator.next();
            try {
                retained.destroy();
                iterator.remove();
            } catch (Throwable cleanupFailure) {
                failure = appendAlgorithmFailure(failure, cleanupFailure);
            }
        }
        return failure;
    }

    private static Throwable appendAlgorithmFailure(
            Throwable failure,
            Throwable addition) {
        if (failure == null) {
            return addition;
        }
        if (failure != addition) {
            failure.addSuppressed(addition);
        }
        return failure;
    }

    public static void rethrowAlgorithmError(Throwable failure) {
        Error error = findAlgorithmError(
                failure,
                Collections.newSetFromMap(new IdentityHashMap<>()));
        if (error != null) {
            throw error;
        }
    }

    private static Error findAlgorithmError(
            Throwable failure,
            Set<Throwable> visited) {
        if (failure == null || !visited.add(failure)) {
            return null;
        }
        if (failure instanceof Error error) {
            return error;
        }
        for (Throwable suppressed : failure.getSuppressed()) {
            Error suppressedError = findAlgorithmError(suppressed, visited);
            if (suppressedError != null) {
                return suppressedError;
            }
        }
        return findAlgorithmError(failure.getCause(), visited);
    }

    private static void throwAlgorithmFailure(Throwable failure) {
        if (failure == null) {
            return;
        }
        rethrowAlgorithmError(failure);
        if (failure instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        throw new RuntimeException(failure);
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
        #if IS_DEV == 1
        if (SuperResolutionConfig.isEnableImgui()) {
            new ImguiMain();
        }
        #else
        if (Platform.currentPlatform.isDevelopmentEnvironment() && SuperResolutionConfig.isEnableImgui()) {
            new ImguiMain();
        }
        #endif


        isInit = true;
        if (!B3DVulkanBridge.isB3DVulkanBackend()) {
            cachedWidth = RenderHandlerManager.getScreenWidth();
            cachedHeight = RenderHandlerManager.getScreenHeight();
        }
    }

    public static void resizeAlgorithmIfChanged(int w, int h) {
        if (w == cachedWidth && h == cachedHeight) {
            return;
        }
        resizeAlgorithm(w, h);
    }

    public void forceResize(int width, int height) {
        resizeAlgorithm(width, height);
    }

    private static void resizeAlgorithm(int w, int h) {
        cachedWidth = w;
        cachedHeight = h;
        if (B3DVulkanBridge.isB3DVulkanBackend()) {
            return;
        }
        if (currentAlgorithm == null || !SuperResolutionConfig.isEnableUpscaleOriginal()) {
            return;
        }
        VulkanPresentationWindow.flushCapturedFrame();
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
        FrameGeneration.invalidateHistory();
    }

    public void destroy() {
        isInit = false;
        isRenderingInitialized = false;
        graphicsBackendDestroyed = false;
        FrameGeneration.shutdown();
        VulkanPresentationFeature.shutdown();
        LowLatency.shutdown();
        if (currentAlgorithm != null) {
            currentAlgorithm.destroy();
            currentAlgorithm = null;
        }
        throwAlgorithmFailure(retryRetainedAlgorithmOwners(null));
        if (!B3DVulkanBridge.isB3DVulkanBackend()) {
            AlgorithmManager.destroy();
        }
        SRReturnCode shutdownCode = SuperResolutionNativeAPI.srShutdown();
        if (shutdownCode != SRReturnCode.OK) {
            throw new IllegalStateException(
                    "SRAPI shutdown failed and retained provider owners for retry: " +
                            shutdownCode);
        }
        Streamline.shutdown();
        NgxInitializer.shutdown();
        // In Vulkan-presentation (interop) mode the hidden OpenGL context and the Vulkan
        // device are torn down later, in destroyGraphicsBackend() at Minecraft.destroy()
        // TAIL, so Minecraft's own shutdown rendering (the disconnect progress screen and
        // GL resource cleanup) still has a current GL context. Destroying them here left
        // that rendering without a context and aborted the JVM on exit. Without the
        // interop presentation there is no shared context to protect, so tear down now.
        if (!VulkanPresentationFeature.isRequested()) {
            destroyGraphicsBackend();
        }
    }

    public void destroyGraphicsBackend() {
        if (graphicsBackendDestroyed) {
            return;
        }
        Throwable algorithmFailure = retryRetainedAlgorithmOwners(null);
        if (currentAlgorithm != null) {
            algorithmFailure = appendAlgorithmFailure(
                    algorithmFailure,
                    new IllegalStateException(
                            "Cannot destroy the graphics backend while an algorithm is active"));
        }
        throwAlgorithmFailure(algorithmFailure);
        // GLFW must destroy the hidden OpenGL context before the Vulkan driver is torn down.
        PresentationWindowState.destroyRenderWindow();
        RenderSystems.destroy();
        graphicsBackendDestroyed = true;
    }
}
