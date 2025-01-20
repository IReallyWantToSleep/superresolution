package io.homo.superresolution.upscale;

import io.homo.superresolution.upscale.utils.Requirement;

public enum AlgorithmType {
        FSR1(
                Requirement.nothing()
                        .majorVersion(4)
                        .minorVersion(3)
        ),
        FSR2(
                Requirement.nothing()
                        .includeExtension("GL_KHR_shader_subgroup")
                        .majorVersion(4)
                        .minorVersion(5)
        ),
        NONE(
                Requirement.nothing()
        );
        private final Requirement value;

        AlgorithmType(Requirement value) {
            this.value = value;
        }

        public Requirement getValue() {
            return value;
        }

}
