package io.homo.superresolution.mixin;

import com.google.common.collect.Lists;
import io.homo.superresolution.SuperResolution;
import io.homo.superresolution.debug.DebugInfo;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
@Mixin(DebugScreenOverlay.class)
public class DebugMixin {
    @Redirect(method = "getSystemInformation", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Lists;newArrayList([Ljava/lang/Object;)Ljava/util/ArrayList;", remap = false))
    private ArrayList<String> redirectRightTextEarly(Object[] elements) {
        ArrayList<String> strings = Lists.newArrayList((String[]) elements);
        strings.add("");
        strings.add(DebugInfo.getTextFrameTimeDelta());
        strings.add(DebugInfo.getTextFrameTimeDeltaFSR2());
        return strings;
    }
}


