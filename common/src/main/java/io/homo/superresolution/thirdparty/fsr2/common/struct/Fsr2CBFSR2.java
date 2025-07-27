package io.homo.superresolution.thirdparty.fsr2.common.struct;

import io.homo.superresolution.core.graphics.impl.buffer.IBufferData;
import io.homo.superresolution.core.math.Vector2f;
import io.homo.superresolution.thirdparty.fsr2.common.Fsr2Context;
import io.homo.superresolution.thirdparty.fsr2.common.Fsr2Dimensions;
import io.homo.superresolution.thirdparty.fsr2.common.Fsr2DispatchDescription;
import org.lwjgl.system.MemoryUtil;

import java.nio.Buffer;
import java.nio.ByteBuffer;

import static io.homo.superresolution.thirdparty.fsr2.common.Fsr2Utils.ffxFsr2GetJitterPhaseCount;

public class Fsr2CBFSR2 implements IBufferData {
    private final ByteBuffer container;
    private final int[] renderSize = new int[2];
    private final int[] maxRenderSize = new int[2];
    private final int[] displaySize = new int[2];
    private final int[] inputColorResourceDimensions = new int[2];
    private final int[] lumaMipDimensions = new int[2];
    private final float[] deviceToViewDepth = new float[4];
    private final float[] jitter = new float[2];
    private final float[] previousJitterOffset = new float[2];
    private final float[] motionVectorScale = new float[2];
    private final float[] downscaleFactor = new float[2];
    private final float[] motionVectorJitterCancellation = new float[2];
    private int lumaMipLevelToUse = 0;
    private int frameIndex = 0;
    private float preExposure = 1.0f;
    private float previousFramePreExposure = 1.0f;
    private float tanHalfFOV = 0;
    private float jitterSequenceLength = 1.0f;
    private float deltaTime = 0;
    private float dynamicResChangeFactor = 1.0f;
    private float viewSpaceToMetersFactor = 1.0f;

    public Fsr2CBFSR2() {
        this.container = MemoryUtil.memCalloc((int) size());
    }

    @Override
    public void free() {
        MemoryUtil.memFree(container);
    }

    @Override
    public void put(byte[] src, long offset) {
        throw new RuntimeException();
    }

    @Override
    public void updatePartial(Buffer data, long offset, long length) {
        throw new RuntimeException();
    }

    @Override
    public void update(Buffer data) {
        throw new RuntimeException();
    }

    public void fillBuffer() {
        container.putInt(0, renderSize[0]);
        container.putInt(4, renderSize[1]);
        container.putInt(8, maxRenderSize[0]);
        container.putInt(12, maxRenderSize[1]);
        container.putInt(16, displaySize[0]);
        container.putInt(20, displaySize[1]);
        container.putInt(24, inputColorResourceDimensions[0]);
        container.putInt(28, inputColorResourceDimensions[1]);
        container.putInt(32, lumaMipDimensions[0]);
        container.putInt(36, lumaMipDimensions[1]);
        container.putInt(40, lumaMipLevelToUse);
        container.putInt(44, frameIndex);

        container.putFloat(48, deviceToViewDepth[0]);
        container.putFloat(52, deviceToViewDepth[1]);
        container.putFloat(56, deviceToViewDepth[2]);
        container.putFloat(60, deviceToViewDepth[3]);

        container.putFloat(64, jitter[0]);
        container.putFloat(68, jitter[1]);
        container.putFloat(72, motionVectorScale[0]);
        container.putFloat(76, motionVectorScale[1]);
        container.putFloat(80, downscaleFactor[0]);
        container.putFloat(84, downscaleFactor[1]);
        container.putFloat(88, motionVectorJitterCancellation[0]);
        container.putFloat(92, motionVectorJitterCancellation[1]);

        container.putFloat(96, preExposure);
        container.putFloat(100, previousFramePreExposure);
        container.putFloat(104, tanHalfFOV);
        container.putFloat(108, jitterSequenceLength);
        container.putFloat(112, deltaTime);
        container.putFloat(116, dynamicResChangeFactor);
        container.putFloat(120, viewSpaceToMetersFactor);
        container.position(128);
        container.flip();
    }

    public void update(Fsr2Context context, Fsr2DispatchDescription desc, Fsr2Dimensions dims) {
        renderSize[0] = (int) desc.renderSize().x;
        renderSize[1] = (int) desc.renderSize().y;
        maxRenderSize[0] = (int) desc.renderSize().x;
        maxRenderSize[1] = (int) desc.renderSize().y;
        displaySize[0] = dims.screenWidth();
        displaySize[1] = dims.screenHeight();
        inputColorResourceDimensions[0] = (int) desc.renderSize().x;
        inputColorResourceDimensions[1] = (int) desc.renderSize().y;
        lumaMipLevelToUse = 4;
        int mipLevel = 4;
        int mipDiv = 1 << mipLevel;
        lumaMipDimensions[0] = (int) (desc.renderSize().x / mipDiv);
        lumaMipDimensions[1] = (int) (desc.renderSize().y / mipDiv);
        frameIndex = 0;
        computeDeviceToViewDepth(
                desc.cameraNear,
                desc.cameraFar,
                context.config.getFlags().isEnableDepthInverted(),
                context.config.getFlags().isEnableDepthInfinite(),
                (int) desc.renderSize().x, (int) desc.renderSize().y,
                desc.cameraFovAngleVertical
        );
        Vector2f jitterVec = desc.jitterOffset() != null ? desc.jitterOffset() : new Vector2f(0, 0);
        jitter[0] = jitterVec.x;
        jitter[1] = jitterVec.y;
        Vector2f mvScale = desc.motionVectorScale() != null ? desc.motionVectorScale() : new Vector2f(1, 1);
        motionVectorScale[0] = mvScale.x;
        motionVectorScale[1] = mvScale.y;
        downscaleFactor[0] = dims.renderWidth() / (float) dims.screenWidth();
        downscaleFactor[1] = dims.renderHeight() / (float) dims.screenHeight();
        if (context.config.getFlags().isEnableMotionVectorsJitterCancellation()) {
            int width = context.config.getFlags().isEnableDisplayResolutionMotionVectors() ? dims.screenWidth() : dims.renderWidth();
            int height = context.config.getFlags().isEnableDisplayResolutionMotionVectors() ? dims.screenHeight() : dims.renderHeight();
            motionVectorJitterCancellation[0] = (previousJitterOffset[0] - jitter[0]) / width;
            motionVectorJitterCancellation[1] = (previousJitterOffset[1] - jitter[1]) / height;
            previousJitterOffset[0] = jitter[0];
            previousJitterOffset[1] = jitter[1];
        }
        previousFramePreExposure = preExposure;
        preExposure = desc.preExposure();
        float aspect = (float) dims.renderWidth() / (float) Math.max(1, dims.renderHeight());
        float cameraFovAngleHorizontal = (float) (Math.atan(Math.tan(desc.cameraFovAngleVertical() / 2) * aspect) * 2.0);
        tanHalfFOV = (float) Math.tan(cameraFovAngleHorizontal * 0.5);
        int jitterPhaseCount = ffxFsr2GetJitterPhaseCount(dims.renderWidth(), dims.screenWidth());
        if (desc.reset() || jitterSequenceLength == 0) {
            jitterSequenceLength = jitterPhaseCount;
        } else {
            int jitterPhaseCountDelta = (int) (jitterPhaseCount - jitterSequenceLength);
            if (jitterPhaseCountDelta > 0) {
                jitterSequenceLength++;
            } else if (jitterPhaseCountDelta < 0) {
                jitterSequenceLength--;
            }
        }
        deltaTime = desc.frameTimeDelta();
        dynamicResChangeFactor = 0.0f;
        viewSpaceToMetersFactor = desc.viewSpaceToMetersFactor() > 0 ? desc.viewSpaceToMetersFactor() : 1.0f;
        fillBuffer();
    }

    @Override
    public ByteBuffer container() {
        return container;
    }

    @Override
    public long size() {
        return 128;
    }

    public void computeDeviceToViewDepth(
            float cameraNear, float cameraFar,
            boolean depthInverted, boolean depthInfinite,
            float renderWidth, float renderHeight,
            float cameraFovAngleVertical
    ) {
        float fMin = Math.min(cameraNear, cameraFar);
        float fMax = Math.max(cameraNear, cameraFar);
        boolean bInverted = depthInverted;
        boolean bInfinite = depthInfinite;
        if (bInverted) {
            float tmp = fMin;
            fMin = fMax;
            fMax = tmp;
        }
        float fQ = fMax / (fMin - fMax);
        float d = -1.0f;
        final float FLT_EPSILON = 1e-7f;
        float[][] matrix_elem_c = {
                {fQ, -1.0f - FLT_EPSILON},
                {fQ, 0.0f + FLT_EPSILON}
        };
        float[][] matrix_elem_e = {
                {fQ * fMin, -fMin - FLT_EPSILON},
                {fQ * fMin, fMax}
        };
        int iInverted = bInverted ? 1 : 0;
        int iInfinite = bInfinite ? 1 : 0;
        this.deviceToViewDepth[0] = d * matrix_elem_c[iInverted][iInfinite];
        this.deviceToViewDepth[1] = matrix_elem_e[iInverted][iInfinite];
        float aspect = renderWidth / Math.max(1.0f, renderHeight);
        float cotHalfFovY = (float) (Math.cos(0.5 * cameraFovAngleVertical) / Math.sin(0.5 * cameraFovAngleVertical));
        float a = cotHalfFovY / aspect;
        float b = cotHalfFovY;
        this.deviceToViewDepth[2] = 1.0f / a;
        this.deviceToViewDepth[3] = 1.0f / b;
    }
}