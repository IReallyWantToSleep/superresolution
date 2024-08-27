package io.homo.superresolution.fsr2.types.enums;

public enum FfxFsr2InitializationFlagBits {

    FFX_FSR2_ENABLE_HIGH_DYNAMIC_RANGE                (1),   ///< A bit indicating if the input color data provided is using a high-dynamic range.
    FFX_FSR2_ENABLE_DISPLAY_RESOLUTION_MOTION_VECTORS (1<<1),   ///< A bit indicating if the motion vectors are rendered at display resolution.
    FFX_FSR2_ENABLE_MOTION_VECTORS_JITTER_CANCELLATION(1<<2),   ///< A bit indicating that the motion vectors have the jittering pattern applied to them.
    FFX_FSR2_ENABLE_DEPTH_INVERTED                    (1<<3),   ///< A bit indicating that the input depth buffer data provided is inverted [1..0].
    FFX_FSR2_ENABLE_DEPTH_INFINITE                    (1<<4),   ///< A bit indicating that the input depth buffer data provided is using an infinite far plane.
    FFX_FSR2_ENABLE_AUTO_EXPOSURE                     (1<<5),   ///< A bit indicating if automatic exposure should be applied to input color data.
    FFX_FSR2_ENABLE_DYNAMIC_RESOLUTION                (1<<6),   ///< A bit indicating that the application uses dynamic resolution scaling.
    FFX_FSR2_ENABLE_TEXTURE1D_USAGE                   (1<<7),   ///< A bit indicating that the backend should use 1D textures.
    FFX_FSR2_ENABLE_DEBUG_CHECKING                    (1<<8),   ///< A bit indicating that the runtime should check some API values and report issues.
    FFX_FSR2_ALLOW_NULL_DEVICE_AND_COMMAND_LIST       (1<<9);  ///< A bit indicating that the runtime should not check for null device/command list.
    private final int value;

    FfxFsr2InitializationFlagBits(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}