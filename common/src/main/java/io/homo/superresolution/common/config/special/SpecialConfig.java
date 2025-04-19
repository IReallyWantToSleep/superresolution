package io.homo.superresolution.common.config.special;

import io.homo.superresolution.common.config.Config;

import java.util.HashMap;
import java.util.Map;

public abstract class SpecialConfig {
    public Map<String, SpecialConfigDescription<?>> getDescriptions() {
        Map<String, SpecialConfigDescription<?>> descriptions = new HashMap<>();
        buildDescriptions(descriptions);
        return descriptions;
    }

    protected SpecialConfigs getSpecialConfigs() {
        return Config.getInstance().getSpecial();
    }

    protected abstract void buildDescriptions(Map<String, SpecialConfigDescription<?>> map);
}
