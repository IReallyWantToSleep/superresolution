package io.homo.superresolution.mixin.gui;

import net.minecraft.client.gui.components.AbstractWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

//sz麻将，明明可以改高度却不肯加个方法
@Mixin(AbstractWidget.class)
public interface AbstractWidgetAccessor {
    @Accessor
    void setHeight(int height);
}
