package io.homo.superresolution.neoforge.mixin.core;

import io.homo.superresolution.core.GraphicsCapabilities;
import net.neoforged.fml.earlydisplay.DisplayWindow;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static org.lwjgl.glfw.GLFW.glfwWindowHint;

@Mixin(DisplayWindow.class)
public class NeoForgeDisplayWindowMixin {

    /*
    由于此提交：https://github.com/neoforged/FancyModLoader/commit/f1f1ec5556b07bcc6e53da6bcc59d21b5a5d4e1f
    强制MC使用OpenGL 3.2，无法用其它办法修改，就直接Mixin了
    */
    #if MC_VER > MC_1_21_4
    @Redirect(method = "initWindow", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwWindowHint(II)V"))
    private void overrideGlVersion(int hint, int value) {
        if (hint == GLFW.GLFW_CONTEXT_VERSION_MAJOR) {
            glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, GraphicsCapabilities.getHighestOpenGLVersion().left());
        } else if (hint == GLFW.GLFW_CONTEXT_VERSION_MINOR) {
            glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, GraphicsCapabilities.getHighestOpenGLVersion().right());
        } else {
            glfwWindowHint(hint, value);
        }
    }
    #endif
}
