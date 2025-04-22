package io.homo.superresolution.common.config.special;

import io.homo.superresolution.common.config.ConfigSpecType;
import net.minecraft.network.chat.Component;

import java.util.Map;

public class FSR1SpecialConfig extends SpecialConfig {
    public boolean fp16 = true;

    @Override
    protected void buildDescriptions(Map<String, SpecialConfigDescription<?>> map) {
        map.put(
                "fp16",
                new SpecialConfigDescription<Boolean>()
                        .setValue(getSpecialConfigs().fsr1.fp16)
                        .setKey("fp16")
                        .setName(Component.translatable("superresolution.screen.config.special.fsr1.fp16.name"))
                        .setTooltip(Component.translatable("superresolution.screen.config.special.fsr1.fp16.tooltip"))
                        .setType(ConfigSpecType.BOOLEAN)
                        .setSaveConsumer((v) -> getSpecialConfigs().fsr1.fp16 = v)
                        .setDefaultValue(true)
        );
    }
}
