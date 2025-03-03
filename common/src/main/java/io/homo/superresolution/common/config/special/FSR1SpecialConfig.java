package io.homo.superresolution.common.config.special;

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
                        .setName(Component.literal("启用半精度(FP16)"))
                        .setTooltip(Component.literal("启用后可以提升性能但会损失一点画质"))
                        .setType(SpecialConfigDescription.ConfigType.BOOLEAN)
                        .setSaveConsumer((v) -> getSpecialConfigs().fsr1.fp16 = v)
                        .setDefaultValue(true)
        );
    }
}
