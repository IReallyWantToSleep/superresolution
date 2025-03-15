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
            "FSR1",
            "AMD FidelityFX Super Resolution 1"
    ),
    FSR2(
            Requirement.nothing()
                    .includeExtension("GL_KHR_shader_subgroup")
                    .glMajorVersion(4)
                    .glMinorVersion(5)
                    .addIncludeOS(new OS(Arch.X86_64, OSType.WINDOWS))
                    .addIncludeOS(new OS(Arch.X86_64, OSType.LINUX)),
            "FSR2",
            "AMD FidelityFX Super Resolution 2"
    ),
    NIS(
            Requirement.nothing()
                    .glMajorVersion(4)
                    .glMinorVersion(5)
                    .developmentEnvironment(true),
            "NVIDIA Image Scaling",
            "NVIDIA Image Scaling"
    ),
    SGSR(
            Requirement.nothing()
                    .glMajorVersion(4)
                    .glMinorVersion(5),
            "Snapdragon™ GSR",
            "Snapdragon™ Game Super Resolution"
    ),
    NONE(
            Requirement.nothing(),
            "None",
            "None"
    );
    private final Requirement value;
    private final String name;
    private final String fullName;

    AlgorithmType(Requirement value, String name, String fullName) {
        this.value = value;
        this.name = name;
        this.fullName = fullName;
    }

    public Component getFullName() {
        return Component.literal(fullName);
    }

    public Requirement getValue() {
        return value;
    }

    @Override
    public String toString() {
        return name;
    }

    public String getString() {
        return getComponent().getString();
    }

    public Component getComponent() {
        return switch (this) {
            case FSR1 -> Component.translatable("superresolution.algo.display_name.fsr1");
            case NIS -> Component.translatable("superresolution.algo.display_name.nis");
            case FSR2 -> Component.translatable("superresolution.algo.display_name.fsr2");
            case SGSR -> Component.translatable("superresolution.algo.display_name.sgsr");
            case NONE -> Component.translatable("superresolution.algo.display_name.none");
        };
    }
}
