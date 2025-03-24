package io.homo.superresolution.common.config;

import io.homo.superresolution.common.config.special.*;
import io.homo.superresolution.common.impl.Pair;
import io.homo.superresolution.common.upscale.AlgorithmType;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.Map;

public class SpecialConfigs {
    public FSR1SpecialConfig fsr1 = new FSR1SpecialConfig();
    public FSR2SpecialConfig fsr2 = new FSR2SpecialConfig();
    public NISSpecialConfig nis = new NISSpecialConfig();
    public SGSR2SpecialConfig sgsr2 = new SGSR2SpecialConfig();
    public SGSR1SpecialConfig sgsr1 = new SGSR1SpecialConfig();

    public transient Map<String, Pair<SpecialConfig, Component>> description = new HashMap<>();

    public SpecialConfigs() {
        description.put("fsr1", Pair.of(fsr1, AlgorithmType.FSR1.getComponent()));
        description.put("fsr2", Pair.of(fsr2, AlgorithmType.FSR2.getComponent()));
        description.put("nis", Pair.of(nis, AlgorithmType.NIS.getComponent()));
        description.put("sgsr2", Pair.of(sgsr2, AlgorithmType.SGSR2.getComponent()));
        description.put("sgsr1", Pair.of(sgsr1, AlgorithmType.SGSR1.getComponent()));
    }
}
