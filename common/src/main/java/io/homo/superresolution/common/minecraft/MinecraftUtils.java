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

package io.homo.superresolution.common.minecraft;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

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

    public static void setScreen(Screen screen){
        #if MC_VER > MC_26_1_2
        Minecraft.getInstance().gui.setScreen(screen);
        #else
        Minecraft.getInstance().setScreen(screen);
        #endif
    }

    public static Screen getScreen(){
        #if MC_VER > MC_26_1_2
        return Minecraft.getInstance().gui.screen();
        #else
        return Minecraft.getInstance().screen;
        #endif
    }

    public static Camera getCamera(){
        #if MC_VER > MC_26_1_2
        return Minecraft.getInstance().gameRenderer.mainCamera();
        #else
        return Minecraft.getInstance().gameRenderer.getMainCamera();
        #endif
    }

    public static Object getFrameBufferCache(){
        #if MC_VER > MC_26_1_2
        try {
            return io.homo.superresolution.common.minecraft.MinecraftRenderTargetUtil.cachedFrameBufferCacheField.get(io.homo.superresolution.common.minecraft.MinecraftRenderTargetUtil.cachedGpuDeviceBackendField.get(RenderSystem.getDevice()));
        } catch(Throwable t){
            return null;
        }
        #else
        return Minecraft.getInstance().getMainRenderTarget();
        #endif
    }

    public static RenderTarget getMainRenderTarget(){
        #if MC_VER > MC_26_1_2
        return Minecraft.getInstance().gameRenderer.mainRenderTarget();
        #else
        return Minecraft.getInstance().getMainRenderTarget();
        #endif
    }

    public static float getCameraFar(){
        #if MC_VER > MC_26_1_2
        return Minecraft.getInstance().gameRenderer.gameRenderState().levelRenderState.cameraRenderState.depthFar;
        #elif MC_VER > MC_1_21_11
        return Minecraft.getInstance().gameRenderer.getGameRenderState().levelRenderState.cameraRenderState.depthFar;
        #else
        return Minecraft.getInstance().gameRenderer.getDepthFar();
        #endif
    }

    public static float getCameraNear(){
        return 0.05F;
    }
}
