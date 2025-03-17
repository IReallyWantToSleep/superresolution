package io.homo.superresolution.common.render.gl.shader;

import io.homo.superresolution.common.utils.FileReadHelper;

public class BlitShader extends GlGeneralShaderProgram {
    private static BlitShader shaderCache;

    private BlitShader() {
        super();
        this.shaderName = "BlitShader";
    }

    public static BlitShader getShader() {
        if (shaderCache == null) {
            shaderCache = ((BlitShader) (new BlitShaderProgramBuilder()
                    .setShaderName("blit")
                    .addAllFragShaderTextList(FileReadHelper.readText("/shader/blit.frag.glsl"))
                    .addAllVertShaderTextList(FileReadHelper.readText("/shader/blit.vert.glsl"))
                    .build()))
                    .compileShader();
        }
        return shaderCache;
    }

    @Override
    public BlitShader compileShader() {
        super.compileShader();
        return this;
    }

    public void bindTexture(int textureId) {
        this.setTexture("uTexture", textureId, 0);
    }

    private static class BlitShaderProgramBuilder extends GeneralShaderProgramBuilder {
        @Override
        public BlitShader build() {
            return (BlitShader) setShaderText(new BlitShader());
        }
    }

}