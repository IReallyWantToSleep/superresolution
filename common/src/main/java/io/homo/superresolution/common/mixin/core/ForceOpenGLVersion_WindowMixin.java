package io.homo.superresolution.common.mixin.core;

import com.mojang.blaze3d.platform.Window;
import io.homo.superresolution.common.mixin.core.accessor.MinecraftAccessor;
import io.homo.superresolution.common.platform.Platform;
import io.homo.superresolution.core.graphics.GraphicsCapabilities;
import io.homo.superresolution.core.graphics.renderdoc.RenderDoc;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static org.lwjgl.glfw.GLFW.*;

@Mixin(value = Window.class)
public class ForceOpenGLVersion_WindowMixin {
    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwWindowHint(II)V", ordinal = 5), remap = false)
    private void forceOpenGLVersion(int hint, int value) {
        //glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 4);
        //glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 1);

        glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, GraphicsCapabilities.getHighestOpenGLVersion().left());
        glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, GraphicsCapabilities.getHighestOpenGLVersion().right());
        glfwWindowHint(hint, value);
    }
}