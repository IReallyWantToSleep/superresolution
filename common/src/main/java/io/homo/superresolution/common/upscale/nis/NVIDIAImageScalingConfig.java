package io.homo.superresolution.common.upscale.nis;

import io.homo.superresolution.core.graphics.impl.IUniformStruct;
import io.homo.superresolution.common.upscale.DispatchResource;
import io.homo.superresolution.common.upscale.nis.enums.NISHDRMode;
import io.homo.superresolution.common.upscale.nis.struct.NISConfig;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class NVIDIAImageScalingConfig implements IUniformStruct {
    public NISConfig config = new NISConfig();
    public ByteBuffer container;

    public NVIDIAImageScalingConfig() {
        container = MemoryStack.stackCalloc(NISConfig.SIZE);
    }

    public boolean NVScalerUpdateConfig(float sharpness,
                                        int inputViewportOriginX, int inputViewportOriginY,
                                        int inputViewportWidth, int inputViewportHeight,
                                        int inputTextureWidth, int inputTextureHeight,
                                        int outputViewportOriginX, int outputViewportOriginY,
                                        int outputViewportWidth, int outputViewportHeight,
                                        int outputTextureWidth, int outputTextureHeight,
                                        NISHDRMode hdrMode) {
        sharpness = Math.max(Math.min(1.f, sharpness), 0.f);
        float sharpenSlider = sharpness - 0.5f;

        float maxScale = (sharpenSlider >= 0.0f) ? 1.25f : 1.75f;
        float minScale = (sharpenSlider >= 0.0f) ? 1.25f : 1.0f;
        float limitScale = (sharpenSlider >= 0.0f) ? 1.25f : 1.0f;

        float kDetectRatio = 2 * 1127.f / 1024.f;

        float kDetectThres = 64.0f / 1024.0f;
        float kMinContrastRatio = 2.0f;
        float kMaxContrastRatio = 10.0f;

        float kSharpStartY = 0.45f;
        float kSharpEndY = 0.9f;
        float kSharpStrengthMin = Math.max(0.0f, 0.4f + sharpenSlider * minScale * 1.2f);
        float kSharpStrengthMax = 1.6f + sharpenSlider * maxScale * 1.8f;
        float kSharpLimitMin = Math.max(0.1f, 0.14f + sharpenSlider * limitScale * 0.32f);
        float kSharpLimitMax = 0.5f + sharpenSlider * limitScale * 0.6f;

        if (hdrMode == NISHDRMode.Linear || hdrMode == NISHDRMode.PQ) {
            kDetectThres = 32.0f / 1024.0f;
            kMinContrastRatio = 1.5f;
            kMaxContrastRatio = 5.0f;
            kSharpStrengthMin = Math.max(0.0f, 0.4f + sharpenSlider * minScale * 1.1f);
            kSharpStrengthMax = 2.2f + sharpenSlider * maxScale * 1.8f;
            kSharpLimitMin = Math.max(0.06f, 0.10f + sharpenSlider * limitScale * 0.28f);
            kSharpLimitMax = 0.6f + sharpenSlider * limitScale * 0.6f;

            if (hdrMode == NISHDRMode.PQ) {
                kSharpStartY = 0.35f;
                kSharpEndY = 0.55f;
            } else {
                kSharpStartY = 0.3f;
                kSharpEndY = 0.5f;
            }
        }

        float kRatioNorm = 1.0f / (kMaxContrastRatio - kMinContrastRatio);
        float kSharpScaleY = 1.0f / (kSharpEndY - kSharpStartY);
        float kSharpStrengthScale = kSharpStrengthMax - kSharpStrengthMin;
        float kSharpLimitScale = kSharpLimitMax - kSharpLimitMin;

        config.kInputViewportWidth = inputViewportWidth == 0 ? inputTextureWidth : inputViewportWidth;
        config.kInputViewportHeight = inputViewportHeight == 0 ? inputTextureHeight : inputViewportHeight;
        config.kOutputViewportWidth = outputViewportWidth == 0 ? outputTextureWidth : outputViewportWidth;
        config.kOutputViewportHeight = outputViewportHeight == 0 ? outputTextureHeight : outputViewportHeight;

        if (config.kInputViewportWidth == 0 || config.kInputViewportHeight == 0 ||
                config.kOutputViewportWidth == 0 || config.kOutputViewportHeight == 0) {
            return false;
        }

        config.kInputViewportOriginX = inputViewportOriginX;
        config.kInputViewportOriginY = inputViewportOriginY;
        config.kOutputViewportOriginX = outputViewportOriginX;
        config.kOutputViewportOriginY = outputViewportOriginY;

        config.kSrcNormX = 1.f / inputTextureWidth;
        config.kSrcNormY = 1.f / inputTextureHeight;
        config.kDstNormX = 1.f / outputTextureWidth;
        config.kDstNormY = 1.f / outputTextureHeight;
        config.kScaleX = (float) config.kInputViewportWidth / config.kOutputViewportWidth;
        config.kScaleY = (float) config.kInputViewportHeight / config.kOutputViewportHeight;
        config.kDetectRatio = kDetectRatio;
        config.kDetectThres = kDetectThres;
        config.kMinContrastRatio = kMinContrastRatio;
        config.kRatioNorm = kRatioNorm;
        config.kContrastBoost = 1.0f;
        config.kEps = 1.0f / 255.0f;
        config.kSharpStartY = kSharpStartY;
        config.kSharpScaleY = kSharpScaleY;
        config.kSharpStrengthMin = kSharpStrengthMin;
        config.kSharpStrengthScale = kSharpStrengthScale;
        config.kSharpLimitMin = kSharpLimitMin;
        config.kSharpLimitScale = kSharpLimitScale;

        return !(config.kScaleX < 0.5f) && !(config.kScaleX > 1.f) && !(config.kScaleY < 0.5f) && !(config.kScaleY > 1.f);
    }

    public boolean NVSharpenUpdateConfig(float sharpness,
                                         int inputViewportOriginX, int inputViewportOriginY,
                                         int inputViewportWidth, int inputViewportHeight,
                                         int inputTextureWidth, int inputTextureHeight,
                                         int outputViewportOriginX, int outputViewportOriginY,
                                         NISHDRMode hdrMode) {
        return NVScalerUpdateConfig(sharpness, inputViewportOriginX, inputViewportOriginY,
                inputViewportWidth, inputViewportHeight, inputTextureWidth, inputTextureHeight,
                outputViewportOriginX, outputViewportOriginY, inputViewportWidth, inputViewportHeight,
                inputTextureWidth, inputTextureHeight, hdrMode);
    }

    public void updateData(DispatchResource dispatchResource) {
        container.clear();
        container.order(ByteOrder.LITTLE_ENDIAN);
        container.putFloat(config.kDetectRatio);
        container.putFloat(config.kDetectThres);
        container.putFloat(config.kMinContrastRatio);
        container.putFloat(config.kRatioNorm);
        container.putFloat(config.kContrastBoost);
        container.putFloat(config.kEps);
        container.putFloat(config.kSharpStartY);
        container.putFloat(config.kSharpScaleY);
        container.putFloat(config.kSharpStrengthMin);
        container.putFloat(config.kSharpStrengthScale);
        container.putFloat(config.kSharpLimitMin);
        container.putFloat(config.kSharpLimitScale);
        container.putFloat(config.kScaleX);
        container.putFloat(config.kScaleY);
        container.putFloat(config.kDstNormX);
        container.putFloat(config.kDstNormY);
        container.putFloat(config.kSrcNormX);
        container.putFloat(config.kSrcNormY);
        container.putInt(config.kInputViewportOriginX);
        container.putInt(config.kInputViewportOriginY);
        container.putInt(config.kInputViewportWidth);
        container.putInt(config.kInputViewportHeight);
        container.putInt(config.kOutputViewportOriginX);
        container.putInt(config.kOutputViewportOriginY);
        container.putInt(config.kOutputViewportWidth);
        container.putInt(config.kOutputViewportHeight);
        container.putFloat(config.reserved0);
        container.putFloat(config.reserved1);
        int writtenBytes = 18 * 4 + 8 * 4 + 2 * 4;
        while (writtenBytes < NISConfig.SIZE) {
            container.put((byte) 0);
            writtenBytes++;
        }
        container.position(256);
        container.flip();
    }

    @Override
    public ByteBuffer container() {
        return container;
    }

    @Override
    public long size() {
        return 256;
    }
}
