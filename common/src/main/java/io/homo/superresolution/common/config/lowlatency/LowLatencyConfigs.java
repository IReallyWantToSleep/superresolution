/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package io.homo.superresolution.common.config.lowlatency;

import io.homo.superresolution.api.config.ModConfigSpec;
import io.homo.superresolution.api.config.ModConfigSpecBuilder;
import io.homo.superresolution.api.config.values.single.EnumValue;
import io.homo.superresolution.core.SuperResolutionConstants;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class LowLatencyConfigs {
    private static final Map<String, ModConfigSpec> configs = new HashMap<>();

    private LowLatencyConfigs() {
    }

    public static ModConfigSpec getOrCreate(String providerId) {
        return configs.computeIfAbsent(providerId, LowLatencyConfigs::createSpec);
    }

    public static ModConfigSpec get(String providerId) {
        return configs.get(providerId);
    }

    public static ModConfigSpecBuilder newBuilder(String providerId) {
        return new ModConfigSpecBuilder().configPath(configPathFor(providerId)).autoSave(true);
    }

    public static Path configPathFor(String providerId) {
        return SuperResolutionConstants.DATA_DIR.getPath().resolve("low_latency").resolve(providerId + ".toml");
    }

    private static ModConfigSpec createSpec(String providerId) {
        return newBuilder(providerId).build();
    }
}
