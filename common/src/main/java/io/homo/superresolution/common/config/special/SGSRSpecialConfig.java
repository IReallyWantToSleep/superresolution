package io.homo.superresolution.common.config.special;

import io.homo.superresolution.common.config.enums.SgsrVariant;
import net.minecraft.network.chat.Component;

import java.util.Map;

public class SGSRSpecialConfig extends SpecialConfig {
    public SgsrVariant variant = SgsrVariant.CS_2;

    @Override
    protected void buildDescriptions(Map<String, SpecialConfigDescription<?>> map) {
        map.put(
                "variant",
                new SpecialConfigDescription<>()
                        .setValue(getSpecialConfigs().sgsr.variant)
                        .setDefaultValue(SgsrVariant.CS_2)
                        .setName(Component.literal("变体"))
                        .setTooltip(Component.literal("CS_2 计算着色器/2通道\nCS_3 计算着色器/3通道\nFS_2 普通着色器/2通道"))
                        .setKey("variant")
                        .setSaveConsumer((v) -> getSpecialConfigs().sgsr.variant = (SgsrVariant) v)
                        .setType(SpecialConfigDescription.ConfigType.ENUM)
                        .setClazz(SgsrVariant.class)
        );
    }
}
