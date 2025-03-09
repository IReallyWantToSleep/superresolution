package io.homo.superresolution.common.mixin.gui;

import net.minecraft.client.gui.components.AbstractWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

//sz麻将，明明可以改高度却不肯加个方法
@Mixin(AbstractWidget.class)
public interface AbstractWidgetAccessor {
    #if MC_VER < MC_1_20_4
    @Accessor
    void setHeight(int height);
    #endif
}
