package io.homo.superresolution.common.upscale.nis.struct;

public class NISConfig {
    public static final int SIZE = 256;
    public float kDetectRatio;
    public float kDetectThres;
    public float kMinContrastRatio;
    public float kRatioNorm;
    public float kContrastBoost;
    public float kEps;
    public float kSharpStartY;
    public float kSharpScaleY;
    public float kSharpStrengthMin;
    public float kSharpStrengthScale;
    public float kSharpLimitMin;
    public float kSharpLimitScale;
    public float kScaleX;
    public float kScaleY;
    public float kDstNormX;
    public float kDstNormY;
    public float kSrcNormX;
    public float kSrcNormY;
    public int kInputViewportOriginX;
    public int kInputViewportOriginY;
    public int kInputViewportWidth;
    public int kInputViewportHeight;
    public int kOutputViewportOriginX;
    public int kOutputViewportOriginY;
    public int kOutputViewportWidth;
    public int kOutputViewportHeight;
    public float reserved0;
    public float reserved1;
}