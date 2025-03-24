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
                    .requireVulkan(true)
                    .developmentEnvironment(true),
            "NVIDIA Image Scaling",
            "NVIDIA Image Scaling"
    ),
    SGSR2(
            Requirement.nothing()
                    .glMajorVersion(4)
                    .glMinorVersion(5),
            "Snapdragon™ GSR 2",
            "Snapdragon™ Game Super Resolution 2"
    ),
    SGSR1(
            Requirement.nothing()
                    .glMajorVersion(4)
                    .glMinorVersion(2),
            "Snapdragon™ GSR 1",
            "Snapdragon™ Game Super Resolution 1"
    ),
    NONE(
            Requirement.nothing(),
            "None",
            "None"
    );
    private final Requirement requirement;
    private final String name;
    private final String fullName;

    AlgorithmType(Requirement requirement, String name, String fullName) {
        this.requirement = requirement;
        this.name = name;
        this.fullName = fullName;
    }

    public Component getFullName() {
        return Component.literal(fullName);
    }

    public Requirement getRequirement() {
        return requirement;
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
            case SGSR2 -> Component.translatable("superresolution.algo.display_name.sgsr2");
            case SGSR1 -> Component.translatable("superresolution.algo.display_name.sgsr1");
            case NONE -> Component.translatable("superresolution.algo.display_name.none");
        };
    }
}
