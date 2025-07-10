package io.homo.superresolution.common.mixin.core.accessor;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(value = PostChain.class)
public interface PostChainAccessor {
    #if MC_VER < MC_1_21_4
    @Mutable
    @Accessor(value = "screenTarget")
    void setScreenTarget(RenderTarget screenTarget);

    @Mutable
    @Accessor(value = "fullSizedTargets")
    List<RenderTarget> getFullSizedTargets();

    @Mutable
    @Accessor(value = "screenWidth")
    int getScreenWidth();

    @Mutable
    @Accessor(value = "screenHeight")
    int getScreenHeight();

    @Accessor(value = "passes")
    List<PostPass> getPasses();
    #endif
}
