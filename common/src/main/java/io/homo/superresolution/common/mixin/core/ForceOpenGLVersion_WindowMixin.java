package io.homo.superresolution.common.mixin.core;

import com.mojang.blaze3d.platform.Window;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Window.class)
public class ForceOpenGLVersion_WindowMixin {
    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwWindowHint(II)V", remap = false))
    private void forceOpenGLVersion(int hint, int value) {
        if (hint == GLFW.GLFW_CONTEXT_VERSION_MAJOR) {
            value = 4;
        } else if (hint == GLFW.GLFW_CONTEXT_VERSION_MINOR) {
            value = 6;
        }
        GLFW.glfwWindowHint(hint, value);
    }
}