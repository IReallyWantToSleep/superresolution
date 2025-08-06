package io.homo.superresolution.srapi;

public enum SRSurfaceFormat {
    UNKNOWN(0),
    R32G32B32A32_TYPELESS(1),
    R32G32B32A32_UINT(2),
    R32G32B32A32_FLOAT(3),
    R16G16B16A16_FLOAT(4),
    R32G32B32_FLOAT(5),
    R32G32_FLOAT(6),
    R8_UINT(7),
    R32_UINT(8),
    R8G8B8A8_TYPELESS(9),
    R8G8B8A8_UNORM(10),
    R8G8B8A8_SNORM(11),
    R8G8B8A8_SRGB(12),
    B8G8R8A8_TYPELESS(13),
    B8G8R8A8_UNORM(14),
    B8G8R8A8_SRGB(15),
    R11G11B10_FLOAT(16),
    R10G10B10A2_UNORM(17),
    R16G16_FLOAT(18),
    R16G16_UINT(19),
    R16G16_SINT(20),
    R16_FLOAT(21),
    R16_UINT(22),
    R16_UNORM(23),
    R16_SNORM(24),
    R8_UNORM(25),
    R8G8_UNORM(26),
    R8G8_UINT(27),
    R32_FLOAT(28),
    R9G9B9E5_SHAREDEXP(29),
    R16G16B16A16_TYPELESS(30),
    R32G32_TYPELESS(31),
    R10G10B10A2_TYPELESS(32),
    R16G16_TYPELESS(33),
    R16_TYPELESS(34),
    R8_TYPELESS(35),
    R8G8_TYPELESS(36),
    R32_TYPELESS(37);

    public final int value;

    SRSurfaceFormat(int value) {
        this.value = value;
    }

    public static SRSurfaceFormat fromValue(int value) {
        for (SRSurfaceFormat fmt : values()) {
            if (fmt.value == value) {
                return fmt;
            }
        }
        throw new IllegalArgumentException("Unknown SRSurfaceFormat value: " + value);
    }

    public int getValue() {
        return value;
    }
}

