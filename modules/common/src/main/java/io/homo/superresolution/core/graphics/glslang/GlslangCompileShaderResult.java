/*
 * Super Resolution
 * Copyright (c) 2025-2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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