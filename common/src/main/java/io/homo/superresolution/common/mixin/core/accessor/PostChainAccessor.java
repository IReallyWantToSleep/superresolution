package io.homo.superresolution.common.mixin.core.accessor;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.PostChain;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = PostChain.class)
public interface PostChainAccessor {
    @Mutable
    @Accessor(value = "fullSizedTargets")
    List<RenderTarget> getFullSizedTargets();

    @Mutable
    @Accessor(value = "screenWidth")
    int getScreenWidth();

    @Mutable
    @Accessor(value = "screenHeight")
    int getScreenHeight();
}
