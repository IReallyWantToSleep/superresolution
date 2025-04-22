package io.homo.superresolution.common.config.special;

import io.homo.superresolution.common.config.ConfigSpecType;
import io.homo.superresolution.common.config.enums.SgsrVariant;
import net.minecraft.network.chat.Component;

import java.util.Map;
import java.util.Optional;

public class SGSR2SpecialConfig extends SpecialConfig {
    public SgsrVariant variant = SgsrVariant.CS_2;

    @Override
    protected void buildDescriptions(Map<String, SpecialConfigDescription<?>> map) {
        map.put(
                "variant",
                new SpecialConfigDescription<>()
                        .setValue(getSpecialConfigs().sgsr2.variant)
                        .setDefaultValue(SgsrVariant.CS_2)
                        .setNameSupplier((variant) -> switch ((SgsrVariant) variant) {
                            case CS_2 -> Optional.of(Component.translatable("superresolution.enum.sgsrvariant.cs_2"));
                            case CS_3 -> Optional.of(Component.translatable("superresolution.enum.sgsrvariant.cs_3"));
                            case FS_2 -> Optional.of(Component.translatable("superresolution.enum.sgsrvariant.fs_2"));
                        })
                        .setTooltip(Component.translatable("superresolution.screen.config.special.sgsr2.variant.tooltip"))
                        .setKey("variant")
                        .setSaveConsumer((v) -> getSpecialConfigs().sgsr2.variant = (SgsrVariant) v)
                        .setType(ConfigSpecType.ENUM)
                        .setClazz(SgsrVariant.class)
        );
    }
}
