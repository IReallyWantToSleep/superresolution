package io.homo.superresolution.common.config.special;

import io.homo.superresolution.api.config.ModConfigSpecBuilder;
import io.homo.superresolution.common.config.SuperResolutionConfig;

import java.util.HashMap;
import java.util.Map;

public abstract class SpecialConfig {
    protected final ModConfigSpecBuilder specBuilder;

    public SpecialConfig(ModConfigSpecBuilder specBuilder) {
        this.specBuilder = specBuilder;
    }

    public Map<String, SpecialConfigDescription<?>> getDescriptions() {
        Map<String, SpecialConfigDescription<?>> descriptions = new HashMap<>();
        buildDescriptions(descriptions);
        return descriptions;
    }

    protected abstract void buildDescriptions(Map<String, SpecialConfigDescription<?>> map);

    protected SpecialConfigs getSpecialConfigs() {
        return SuperResolutionConfig.SPECIAL;
    }
}
