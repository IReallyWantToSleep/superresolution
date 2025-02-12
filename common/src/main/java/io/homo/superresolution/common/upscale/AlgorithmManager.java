package io.homo.superresolution.common.upscale;

import io.homo.superresolution.common.upscale.fsr1.FSR1;
import io.homo.superresolution.common.upscale.fsr2.FSR2;
import io.homo.superresolution.common.upscale.nis.NVIDIAImageScaling;
import io.homo.superresolution.common.upscale.none.None;
import io.homo.superresolution.common.upscale.utils.AlgorithmHelper;
import org.joml.Matrix4f;

public class AlgorithmManager {
    public static AlgorithmHelper helper;
    public static AlgorithmParam param = new AlgorithmParam();

    static {
        helper = new AlgorithmHelper();
    }

    public static void destroy() {
        helper.destroy();
    }

    public static void resize(int width, int height) {
        helper.resize(width, height);
    }

    public static AbstractAlgorithm getAlgorithm(AlgorithmType type) {
        AbstractAlgorithm algo = null;
        switch (type) {
            case FSR1 -> algo = FSR1.create();
            case FSR2 -> algo = FSR2.create();
            case NIS -> algo = NVIDIAImageScaling.create();
            case NONE -> algo = None.create();
        }
        if (algo != null) {
            algo.init();
        }
        return algo;
    }

    public static boolean isSupportAlgorithm(AlgorithmType type) {
        boolean support = false;
        switch (type) {
            case FSR1 -> support = AlgorithmType.FSR1.getValue().check().support();
            case NIS -> support = AlgorithmType.NIS.getValue().check().support();
            case FSR2 -> support = AlgorithmType.FSR2.getValue().check().support();
            case NONE -> support = AlgorithmType.NONE.getValue().check().support();
        }
        return support;
    }

    public static void setProjectionMatrix(Matrix4f cur) {
        //SuperResolution.LOGGER.info(param.currentProjectionMatrix.toString());
        if (param.lastProjectionMatrix == null) {
            param.lastProjectionMatrix = cur;
        } else {
            param.lastProjectionMatrix = param.currentProjectionMatrix;
        }
        param.currentProjectionMatrix = cur;
    }

    public static void setModelViewMatrix(Matrix4f cur) {
        if (param.lastModelViewMatrix == null) {
            param.lastModelViewMatrix = cur;
        } else {
            param.lastModelViewMatrix = param.currentModelViewMatrix;
        }
        param.currentModelViewMatrix = cur;
    }

    public static void setFov(double fov) {
        param.fov = fov;
    }

    public static class AlgorithmParam {
        public Matrix4f lastProjectionMatrix;
        public Matrix4f currentProjectionMatrix;
        public Matrix4f currentModelViewMatrix;
        public Matrix4f lastModelViewMatrix;
        public double fov = 11.4514f;
    }
}
