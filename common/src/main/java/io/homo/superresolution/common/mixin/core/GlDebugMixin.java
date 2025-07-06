package io.homo.superresolution.common.mixin.core;
#if MC_VER < MC_1_21_5
import com.mojang.blaze3d.platform.GlDebug;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GlDebug.class)
public class GlDebugMixin {
    private static Logger LOGGER = LoggerFactory.getLogger("OpenGLDebug");

    @Inject(method = "printDebugLog", at = @At("TAIL"))
    private static void printGlErrorStackTrace(int source, int type, int id, int severity, int messageLength, long message, long userParam, CallbackInfo ci) {
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        LOGGER.error("OpenGL Error!");
        for (StackTraceElement element : elements) {
            LOGGER.error("    {}", element.toString());
        }
    }
}
#else
import com.mojang.blaze3d.opengl.GlDebug;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GlDebug.class)
public class GlDebugMixin {
    private static Logger LOGGER = LoggerFactory.getLogger("OpenGLDebug");
    @Inject(method = "printDebugLog", at = @At("TAIL"))
    private void printGlErrorStackTrace(int source, int type, int id, int severity, int messageLength, long message, long userParam, CallbackInfo ci) {
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        LOGGER.error("OpenGL Error!");
        for (StackTraceElement element : elements) {
            LOGGER.error("    {}", element.toString());
        }
    }
}
#endif
