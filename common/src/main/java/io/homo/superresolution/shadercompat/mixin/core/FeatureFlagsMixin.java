package io.homo.superresolution.shadercompat.mixin.core;

import org.spongepowered.asm.mixin.*;

import net.irisshaders.iris.features.FeatureFlags;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.BooleanSupplier;

@Mixin(value = FeatureFlags.class, remap = false)
@Unique
public abstract class FeatureFlagsMixin {
    @Shadow(remap = false)
    @Final
    @Mutable
    private static FeatureFlags[] $VALUES;

    private static final FeatureFlags SR_UPSCALE = featureFlags$addVariant(() -> true, () -> true);

    @Invoker("<init>")
    public static FeatureFlags featureFlags$invokeInit(String name, int o, BooleanSupplier irisRequirement, BooleanSupplier hardwareRequirement) {
        throw new AssertionError();
    }

    @Unique
    private static FeatureFlags featureFlags$addVariant(BooleanSupplier irisRequirement, BooleanSupplier hardwareRequirement) {
        ArrayList<FeatureFlags> variants = new ArrayList<>(Arrays.asList(FeatureFlags.values()));
        FeatureFlags instrument = featureFlags$invokeInit("SR_UPSCALE", variants.get(variants.size() - 1).ordinal() + 1, irisRequirement, hardwareRequirement);
        variants.add(instrument);
        FeatureFlagsMixin.$VALUES = variants.toArray(new FeatureFlags[0]);
        return instrument;
    }
}
