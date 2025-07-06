package io.homo.superresolution.core.graphics.glslang;

import io.homo.superresolution.core.graphics.glslang.enums.GlslangCompileShaderError;

import java.nio.ByteBuffer;

public class GlslangCompileShaderResult {
    private final String sourceCode;
    private final String preprocessedCode;
    private final long spirVDataSize;
    private final ByteBuffer spirvBuffer;
    private final GlslangCompileShaderError error;
    private final String log;

    public GlslangCompileShaderResult(
            String sourceCode,
            String preprocessedCode,
            int error,
            long spirVDataSize,
            ByteBuffer spirvBuffer,
            String log
    ) {
        this.sourceCode = sourceCode;
        this.preprocessedCode = preprocessedCode;
        this.spirVDataSize = spirVDataSize;
        this.spirvBuffer = spirvBuffer;
        this.log = log;
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

    public ByteBuffer spirvBuffer() {
        return spirvBuffer;
    }

    public GlslangCompileShaderError error() {
        return error;
    }

    @Override
    public String toString() {
        return "GlslangCompileShaderResult{" +
                "sourceCode='" + sourceCode + '\'' +
                ", preprocessedCode='" + preprocessedCode + '\'' +
                ", spirVDataSize=" + spirVDataSize +
                ", spirvBuffer=" + spirvBuffer +
                ", error=" + error +
                ", log='" + log + '\'' +
                '}';
    }
}