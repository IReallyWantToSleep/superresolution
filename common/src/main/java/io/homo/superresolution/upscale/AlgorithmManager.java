package io.homo.superresolution.upscale;

import com.mojang.blaze3d.vertex.PoseStack;
import io.homo.superresolution.upscale.fsr1.FSR1;
import io.homo.superresolution.upscale.fsr2.FSR2;
import io.homo.superresolution.upscale.none.None;
import io.homo.superresolution.upscale.utils.AlgorithmHelper;
import org.joml.Matrix4f;

public class AlgorithmManager{
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

    public enum AlgorithmType {
        FSR1,FSR2,NONE
    }

    public static AbstractAlgorithm getAlgorithm(AlgorithmType type){
        AbstractAlgorithm algo = null;
        switch (type){
            case FSR1 -> {
                algo = FSR1.create();
            }
            case FSR2 -> {
                algo = FSR2.create();
            }
            case NONE -> {
                algo = None.create();
            }

        }
        if (algo != null){
            algo.init();
        }
        return algo;
    }

    public static class AlgorithmParam{
        public PoseStack poseStack;
        public Matrix4f lastProjectionMatrix;
        public Matrix4f currentProjectionMatrix;
    }

    public static void setProjectionMatrix(Matrix4f cur){
        if (param.lastProjectionMatrix == null){
            param.lastProjectionMatrix = cur;
        }else {
            param.lastProjectionMatrix = param.currentProjectionMatrix;
        }
        param.currentProjectionMatrix = cur;
    }
}
