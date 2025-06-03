package io.homo.superresolution.core.graphics.glslang.enums;

public enum EShMessages {
    EShMsgDefault(0),
    EShMsgRelaxedErrors((1)),
    EShMsgSuppressWarnings((1 << 1)),
    EShMsgAST((1 << 2)),
    EShMsgSpvRules((1 << 3)),
    EShMsgVulkanRules((1 << 4)),
    EShMsgOnlyPreprocessor((1 << 5)),
    EShMsgReadHlsl((1 << 6)),
    EShMsgCascadingErrors((1 << 7)),
    EShMsgKeepUncalled((1 << 8)),
    EShMsgHlslOffsets((1 << 9)),
    EShMsgDebugInfo((1 << 10)),
    EShMsgHlslEnable16BitTypes((1 << 11)),
    EShMsgHlslLegalization((1 << 12)),
    EShMsgHlslDX9Compatible((1 << 13)),
    EShMsgBuiltinSymbolTable((1 << 14)),
    EShMsgEnhanced((1 << 15)),
    EShMsgAbsolutePath((1 << 16)),
    EShMsgDisplayErrorColumn((1 << 17)),
    EShMsgLinkTimeOptimization((1 << 18)),
    EShMsgValidateCrossStageIO((1 << 19));
    private final int value;

    EShMessages(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
