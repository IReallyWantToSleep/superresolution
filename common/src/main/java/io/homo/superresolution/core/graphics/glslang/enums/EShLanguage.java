package io.homo.superresolution.core.graphics.glslang.enums;

public enum EShLanguage {
    EShLangVertex(0),
    EShLangTessControl(1),
    EShLangTessEvaluation(2),
    EShLangGeometry(3),
    EShLangFragment(4),
    EShLangCompute(5),
    EShLangRayGen(6),
    EShLangIntersect(7),
    EShLangAnyHit(8),
    EShLangClosestHit(9),
    EShLangMiss(10),
    EShLangCallable(11),
    EShLangTask(12),
    EShLangMesh(13),
    ;
    private final int value;

    EShLanguage(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
