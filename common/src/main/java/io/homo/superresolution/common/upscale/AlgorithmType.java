package io.homo.superresolution.common.upscale;

import io.homo.superresolution.common.platform.Arch;
import io.homo.superresolution.common.platform.OS;
import io.homo.superresolution.common.platform.OSType;
import io.homo.superresolution.common.upscale.utils.Requirement;
import net.minecraft.network.chat.Component;

public enum AlgorithmType {
    FSR1(
            Requirement.nothing()
                    .glMajorVersion(4)
                    .glMinorVersion(3),
            "FSR1"
    ),
    NIS(
            Requirement.nothing()
                    .glMajorVersion(4)
                    .glMinorVersion(5)
                    .developmentEnvironment(true),
            "NVIDIA Image Scaling"
    ),
    FSR2(
            Requirement.nothing()
                    .includeExtension("GL_KHR_shader_subgroup")
                    .glMajorVersion(4)
                    .glMinorVersion(5)
                    .addIncludeOS(new OS(Arch.X86_64, OSType.WINDOWS))
                    .addIncludeOS(new OS(Arch.X86_64, OSType.LINUX)),
            "FSR2"
    ),
    NONE(
            Requirement.nothing(),
            "None"
    );
    private final Requirement value;
    private final String name;

    AlgorithmType(Requirement value, String name) {
        this.value = value;
        this.name = name;
    }

    public Requirement getValue() {
        return value;
    }

    @Override
    public String toString() {
        return name;
    }

    public String getString() {
        return switch (this) {
            case FSR1 -> Component.translatable("superresolution.algo.display_name.fsr1").getString();
            case NIS -> Component.translatable("superresolution.algo.display_name.nis").getString();
            case FSR2 -> Component.translatable("superresolution.algo.display_name.fsr2").getString();
            case NONE -> Component.translatable("superresolution.algo.display_name.none").getString();
        };
    }
}
