package io.homo.superresolution.core.glslang;

import io.homo.superresolution.core.glslang.enums.GlslangCompileShaderError;

public class GlslangCompileShaderResult {
    private final String sourceCode;
    private final String preprocessedCode;
    private final long spirVDataSize;
    private final String spirVFilePath;
    private final GlslangCompileShaderError error;
    private final String log;

    public GlslangCompileShaderResult(String sourceCode, String preprocessedCode, int error, long spirVDataSize, String spirVFilePath, String log) {
        this.sourceCode = sourceCode;
        this.preprocessedCode = preprocessedCode;
        if (error == GlslangCompileShaderError.OK.getValue()) {
            this.error = GlslangCompileShaderError.OK;
        } else if (error == GlslangCompileShaderError.LINK_ERROR.getValue()) {
            this.error = GlslangCompileShaderError.LINK_ERROR;
        } else if (error == GlslangCompileShaderError.PREPROCESS_ERROR.getValue()) {
            this.error = GlslangCompileShaderError.PREPROCESS_ERROR;
        } else if (error == GlslangCompileShaderError.PARSE_ERROR.getValue()) {
            this.error = GlslangCompileShaderError.PARSE_ERROR;
        } else {
            this.error = GlslangCompileShaderError.OK;
        }
        this.spirVDataSize = spirVDataSize;
        this.spirVFilePath = spirVFilePath;
        this.log = log;
    }

    public String log() {
        return log;
    }

    public String sourceCode() {
        return sourceCode;
    }

    public String preprocessedCode() {
        return preprocessedCode;
    }

    public long spirVDataSize() {
        return spirVDataSize;
    }

    public String spirVFilePath() {
        return spirVFilePath;
    }

    public GlslangCompileShaderError error() {
        return error;
    }
}
