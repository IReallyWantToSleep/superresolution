package io.homo.superresolution.core.gl.shader;

import io.homo.superresolution.core.utils.FileReadHelper;

public class BlitShader extends GlGeneralShaderProgram {
    private static BlitShader shaderCache;

    private BlitShader() {
        super();
        this.shaderName = "BlitShader";
    }

    public static BlitShader getShader() {
        if (shaderCache == null) {
            shaderCache = new BlitShaderProgramBuilder()
                    .setShaderName("blit")
                    .addAllFragShaderTextList(FileReadHelper.readText("/shader/blit.frag.glsl"))
                    .addAllVertShaderTextList(FileReadHelper.readText("/shader/blit.vert.glsl"))
                    .build()
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

    private static class BlitShaderProgramBuilder extends AbstractShaderProgramBuilder<BlitShader> {
        @Override
        public BlitShader build() {
            return checkShaderCache() ? updateShader(new BlitShader().fromBin(getShaderCache())) : updateShader(new BlitShader());
        }
    }
}