package io.homo.superresolution.resolutioncontrol.mixin;

import io.homo.superresolution.resolutioncontrol.ResolutionControl;
import net.minecraft.client.main.GameConfig;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Minecraft.class)
public abstract class MinecraftClientMixin{
}
