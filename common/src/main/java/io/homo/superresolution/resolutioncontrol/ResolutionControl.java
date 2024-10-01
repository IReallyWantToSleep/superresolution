package io.homo.superresolution.resolutioncontrol;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Window;
import io.homo.superresolution.SuperResolution;
import io.homo.superresolution.config.Config;
import io.homo.superresolution.resolutioncontrol.mixin.MinecraftAccessor;
import io.homo.superresolution.utils.FrameBuffer;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

public class ResolutionControl {
    private static ResolutionControl instance;
    private static Minecraft minecraft;
    private FrameBuffer framebuffer;
    private boolean shouldScale = false;
    public int currentWidth;
    public int currentHeight;
    private Set<RenderTarget> minecraftFramebuffers;
    public ResolutionControl(Minecraft mc){
        minecraft = mc;
        instance = this;
    }
    public void init(){
        onResolutionChanged();
    }

    public static ResolutionControl getInstance() {
        return instance;
    }
    public static boolean isInit(){
        return !(instance == null);
    }
    public float getScaleFactor() {
        return Config.getRenderScaleFactor();
    }
    private Window getWindow() { return minecraft.getWindow(); }
    public void setShouldScale(boolean shouldScale) {
        if (shouldScale == this.shouldScale) return;
        if (getScaleFactor() == 1) return;
        if (framebuffer == null) {
            this.shouldScale = true;
            calculateSize();
            framebuffer = new FrameBuffer(true);
        }
        this.shouldScale = shouldScale;
        minecraft.getProfiler().popPush(shouldScale ? "startScaling" : "finishScaling");
        if (shouldScale) {
            setClientFramebuffer(framebuffer);
            SuperResolution.FSR.setWorldFramebuffer(framebuffer);
            framebuffer.bindWrite(true);
        } else {
            setClientFramebuffer(SuperResolution.mainTarget);
            SuperResolution.mainTarget.bindWrite(true);
        }
        minecraft.getProfiler().popPush("level");
    }

    private void calculateSize() {
        if (warnFramebufferNull()) return;
        currentWidth = framebuffer.width;
        currentHeight = framebuffer.height;
    }
    public void setClientFramebuffer(RenderTarget framebuffer){
        ((MinecraftAccessor)Minecraft.getInstance()).setFramebuffer(framebuffer);
    }
    public float getCurrentScaleFactor(){
        return shouldScale?getScaleFactor():1;
    }
    private void initMinecraftFramebuffers() {
        if (minecraftFramebuffers != null) {
            minecraftFramebuffers.clear();
        } else {
            minecraftFramebuffers = new HashSet<>();
        }
        minecraftFramebuffers.add(minecraft.levelRenderer.entityTarget());
        minecraftFramebuffers.add(minecraft.levelRenderer.getTranslucentTarget());
        minecraftFramebuffers.add(minecraft.levelRenderer.getItemEntityTarget());
        minecraftFramebuffers.add(minecraft.levelRenderer.getParticlesTarget());
        minecraftFramebuffers.add(minecraft.levelRenderer.getWeatherTarget());
        minecraftFramebuffers.add(minecraft.levelRenderer.getCloudsTarget());
        minecraftFramebuffers.remove(null);
    }
    private void resize(@Nullable RenderTarget framebuffer) {
        if (warnFramebufferNull()) return;
        boolean prev = shouldScale;
        shouldScale = true;
        if (framebuffer != null) {
            framebuffer.resize(
                    getWindow().getWidth(),
                    getWindow().getHeight(),
                    Minecraft.ON_OSX
            );
        }

        shouldScale = prev;
    }
    public void resizeMinecraftFramebuffers() {
        initMinecraftFramebuffers();
        minecraftFramebuffers.forEach(this::resize);
    }
    public void onResolutionChanged() {
        if (minecraft.level == null) return;
        updateFramebufferSize();
    }

    public void updateFramebufferSize() {

        if (warnFramebufferNull()) return;
        framebuffer.resize(SuperResolution.FSR.getHelper().renderWidth,SuperResolution.FSR.getHelper().renderHeight,Minecraft.ON_OSX);
        resize(minecraft.levelRenderer.entityTarget());
        calculateSize();
    }

    private boolean warnFramebufferNull(){
        if (framebuffer == null){
            SuperResolution.LOGGER.warn("framebuffer=null");
            return true;
        }
        return false;
    }
}

