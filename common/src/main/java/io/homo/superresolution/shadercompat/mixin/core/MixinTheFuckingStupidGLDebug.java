package io.homo.superresolution.shadercompat.mixin.core;

import net.irisshaders.iris.gl.GLDebug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.PrintStream;

@Mixin(value = GLDebug.class,remap = false)
public class MixinTheFuckingStupidGLDebug {
    #if IS_DEV
    @Inject(method = "setupDebugMessageCallback(Ljava/io/PrintStream;)I", at=@At("HEAD"),cancellable = true)
    private static void cancelTheFuckingStupidMessage(PrintStream stream, CallbackInfoReturnable<Integer> cir){
        cir.cancel();
    }
    #endif
}
