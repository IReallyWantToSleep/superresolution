package io.homo.superresolution.fabric;

import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import io.homo.superresolution.SuperResolution;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;

public final class SuperResolutionFabricClient implements ClientModInitializer {
    public static SuperResolution mod;
    @Override
    public void onInitializeClient() {
        SuperResolution.preInit();
        mod = new SuperResolution();
        RenderSystem.recordRenderCall(()->{
            SuperResolution.mainTarget = (MainTarget) Minecraft.getInstance().getMainRenderTarget();
            SuperResolution.initRendering();
            SuperResolution.createAlgo();
            SuperResolutionFabricClient.mod.init();
        });
    }
}
