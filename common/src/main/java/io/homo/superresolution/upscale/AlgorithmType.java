package io.homo.superresolution.upscale;

import io.homo.superresolution.upscale.utils.Requirement;

public enum AlgorithmType {
    FSR1(
            Requirement.nothing()
                    .majorVersion(4)
                    .minorVersion(3),
            "FSR1"
    ),
    NIS(
            Requirement.nothing()
                    .majorVersion(4)
                    .minorVersion(5),
            "NVIDIA Image Scaling"
    ),
    FSR2(
            Requirement.nothing()
                    .includeExtension("GL_KHR_shader_subgroup")
                    .majorVersion(4)
                    .minorVersion(5),
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
