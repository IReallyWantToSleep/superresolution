package io.homo.superresolution.common.config.special;

import io.homo.superresolution.api.config.ModConfigSpecBuilder;
import io.homo.superresolution.common.config.enums.SgsrVariant;
import net.minecraft.network.chat.Component;

import java.util.Map;

public class SGSR1SpecialConfig extends SpecialConfig {

    public SGSR1SpecialConfig(ModConfigSpecBuilder specBuilder) {
        super(specBuilder);
    }

    @Override
    protected void buildDescriptions(Map<String, SpecialConfigDescription<?>> map) {
    }
}
