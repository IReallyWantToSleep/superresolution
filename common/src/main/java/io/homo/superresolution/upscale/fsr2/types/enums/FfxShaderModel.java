package io.homo.superresolution.upscale.fsr2.types.enums;

public enum FfxShaderModel {
    FFX_SHADER_MODEL_5_1(0),
    FFX_SHADER_MODEL_6_0(1),
    FFX_SHADER_MODEL_6_1(2),
    FFX_SHADER_MODEL_6_2(3),
    FFX_SHADER_MODEL_6_3(4),
    FFX_SHADER_MODEL_6_4(5),
    FFX_SHADER_MODEL_6_5(6),
    FFX_SHADER_MODEL_6_6(7),
    FFX_SHADER_MODEL_6_7(8);

    private final int value;

    FfxShaderModel(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}