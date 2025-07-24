package io.homo.superresolution.common.config.special;

import io.homo.superresolution.api.SuperResolutionAPI;
import io.homo.superresolution.api.config.ModConfigSpecBuilder;
import io.homo.superresolution.api.config.values.single.BooleanValue;
import io.homo.superresolution.api.config.values.single.EnumValue;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.ConfigSpecType;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.upscale.AlgorithmDescriptions;
import io.homo.superresolution.thirdparty.fsr2.common.Fsr2Version;
import net.minecraft.network.chat.Component;

import java.util.Map;
import java.util.Optional;

public class FSR2SpecialConfig extends SpecialConfig {

    public EnumValue<Fsr2Version> VERSION = specBuilder.defineEnum(
            "special/fsr2/version",
            Fsr2Version.class,
            () -> Fsr2Version.V233
    );
    public BooleanValue FP16 = this.specBuilder.defineBoolean(
            "special/fsr2/fp16",
            () -> true,
            ""
    );

    public FSR2SpecialConfig(ModConfigSpecBuilder specBuilder) {
        super(specBuilder);
    }

    @Override
    protected void buildDescriptions(Map<String, SpecialConfigDescription<?>> map) {
        map.put(
                "fp16",
                new SpecialConfigDescription<Boolean>()
                        .setValue(getSpecialConfigs().FSR2.FP16.get())
                        .setKey("fp16")
                        .setName(Component.translatable("superresolution.screen.config.special.fsr2.fp16.name"))
                        .setTooltip(Component.translatable("superresolution.screen.config.special.fsr2.fp16.tooltip"))
                        .setType(ConfigSpecType.BOOLEAN)
                        .setSaveConsumer((v) -> {
                            getSpecialConfigs().FSR2.FP16.set(v);
                            if (SuperResolutionAPI.getCurrentAlgorithmDescription() == AlgorithmDescriptions.FSR2) {
                                SuperResolution.recreateAlgorithm();
                            }
                        })
                        .setDefaultValue(true)
        );
        map.put(
                "version",
                new SpecialConfigDescription<>()
                        .setValue(getSpecialConfigs().FSR2.VERSION.get())
                        .setDefaultValue(Fsr2Version.V233)
                        .setValueNameSupplier((variant) -> switch ((Fsr2Version) variant) {
                            case V233 -> Optional.of(Component.literal("2.3.3"));
                            case V221 -> Optional.of(Component.literal("2.2.1"));
                        })
                        .setName(Component.translatable("superresolution.screen.config.special.fsr2.version.name"))
                        .setTooltip(Component.translatable("superresolution.screen.config.special.fsr2.version.tooltip"))
                        .setKey("version")
                        .setSaveConsumer((v) -> {
                            getSpecialConfigs().FSR2.VERSION.set(v);
                            if (SuperResolutionAPI.getCurrentAlgorithmDescription() == AlgorithmDescriptions.FSR2) {
                                SuperResolution.recreateAlgorithm();
                            }
                        })
                        .setType(ConfigSpecType.ENUM)
                        .setClazz(Fsr2Version.class)
        );
    }
}
