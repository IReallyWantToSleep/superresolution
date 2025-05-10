package io.homo.superresolution.core.gl.shader;

import io.homo.superresolution.core.impl.shader.ShaderSource;
import io.homo.superresolution.core.utils.FileReadHelper;

public class GlBlitShader extends GlGeneralShaderProgram {
    private static GlBlitShader shaderCache;

    private GlBlitShader() {
        super();
        this.shaderName = "BlitShader";
    }

    public static GlBlitShader getShader() {
        if (shaderCache == null) {
            shaderCache = new BlitShaderProgramBuilder()
                    .setShaderName("blit")
                    .addShaderSource(new ShaderSource(ShaderSource.Type.FRAGMENT, "/shader/blit.frag.glsl", true))
                    .addShaderSource(new ShaderSource(ShaderSource.Type.VERTEX, "/shader/blit.vert.glsl", true))
                    .build()
                    .compileShader();
        }
        return shaderCache;
    }

    @Override
    public GlBlitShader compileShader() {
        super.compileShader();
        return this;
    }

    public void bindTexture(int textureId) {
        uniforms().strictTexture("uTexture").value(textureId);
    }

    private static class BlitShaderProgramBuilder extends AbstractShaderProgramBuilder<GlBlitShader> {
        @Override
        public GlBlitShader build() {
            return updateShader(new GlBlitShader());
        }
    }
}