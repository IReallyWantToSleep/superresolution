package io.homo.superresolution.common.upscale;

import io.homo.superresolution.common.upscale.utils.Requirement;

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
                    .glMinorVersion(5),
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
}
