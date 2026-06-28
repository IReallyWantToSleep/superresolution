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

package io.homo.superresolution.common.minecraft.handler.shadercompat;

import com.google.common.collect.ImmutableList;
import io.homo.superresolution.common.debug.imgui.ImGuiDebugContext;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.minecraft.CallType;
import io.homo.superresolution.common.minecraft.MinecraftRenderTargetType;
import io.homo.superresolution.common.minecraft.MinecraftRenderTargetWrapper;
import io.homo.superresolution.common.minecraft.handler.IMinecraftRenderHandler;
import io.homo.superresolution.common.minecraft.handler.RenderHandlerManager;
import io.homo.superresolution.common.minecraft.handler.shadercompat.v2.SRCompatV2Processor;
import io.homo.superresolution.common.mixin.core.accessor.PostChainAccessor;
import io.homo.superresolution.common.upscale.InteropResourcesConverter;
import io.homo.superresolution.core.graphics.impl.framebuffer.IBindableFrameBuffer;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.opengl.framebuffer.GlFrameBuffer;
import net.irisshaders.iris.helpers.StringPair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class ShaderCompatHandler implements IMinecraftRenderHandler {
    private static boolean isLoadingShader;
    private static volatile boolean irisReloadReflectionInitialized;
    private static Method irisReloadMethod;
    private static volatile boolean irisApiReflectionInitialized;
    private static Method irisApiGetInstanceMethod;
    private static Method irisGetCurrentPackMethod;
    private static Method irisApiIsShaderPackInUseMethod;
    private static volatile boolean shaderCompatUtilsReflectionInitialized;
    private static Method shouldApplySuperResolutionChangesMethod;
    private static Method getCurrentShaderPackConfigMethod;
    private static Method getCurrentConfigMethod;
    private static volatile boolean shaderCompatDispatcherReflectionInitialized;
    private static Field shaderCompatColorTextureField;
    private static Field shaderCompatDepthTextureField;
    private static Throwable shaderCompatDispatcherReflectionError;
    private static SRShaderCompatData shaderCompatData;
    private static Path cachedShaderPackPath;
    private final Map<MinecraftRenderTargetType, IBindableFrameBuffer> renderTargets = new HashMap<>();

    public static SRShaderCompatData getShaderCompatData() {
        return shaderCompatData;
    }

    public static void setShaderCompatData(SRShaderCompatData shaderCompatData) {
        ShaderCompatHandler.shaderCompatData = shaderCompatData;
    }

    public static Path getCachedShaderPackPath() {
        return cachedShaderPackPath;
    }

    public static void setCachedShaderPackPath(Path path) {
        cachedShaderPackPath = path;
    }

    public static boolean isLoadingShader() {
        return isLoadingShader;
    }

    public static void setLoadingShader(boolean loadingShader) {
        isLoadingShader = loadingShader;
    }

    private static void initIrisReloadReflection() {
        if (irisReloadReflectionInitialized) {
            return;
        }
        synchronized (ShaderCompatHandler.class) {
            if (irisReloadReflectionInitialized) {
                return;
            }
            try {
                Class<?> irisApiClazz = Class.forName("net.irisshaders.iris.Iris");
                irisReloadMethod = irisApiClazz.getMethod("reload");
                irisGetCurrentPackMethod = irisApiClazz.getMethod("getCurrentPack");
            } catch (Throwable ignored) {
            }
            irisReloadReflectionInitialized = true;
        }
    }

    private static void initIrisApiReflection() {
        if (irisApiReflectionInitialized) {
            return;
        }
        synchronized (ShaderCompatHandler.class) {
            if (irisApiReflectionInitialized) {
                return;
            }
            try {
                Class<?> irisApiClazz = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                irisApiGetInstanceMethod = irisApiClazz.getMethod("getInstance");
                irisApiIsShaderPackInUseMethod = irisApiClazz.getMethod("isShaderPackInUse");
            } catch (Throwable ignored) {
            }
            irisApiReflectionInitialized = true;
        }
    }

    private static void initShaderCompatUtilsReflection() {
        if (shaderCompatUtilsReflectionInitialized) {
            return;
        }
        synchronized (ShaderCompatHandler.class) {
            if (shaderCompatUtilsReflectionInitialized) {
                return;
            }
            try {
                Class<?> shaderCompatUtilsClass = Class.forName("io.homo.superresolution.shadercompat.IrisShaderCompatUtils");
                shouldApplySuperResolutionChangesMethod = shaderCompatUtilsClass.getMethod("shouldApplySuperResolutionChanges");
                getCurrentShaderPackConfigMethod = shaderCompatUtilsClass.getMethod("getCurrentShaderPackConfig");
                getCurrentConfigMethod = shaderCompatUtilsClass.getMethod("getCurrentConfig");
            } catch (Throwable ignored) {
            }
            shaderCompatUtilsReflectionInitialized = true;
        }
    }

    private static void initShaderCompatDispatcherReflection() {
        if (shaderCompatDispatcherReflectionInitialized) {
            return;
        }
        synchronized (ShaderCompatHandler.class) {
            if (shaderCompatDispatcherReflectionInitialized) {
                return;
            }
            try {
                Class<?> dispatcherClass = Class.forName("io.homo.superresolution.shadercompat.IrisShaderCompatUpscaleDispatcher");
                shaderCompatColorTextureField = dispatcherClass.getField("colorTexture");
                shaderCompatDepthTextureField = dispatcherClass.getField("depthTexture");
            } catch (Throwable e) {
                shaderCompatDispatcherReflectionError = e;
            }
            shaderCompatDispatcherReflectionInitialized = true;
        }
    }

    public static void irisApiReloadShader() {
        initIrisReloadReflection();
        if (irisReloadMethod == null) {
            return;
        }
        try {
            irisReloadMethod.invoke(null);
        } catch (Throwable ignored) {
        }
    }

    public static boolean irisHasShaderPack() {
        initIrisReloadReflection();
        if (irisGetCurrentPackMethod == null) {
            return false;
        }
        try {
            Optional<?> shaderPack = (Optional<?>) irisGetCurrentPackMethod.invoke(null);
            return shaderPack.isPresent();
        } catch (Throwable ignored) {
        }
        return false;
    }

    public static boolean irisApiIsShaderPackInUse() {
        initIrisApiReflection();
        if (irisApiGetInstanceMethod == null || irisApiIsShaderPackInUseMethod == null) {
            return false;
        }
        try {
            Object irisApiInstance = irisApiGetInstanceMethod.invoke(null);
            return (boolean) irisApiIsShaderPackInUseMethod.invoke(irisApiInstance) || irisHasShaderPack();
        } catch (Throwable ignored) {
        }
        return false;
    }

    public static boolean dontHackMinecraftRenderingPipeline() {
        initShaderCompatUtilsReflection();
        if (shouldApplySuperResolutionChangesMethod == null) {
            return false;
        }
        try {
            return (Boolean) shouldApplySuperResolutionChangesMethod.invoke(null);
        } catch (Throwable e) {
            return false;
        }
    }

    public static Optional<SRShaderCompatData> getShaderPackCompatConfig() {
        initShaderCompatUtilsReflection();
        if (getCurrentShaderPackConfigMethod == null) {
            return Optional.empty();
        }
        try {
            return (Optional<SRShaderCompatData>) getCurrentShaderPackConfigMethod.invoke(null);
        } catch (Throwable e) {
            return Optional.empty();
        }
    }

    public static Optional<SRShaderCompatData.WorldProfile> getCurrentLevelCompatConfig() {
        initShaderCompatUtilsReflection();
        if (getCurrentConfigMethod == null) {
            return Optional.empty();
        }
        try {
            Object result = getCurrentConfigMethod.invoke(null);
            // getCurrentConfig() 返回 Optional<WorldProfile>，需要先转换为 Optional 再取内容
            Optional<?> opt = (Optional<?>) result;
            return opt.map(o -> (SRShaderCompatData.WorldProfile) o);
        } catch (Throwable e) {
            return Optional.empty();
        }
    }



    public static void loadConfig(
            Path root,
            ImmutableList<?> environmentDefines
    ){
        try {
            Path srConfigPath = null;
            for (int ver = SRCompatConfigParser.LATEST_CONFIG_VERSION; ver >= 1; ver--) {
                Path candidate = root.resolve("superresolution.v" + ver + ".json");
                if (Files.exists(candidate)) {
                    srConfigPath = candidate;
                    SuperResolution.LOGGER.info("加载光影接口配置文件: superresolution.v{}.json", ver);
                    break;
                }
            }
            if (srConfigPath == null) {
                Path candidate = root.resolve("superresolution.json");
                if (Files.exists(candidate)) {
                    srConfigPath = candidate;
                    SuperResolution.LOGGER.info("加载光影接口配置文件: superresolution.json");
                }
            }
            if (srConfigPath != null) {

                JsonMacroPreprocessor preprocessor = new JsonMacroPreprocessor();
                for (Object stringPair : environmentDefines) {
                    preprocessor.addMacro(((StringPair) stringPair).key(), ((StringPair) stringPair).value());
                }
                SRCompatBuiltinMacros.addMacros(preprocessor);
                shaderCompatData = SRCompatConfigParser.load(srConfigPath, preprocessor);
                SuperResolution.LOGGER.info("光影包 {} 支持超分辨率功能", root);
                return;
            }
        } catch (Throwable throwable) {
            SuperResolution.LOGGER.trace("从光影包 {} 加载SR配置失败", root, throwable);
            SuperResolution.LOGGER.warn("加载 {} 光影包中的SR配置时发生错误", root);
        }
        shaderCompatData = null;
    }

    public void updateRenderTarget() {
        renderTargets.clear();
        for (MinecraftRenderTargetType minecraftRenderTargetType : MinecraftRenderTargetType.values()) {
            IBindableFrameBuffer renderTarget = minecraftRenderTargetType.get(Minecraft.getInstance().levelRenderer);
            if (renderTarget != null) {
                renderTargets.put(
                        minecraftRenderTargetType,
                        renderTarget
                );
            }
        }
    }

    public void callOnRenderTargets(Consumer<IFrameBuffer> callback) {
        renderTargets.forEach(((minecraftRenderTargetType, renderTarget) -> {
            if (renderTarget != null && minecraftRenderTargetType != MinecraftRenderTargetType.HAND) {
                callback.accept(renderTarget);
            }
        }));
    }

    public void updateRenderTargetSize() {
        int screenWidth = RenderHandlerManager.getScreenWidth();
        int screenHeight = RenderHandlerManager.getScreenHeight();
        callOnRenderTargets(
                (renderTarget) -> {
                    if (renderTarget.getWidth() != screenWidth || renderTarget.getHeight() != screenHeight) {
                        if (renderTarget instanceof MinecraftRenderTargetWrapper wrapper) {
                            wrapper.resizeFrameBuffer(screenWidth, screenHeight);
                        } else if (renderTarget instanceof GlFrameBuffer glFbo) {
                            glFbo.resizeFrameBuffer(screenWidth, screenHeight);
                        }
                    }
                }
        );
        IFrameBuffer handRenderTarget = getRenderTarget(MinecraftRenderTargetType.HAND);
        if (handRenderTarget != null && (handRenderTarget.getWidth() != screenWidth || handRenderTarget.getHeight() != screenHeight)) {
            if (handRenderTarget instanceof GlFrameBuffer glFbo) {
                glFbo.resizeFrameBuffer(screenWidth, screenHeight);
            }
        }
    }

    public IBindableFrameBuffer getRenderTarget(MinecraftRenderTargetType type) {
        return renderTargets.get(type);
    }

    @Override
    public void onRenderWorldBegin(CallType type) {
        updateRenderTarget();
        updateRenderTargetSize();
    }

    @Override
    public void onRenderWorldEnd(CallType type) {
        //updateRenderTarget();
        //updateRenderTargetSize();
    }

    @Override
    public void onRenderHandBegin() {

    }

    @Override
    public void onRenderHandEnd() {

    }

    @Override
    public void onProcessPostChain(PostChain postChain) {
        //#if MC_VER < MC_1_21_4
        //int renderWidth = RenderHandlerManager.getScreenWidth();
        //int renderHeight = RenderHandlerManager.getScreenHeight();
        ////修复PostChain中的RenderTarget大小不正确
        //for (com.mojang.blaze3d.pipeline.RenderTarget renderTarget : ((PostChainAccessor) postChain).getFullSizedTargets()) {
        //    if (renderTarget.width != renderWidth ||
        //            renderTarget.height != renderHeight ||
        //            ((PostChainAccessor) postChain).getScreenWidth() != renderWidth ||
        //            ((PostChainAccessor) postChain).getScreenHeight() != renderHeight) {
        //        postChain.resize(renderWidth, renderHeight);
        //        break;
        //    }
        //}
        //#endif
    }

    @Override
    public IBindableFrameBuffer getFullSizeRenderTarget() {
        return RenderHandlerManager.getOriginRenderTarget();
    }

    @Override
    public IBindableFrameBuffer getScaledRenderTarget() {
        return RenderHandlerManager.getOriginRenderTarget();
    }

    @Override
    public void initialize() {

    }

    @Override
    public void resize() {

    }

    @Nullable
    public ITexture getColorTexture() {
        initShaderCompatDispatcherReflection();
        if (shaderCompatDispatcherReflectionError != null) {
            throw new RuntimeException(shaderCompatDispatcherReflectionError);
        }
        if (shaderCompatColorTextureField == null) {
            return null;
        }
        try {
            ShaderCompatTextureInfo textureInfo = ((ShaderCompatTextureInfo) shaderCompatColorTextureField.get(null));
            if (textureInfo == null) return null;
            return textureInfo.getInternalTexture();
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Nullable
    public ITexture getDepthTexture() {
        initShaderCompatDispatcherReflection();
        if (shaderCompatDispatcherReflectionError != null) {
            throw new RuntimeException(shaderCompatDispatcherReflectionError);
        }
        if (shaderCompatDepthTextureField == null) {
            return null;
        }
        try {
            ShaderCompatTextureInfo textureInfo = ((ShaderCompatTextureInfo) shaderCompatDepthTextureField.get(null));
            if (textureInfo == null) return null;
            return textureInfo.getInternalTexture();
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void destroy() {
        renderTargets.clear();
        InteropResourcesConverter.destroy();
        SRCompatV2Processor.destroyPipelineCache();
    }

    @Override
    public void renderImGuiDebug(ImGuiDebugContext ctx) {
        ctx.property("Shader Loading", isLoadingShader());
        ctx.property("Cached Shader Pack Path", getCachedShaderPackPath());
        ctx.property("Shader Pack Present", irisHasShaderPack());
        ctx.property("Shader Pack In Use", irisApiIsShaderPackInUse());
        ctx.property("Compat Config Loaded", shaderCompatData != null);
        if (shaderCompatData != null) {
            ctx.property("Compat Config Version", shaderCompatData.version);
        }
    }

    @Override
    public void collectDebugTextures(ImGuiDebugContext ctx) {
        ctx.addFramebufferTextures("origin_render_target", "Origin Render Target", getFullSizeRenderTarget());
        ctx.addFramebufferTextures("scaled_render_target", "Scaled Render Target", getScaledRenderTarget());

        ShaderCompatTextureInfo colorInfo = getTextureInfo("colorTexture");
        ShaderCompatTextureInfo depthInfo = getTextureInfo("depthTexture");
        ShaderCompatTextureInfo motionVectorsInfo = getTextureInfo("motionVectorsTexture");
        ShaderCompatTextureInfo exposureInfo = getTextureInfo("exposureTexture");

        if (colorInfo != null) {
            addTextureInfo(ctx, "dispatcher_color", "Dispatcher Color", colorInfo);
        }
        if (depthInfo != null) {
            addTextureInfo(ctx, "dispatcher_depth", "Dispatcher Depth", depthInfo);
        }
        if (motionVectorsInfo != null) {
            addTextureInfo(ctx, "dispatcher_mv", "Dispatcher Motion Vectors", motionVectorsInfo);
        }
        if (exposureInfo != null) {
            addTextureInfo(ctx, "dispatcher_exposure", "Dispatcher Exposure", exposureInfo);
        }

    }

    private void addTextureInfo(ImGuiDebugContext ctx, String id, String label, ShaderCompatTextureInfo info) {
        ctx.addTexture(id + ".source", label + " Source", info.getSourceTexture(), "Source Texture", true);
        ctx.addTexture(id + ".internal", label + " Internal", info.getInternalTexture(), "Internal Texture", true);
        ctx.addTexture(id + ".preprocess_in", label + " PreProcess Input", info.getPreProcessInputTexture(), "PreProcess Input", true);
        ctx.addTexture(id + ".preprocess_out", label + " PreProcess Output", info.getPreProcessOutputTexture(), "PreProcess Output", true);
    }

    @Nullable
    private ShaderCompatTextureInfo getTextureInfo(String fieldName) {
        initShaderCompatDispatcherReflection();
        if (shaderCompatDispatcherReflectionError != null) {
            return null;
        }
        try {
            Class<?> dispatcherClass = Class.forName("io.homo.superresolution.shadercompat.IrisShaderCompatUpscaleDispatcher");
            Field field = dispatcherClass.getField(fieldName);
            return (ShaderCompatTextureInfo) field.get(null);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
