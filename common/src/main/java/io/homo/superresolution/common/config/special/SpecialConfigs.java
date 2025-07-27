package io.homo.superresolution.common.config.special;

import io.homo.superresolution.api.config.ModConfigSpecBuilder;
import io.homo.superresolution.core.impl.Pair;
import io.homo.superresolution.common.upscale.AlgorithmDescriptions;

import java.util.HashMap;
import java.util.Map;

public class SpecialConfigs {
    public FSR1SpecialConfig FSR1;
    public FSR2SpecialConfig FSR2;
    public SGSR2SpecialConfig SGSR2;
    public SGSR1SpecialConfig SGSR1;

    public transient Map<String, Pair<SpecialConfig, String>> description = new HashMap<>();

    public SpecialConfigs(ModConfigSpecBuilder builder) {
        builder.comment("special", "Algorithm special configuration");
        FSR1 = new FSR1SpecialConfig(builder);
        FSR2 = new FSR2SpecialConfig(builder);
        SGSR2 = new SGSR2SpecialConfig(builder);
        SGSR1 = new SGSR1SpecialConfig(builder);
        description.put("fsr1", Pair.of(FSR1, AlgorithmDescriptions.FSR1.getDisplayName()));
        description.put("fsr2", Pair.of(FSR2, AlgorithmDescriptions.FSR2.getDisplayName()));
        description.put("sgsr2", Pair.of(SGSR2, AlgorithmDescriptions.SGSR2.getDisplayName()));
        description.put("sgsr1", Pair.of(SGSR1, AlgorithmDescriptions.SGSR1.getDisplayName()));
    }
}
