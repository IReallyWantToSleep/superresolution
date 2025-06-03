package io.homo.superresolution.common.config.special;

import io.homo.superresolution.core.impl.Pair;
import io.homo.superresolution.common.upscale.AlgorithmDescriptions;

import java.util.HashMap;
import java.util.Map;

public class SpecialConfigs {
    public FSR1SpecialConfig fsr1 = new FSR1SpecialConfig();
    public FSR2SpecialConfig fsr2 = new FSR2SpecialConfig();
    public NISSpecialConfig nis = new NISSpecialConfig();
    public SGSR2SpecialConfig sgsr2 = new SGSR2SpecialConfig();
    public SGSR1SpecialConfig sgsr1 = new SGSR1SpecialConfig();

    public transient Map<String, Pair<SpecialConfig, String>> description = new HashMap<>();

    public SpecialConfigs() {
        description.put("fsr1", Pair.of(fsr1, AlgorithmDescriptions.FSR1.getDisplayName()));
        description.put("fsr2", Pair.of(fsr2, AlgorithmDescriptions.FSR2.getDisplayName()));
        //description.put("nis", Pair.of(nis, AlgorithmDescriptions.NIS.getDisplayName()));
        description.put("sgsr2", Pair.of(sgsr2, AlgorithmDescriptions.SGSR2.getDisplayName()));
        description.put("sgsr1", Pair.of(sgsr1, AlgorithmDescriptions.SGSR1.getDisplayName()));
    }
}
