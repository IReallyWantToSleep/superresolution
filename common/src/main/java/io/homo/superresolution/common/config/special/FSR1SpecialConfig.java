package io.homo.superresolution.common.config.special;

import io.homo.superresolution.api.SuperResolutionAPI;
import io.homo.superresolution.api.config.ModConfigSpecBuilder;
import io.homo.superresolution.api.config.values.single.BooleanValue;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.ConfigSpecType;
import io.homo.superresolution.common.upscale.AlgorithmDescriptions;
import net.minecraft.network.chat.Component;

import java.util.Map;

public class FSR1SpecialConfig extends SpecialConfig {
    public BooleanValue FP16 = this.specBuilder.defineBoolean(
            "special/fsr1/fp16",
            () -> true,
            ""
    );

    public FSR1SpecialConfig(ModConfigSpecBuilder specBuilder) {
        super(specBuilder);
    }

    @Override
    protected void buildDescriptions(Map<String, SpecialConfigDescription<?>> map) {
        map.put(
                "fp16",
                new SpecialConfigDescription<Boolean>()
                        .setValue(getSpecialConfigs().FSR1.FP16.get())
                        .setKey("fp16")
                        .setName(Component.translatable("superresolution.screen.config.special.fsr1.fp16.name"))
                        .setTooltip(Component.translatable("superresolution.screen.config.special.fsr1.fp16.tooltip"))
                        .setType(ConfigSpecType.BOOLEAN)
                        .setSaveConsumer((v) -> {
                            getSpecialConfigs().FSR1.FP16.set(v);
                            if (SuperResolutionAPI.getCurrentAlgorithmDescription() == AlgorithmDescriptions.FSR1) {
                                SuperResolution.recreateAlgorithm();
                            }
                        })
                        .setDefaultValue(true)
        );
    }
}
