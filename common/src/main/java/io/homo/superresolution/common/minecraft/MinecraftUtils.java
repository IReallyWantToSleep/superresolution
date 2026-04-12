package io.homo.superresolution.common.minecraft;

import net.minecraft.client.Minecraft;

public class MinecraftUtils {
    public static void resize(){
        #if MC_VER > MC_1_21_11
        Minecraft.getInstance().resizeGui();
        Minecraft.getInstance().gameRenderer.resize(
                MinecraftWindow.getWindowWidth(),
                MinecraftWindow.getWindowHeight()
        );
        #else
        Minecraft.getInstance().resizeDisplay();
        #endif
    }

    public static float getCameraFar(){
        #if MC_VER > MC_1_21_11
        return Minecraft.getInstance().gameRenderer.getGameRenderState().levelRenderState.cameraRenderState.depthFar;
        #else
        return Minecraft.getInstance().gameRenderer.getDepthFar();
        #endif
    }

    public static float getCameraNear(){
        return 0.05F;
    }
}
