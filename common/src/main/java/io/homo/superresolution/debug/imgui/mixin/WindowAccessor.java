package io.homo.superresolution.debug.imgui.mixin;

import com.mojang.blaze3d.platform.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = Window.class)
public interface WindowAccessor {
    @Mutable
    @Accessor(value = "window")
    long getWindow();
}
