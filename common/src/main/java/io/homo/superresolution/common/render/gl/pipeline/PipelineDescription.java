package io.homo.superresolution.common.render.gl.pipeline;

import io.homo.superresolution.common.render.gl.shader.AbstractGlShaderProgram;

public record PipelineDescription(
        AbstractGlShaderProgram program,
        PipelineResourceDescriptions resourceDescriptions
) {

}
