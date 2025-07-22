package io.homo.superresolution.common.config.special;

import io.homo.superresolution.api.config.ModConfigSpecBuilder;
import io.homo.superresolution.api.config.values.single.EnumValue;
import io.homo.superresolution.common.config.ConfigSpecType;
import io.homo.superresolution.common.config.enums.SgsrVariant;
import net.minecraft.network.chat.Component;

import java.util.Map;
import java.util.Optional;

public class SGSR2SpecialConfig extends SpecialConfig {
    public EnumValue<SgsrVariant> VARIANT = specBuilder.defineEnum(
            "special/sgsr2/variant",
            SgsrVariant.class,
            () -> SgsrVariant.CS_2
    );

    public SGSR2SpecialConfig(ModConfigSpecBuilder specBuilder) {
        super(specBuilder);
    }

    @Override
    protected void buildDescriptions(Map<String, SpecialConfigDescription<?>> map) {
        map.put(
                "variant",
                new SpecialConfigDescription<>()
                        .setValue(getSpecialConfigs().SGSR2.VARIANT.get())
                        .setDefaultValue(SgsrVariant.CS_2)
                        .setValueNameSupplier((variant) -> switch ((SgsrVariant) variant) {
                            case CS_2 -> Optional.of(Component.translatable("superresolution.enum.sgsrvariant.cs_2"));
                            case CS_3 -> Optional.of(Component.translatable("superresolution.enum.sgsrvariant.cs_3"));
                            case FS_2 -> Optional.of(Component.translatable("superresolution.enum.sgsrvariant.fs_2"));
                        })
                        .setName(Component.translatable("superresolution.screen.config.special.sgsr2.variant.name"))
                        .setTooltip(Component.translatable("superresolution.screen.config.special.sgsr2.variant.tooltip"))
                        .setKey("variant")
                        .setSaveConsumer((v) -> getSpecialConfigs().SGSR2.VARIANT.set(v))
                        .setType(ConfigSpecType.ENUM)
                        .setClazz(SgsrVariant.class)
        );
    }
}
