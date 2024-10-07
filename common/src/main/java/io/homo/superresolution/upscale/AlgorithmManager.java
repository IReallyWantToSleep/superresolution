package io.homo.superresolution.upscale;

import com.mojang.blaze3d.vertex.PoseStack;
import io.homo.superresolution.upscale.fsr1.FSR1;
import io.homo.superresolution.upscale.fsr2.FSR2;
import io.homo.superresolution.upscale.none.None;
import io.homo.superresolution.upscale.utils.AlgorithmHelper;
import io.homo.superresolution.upscale.utils.Requirement;
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
    public static AbstractAlgorithm getAlgorithm(AlgorithmType type){
        AbstractAlgorithm algo = null;
        switch (type){
            case FSR1 -> algo = FSR1.create();
            case FSR2 -> algo = FSR2.create();
            case NONE -> algo = None.create();
        }
        if (algo != null){
            algo.init();
        }
        return algo;
    }

    public static boolean isSupportAlgorithm(AlgorithmType type) {
        boolean support = false;
        switch (type) {
            case FSR1 -> support = AlgorithmType.FSR1.getValue().check();
            case FSR2 -> support = AlgorithmType.FSR2.getValue().check();
            case NONE -> support = AlgorithmType.NONE.getValue().check();
        }
        return support;
    }

    public enum AlgorithmType {
        FSR1(
                Requirement.nothing()
                        .majorVersion(4)
                        .majorVersion(3)
        ),
        FSR2(
                Requirement.nothing()
                        .includeExtension("GL_KHR_shader_subgroup")
                        .majorVersion(4)
                        .majorVersion(5)
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
