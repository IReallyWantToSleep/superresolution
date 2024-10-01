package io.homo.superresolution.mixin;

import com.mojang.blaze3d.platform.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Window.class)
public class TestMixin {
    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwWindowHint(II)V"))
    private void hintOverride(int hint, int value) {
        if (hint == GLFW.GLFW_CONTEXT_VERSION_MAJOR) {
            value = 3;
        } else if (hint == GLFW.GLFW_CONTEXT_VERSION_MINOR) {
            value = 2;
        }
        GLFW.glfwWindowHint(hint, value);
    }
}
