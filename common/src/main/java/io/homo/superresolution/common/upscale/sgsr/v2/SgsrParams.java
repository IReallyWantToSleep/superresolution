package io.homo.superresolution.common.upscale.sgsr.v2;

import io.homo.superresolution.common.impl.Vec2;

import io.homo.superresolution.common.render.impl.IUniformStruct;
import io.homo.superresolution.common.upscale.DispatchResource;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;

public class SgsrParams implements IUniformStruct {
    private final ByteBuffer container;
    private int sameFrameNum = 0;
    // 新增字段 prev_view_proj_matrix 用于保存上一帧的视图投影矩阵
    private Matrix4f prev_view_proj_matrix;

    public SgsrParams() {
        this.container = MemoryStack.stackCalloc(sizeof());
    }

    private static boolean isCameraStill(Matrix4f currentMVP, Matrix4f prevMVP, float threshold) {
        float diff = 0;
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                diff += Math.abs(currentMVP.get(r, c) - prevMVP.get(r, c));
            }
        }
        return diff < threshold;
    }

    public void setRenderSize(Vec2 renderSize) {
        container.putInt(0, (int) renderSize.x);
        container.putInt(4, (int) renderSize.y);
    }

    public void setDisplaySize(Vec2 displaySize) {
        container.putInt(8, (int) displaySize.x);
        container.putInt(12, (int) displaySize.y);
    }

    public void setRenderSizeRcp(Vec2 renderSizeRcp) {
        container.putFloat(16, renderSizeRcp.x);
        container.putFloat(20, renderSizeRcp.y);

    }

    public void setDisplaySizeRcp(Vec2 displaySizeRcp) {
        container.putFloat(24, displaySizeRcp.x);
        container.putFloat(28, displaySizeRcp.y);
    }

    public void setJitterOffset(Vec2 jitterOffset) {
        container.putFloat(32, jitterOffset.x);
        container.putFloat(36, jitterOffset.y);
    }

    public void setClipToPrevClip(Matrix4f clipToPrevClip) {
        clipToPrevClip.get(48, container);
    }

    public void setPreExposure(float preExposure) {
        container.putFloat(112, preExposure);
    }

    public void setCameraFovAngleHor(float cameraFovAngleHor) {
        container.putFloat(116, cameraFovAngleHor);
    }

    public void setCameraNear(float cameraNear) {
        container.putFloat(120, cameraNear);
    }

    public void setMinLerpContribution(float minLerpContribution) {
        container.putFloat(124, minLerpContribution);
    }

    public void setbSameCamera(boolean bSameCamera) {
        container.putInt(128, bSameCamera ? 1 : 0);
    }

    public void setReset(boolean reset) {
        container.putInt(132, reset ? 1 : 0);
    }

    public void fillZero(int start, int end) {
        for (int i = start; i < end; i++) {
            container.put(i, (byte) 0);
        }
    }

    public void updateData(DispatchResource dispatchResource) {
        container.clear();
        setRenderSize(dispatchResource.renderSize());
        setDisplaySize(dispatchResource.screenSize());
        setRenderSizeRcp(dispatchResource.renderSize().divideInto(1.0f));
        setDisplaySizeRcp(dispatchResource.screenSize().divideInto(1.0f));
        setJitterOffset(new Vec2(0.0f));
        fillZero(40, 48);
        //const auto curr_view_proj_matrix        = m_cameraProjection * m_cameraView;
        //const auto inv_current_view_proj_matrix = glm::inverse( m_cameraView ) * glm::inverse( m_cameraProjection );
        //const auto mt                           = m_prevViewProjection * inv_current_view_proj_matrix;
        Matrix4f curr_view_proj_matrix = new Matrix4f(dispatchResource.projectionMatrix()).mul(dispatchResource.viewMatrix());
        Matrix4f inv_current_view_proj_matrix = new Matrix4f(dispatchResource.viewMatrix()).invert().mul(new Matrix4f(dispatchResource.projectionMatrix()).invert());
        Matrix4f clipToPrevClipMat = new Matrix4f(dispatchResource.lastViewMatrix()).mul(inv_current_view_proj_matrix);
        setClipToPrevClip(clipToPrevClipMat);
        setPreExposure(1.2f);
        setCameraFovAngleHor(dispatchResource.horizontalFov());
        setCameraNear(dispatchResource.cameraNear());
        boolean isCameraStill = isCameraStill(
                curr_view_proj_matrix,
                (prev_view_proj_matrix == null ? curr_view_proj_matrix : prev_view_proj_matrix),
                1e-5f
        );
        double MinLerpContribution = 0.0;
        if (isCameraStill) {
            sameFrameNum += 1;
            if (sameFrameNum > 5) {
                MinLerpContribution = 0.3;
            }
            if (sameFrameNum == 0xFFFF) {
                sameFrameNum = 1;
            }
        } else {
            sameFrameNum = 0;
        }
        setMinLerpContribution((float) MinLerpContribution);
        setbSameCamera(isCameraStill);
        fillZero(136, 144);
        container.position(144);
        container.flip();
        prev_view_proj_matrix = new Matrix4f(curr_view_proj_matrix);
    }

    @Override
    public ByteBuffer container() {
        return container;
    }

    @Override
    public int sizeof() {
        return 144;
    }
}
