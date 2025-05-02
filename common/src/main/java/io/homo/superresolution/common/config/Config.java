package io.homo.superresolution.common.config;

import com.google.gson.GsonBuilder;
import io.homo.superresolution.api.registry.AlgorithmDescription;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.special.SpecialConfigs;
import io.homo.superresolution.common.config.enums.CaptureMode;
import io.homo.superresolution.common.config.enums.SgsrVariant;
import io.homo.superresolution.common.minecraft.MinecraftRenderHandle;
import net.minecraft.client.Minecraft;

public class Config {
    private static ConfigData instance = new ConfigData();
    private static Runnable resolutionChangeCallback;

    static {
        setResolutionChangeCallback(() -> {
            MinecraftRenderHandle.resize();
            SuperResolution.getInstance().resize(
                    MinecraftRenderHandle.getScreenWidth(),
                    MinecraftRenderHandle.getScreenHeight()
            );
            Minecraft.getInstance().levelRenderer.resize(
                    MinecraftRenderHandle.getScreenWidth(),
                    MinecraftRenderHandle.getScreenHeight()
            );
        });
    }


    public static boolean isEnableRenderDoc() {
        return instance.isEnableRenderDoc();
    }

    public static void setEnableRenderDoc(boolean enableRenderDoc) {
        instance.setEnableRenderDoc(enableRenderDoc);
    }

    public static boolean isEnableImgui() {
        return instance.isEnableImgui();
    }

    public static void setEnableImgui(boolean enableImgui) {
        instance.setEnableImgui(enableImgui);
    }

    public static ConfigData getInstance() {
        return instance;
    }

    public static void setInstance(ConfigData instance) {
        if (instance != null) Config.instance = instance;
    }

    public static void registerTypeAdapter(GsonBuilder gsonBuilder) {
        gsonBuilder.registerTypeAdapter(
                AlgorithmDescription.class,
                new AlgorithmDescriptionSerializer()
        );
        gsonBuilder.registerTypeAdapter(
                CaptureMode.class,
                new EnumSerializer.Builder<CaptureMode>()
                        .addMapping("a", CaptureMode.A)
                        .addMapping("b", CaptureMode.B)
                        .addMapping("c", CaptureMode.C)
                        .setDefault(CaptureMode.A)
                        .build()
        );

        gsonBuilder.registerTypeAdapter(
                SgsrVariant.class,
                new EnumSerializer.Builder<SgsrVariant>()
                        .addMapping("CS_2", SgsrVariant.CS_2)
                        .addMapping("CS_3", SgsrVariant.CS_3)
                        .addMapping("FS_2", SgsrVariant.FS_2)
                        .setDefault(SgsrVariant.CS_2)
                        .build()
        );
    }

    public static CaptureMode getCaptureMode() {
        return instance.getCaptureMode();
    }

    public static void setCaptureMode(CaptureMode captureMode) {
        instance.setCaptureMode(captureMode);
    }

    public static float getRenderScaleFactor() {
        return instance.getRenderScaleFactor();
    }

    public static float getUpscaleRatio() {
        return instance.getUpscaleRatio();
    }

    public static void setUpscaleRatio(float value) {
        boolean resolutionChanged = getUpscaleRatio() != value;
        instance.setUpscaleRatio(value);
        if (resolutionChanged) runResolutionChangeCallback();
    }

    public static AlgorithmDescription<?> getUpscaleAlgo() {
        return instance.getUpscaleAlgo();
    }

    public static void setUpscaleAlgo(AlgorithmDescription<?> upscaleAlgo) {
        instance.setUpscaleAlgo(upscaleAlgo);
    }

    public static float getSharpness() {
        return instance.getSharpness();
    }

    public static void setSharpness(float sharpness) {
        instance.setSharpness(sharpness);
    }

    public static double getMinUpscaleRatio() {
        int maxSize = 16384;
        if (Minecraft.getInstance().getWindow() == null) return 0.1;
        double maxWidth = 1 / ((double) maxSize / Minecraft.getInstance().getWindow().getScreenWidth());
        double maxHeight = 1 / ((double) maxSize / Minecraft.getInstance().getWindow().getScreenHeight());
        return Math.max(maxWidth, maxHeight);
    }

    public static boolean isEnableUpscale() {
        return instance.isEnableUpscale();
    }

    public static void setEnableUpscale(boolean enableUpscale) {
        instance.setEnableUpscale(enableUpscale);
    }

    public static SpecialConfigs getSpecial() {
        return instance.getSpecial();
    }

    public static void setSpecial(SpecialConfigs special) {
        instance.setSpecial(special);
    }

    public static void setResolutionChangeCallback(Runnable callback) {
        resolutionChangeCallback = callback;
    }

    private static void runResolutionChangeCallback() {
        if (resolutionChangeCallback != null) {
            resolutionChangeCallback.run();
        }
    }

    public static boolean isSkipLoadNativeLib() {
        return instance.isSkipLoadNativeLib();
    }

    public static void setSkipLoadNativeLib(boolean skipLoadNativeLib) {
        instance.setSkipLoadNativeLib(skipLoadNativeLib);
    }

    public static boolean isSkipInitVulkan() {
        return instance.isSkipInitVulkan();

    }

    public static void setSkipInitVulkan(boolean skipInitVulkan) {
        instance.setSkipInitVulkan(skipInitVulkan);

    }

    public static boolean isGenerateMotionVectors() {
        return instance.isGenerateMotionVectors();
    }

    public static void setGenerateMotionVectors(boolean generateMotionVectors) {
        instance.setGenerateMotionVectors(generateMotionVectors);
    }
}
