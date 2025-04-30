package io.homo.superresolution.forge.mixin.compat.embeddium;

import me.jellysquid.mods.sodium.client.render.chunk.ShaderChunkRenderer;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.embeddedt.embeddium.client.gui.options.OptionIdentifier;
import org.embeddedt.embeddium.gui.EmbeddiumVideoOptionsScreen;
import org.embeddedt.embeddium.gui.frame.tab.Tab;
import net.minecraft.network.chat.Component;
import com.google.common.collect.Multimap;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.gui.ConfigScreenBuilder;

@Mixin(EmbeddiumVideoOptionsScreen.class)
public class EmbeddiumOptionMixin {
    @Inject(method = "createShaderPackButton", at = @At(value = "RETURN"), remap = false)
    private void addMyConfigScreen(Multimap<String, Tab<?>> tabs, CallbackInfo ci) {
        tabs.put(SuperResolution.MOD_ID,
                Tab.createBuilder()
                        .setTitle(Component.translatable("superresolution.name"))
                        .setId(OptionIdentifier.create(SuperResolution.MOD_ID, "emb_configscreen"))
                        .setOnSelectFunction(() -> {
                            Minecraft.getInstance().setScreen(ConfigScreenBuilder.create().buildConfigScreen((EmbeddiumVideoOptionsScreen) (Object) this));
                            return false;
                        }).build()
        );

    }
}
