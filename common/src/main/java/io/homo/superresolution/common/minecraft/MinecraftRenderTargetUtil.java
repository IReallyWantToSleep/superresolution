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

package io.homo.superresolution.common.minecraft;


import com.mojang.blaze3d.pipeline.RenderTarget;

#if MC_VER > MC_1_21_4
    #if MC_VER <= MC_1_21_11
    //He delete this line on commit 09d38666d75667565cec2e34664260e32ad1e639, but we need to use it here.
    import com.mojang.blaze3d.opengl.GlDevice;
    #endif
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.opengl.DirectStateAccess;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

public class MinecraftRenderTargetUtil {
    private static Class<?> cachedGlDeviceClass;
    private static Method cachedGlDeviceDirectStateAccessMethod;

    #if MC_VER > MC_1_21_11
    private static Class<?> cachedGpuDeviceClass;
    private static Field cachedGpuDeviceBackendField;

    static {
        try {
            cachedGlDeviceClass = Class.forName("com.mojang.blaze3d.opengl.GlDevice");
            cachedGpuDeviceClass = Class.forName("com.mojang.blaze3d.systems.GpuDevice");
            cachedGpuDeviceBackendField = cachedGpuDeviceClass.getDeclaredField("backend");
            cachedGpuDeviceBackendField.setAccessible(true);
            cachedGlDeviceDirectStateAccessMethod = cachedGlDeviceClass.getMethod("directStateAccess");
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
    #else
    //On <=1.21.11
    //java.lang.RuntimeException: java.lang.ClassNotFoundException: com.mojang.blaze3d.opengl.GlDevice 

//    static {
//        try {
//            cachedGlDeviceClass = Class.forName("com.mojang.blaze3d.opengl.GlDevice");
//            cachedGlDeviceDirectStateAccessMethod = cachedGlDeviceClass.getMethod("directStateAccess");
//        } catch (Throwable e) {
//            throw new RuntimeException(e);
//        }
//    }
    #endif

    #if MC_VER > MC_1_21_11
    public static int getFboId(RenderTarget renderTarget) {
        try {
            //getDevice返回的不是GlDevice，而是一个像验证层的东西，它的backend字段才是实际GlDevice
            //RenderSystem.getDevice()->GpuDevice.backend-->GlDevice.directStateAccess()-->GlTexture.getFbo()
            return ((GlTexture) Objects.requireNonNull(renderTarget.getColorTexture()))
                    .getFbo((DirectStateAccess) cachedGlDeviceDirectStateAccessMethod.invoke(cachedGpuDeviceBackendField.get(RenderSystem.getDevice())), renderTarget.getDepthTexture());

        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
    #else
    public static int getFboId(RenderTarget renderTarget) {
        try {
            //Rollback to 57b8c0474b29f511d88faba108b43e869f282192
            return ((GlTexture) Objects.requireNonNull(renderTarget.getColorTexture())).getFbo(((GlDevice) RenderSystem.getDevice()).directStateAccess(), renderTarget.getDepthTexture());
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
    #endif

    public static int getColorTexId(RenderTarget renderTarget) {
        return ((GlTexture) Objects.requireNonNull(renderTarget.getColorTexture())).glId();
    }

    public static int getDepthTexId(RenderTarget renderTarget) {
        return ((GlTexture) Objects.requireNonNull(renderTarget.getDepthTexture())).glId();
    }
}
#else
public class MinecraftRenderTargetUtil {
    public static int getFboId(RenderTarget renderTarget) {
        return renderTarget.frameBufferId;
    }

    public static int getColorTexId(RenderTarget renderTarget) {
        return renderTarget.getColorTextureId();
    }

    public static int getDepthTexId(RenderTarget renderTarget) {
        return renderTarget.getDepthTextureId();
    }
}
#endif