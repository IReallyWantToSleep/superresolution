package io.homo.superresolution.common.mixin.core.accessor;


import com.mojang.blaze3d.platform.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = Window.class, remap = false)
public interface WindowAccessor {
    @Mutable
    @Accessor("framebufferHeight")
    int getFramebufferHeight_();

    @Mutable
    @Accessor("framebufferWidth")
    int getFramebufferWidth_();

}
