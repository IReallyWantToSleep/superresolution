package io.homo.superresolution.core.graphics.opengl.shader;

import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.impl.shader.ShaderDescription;
import io.homo.superresolution.core.graphics.impl.shader.ShaderSource;
import io.homo.superresolution.core.graphics.impl.shader.ShaderType;

public class GlBlitShader {
    private static GlShaderProgram shaderCache;

    public static GlShaderProgram getShader() {
        if (shaderCache == null) {
            shaderCache = RenderSystems.current().device().createShaderProgram(
                    ShaderDescription.create()
                            .fragment(new ShaderSource(ShaderType.FRAGMENT, "/shader/blit.frag.glsl", true))
                            .vertex(new ShaderSource(ShaderType.VERTEX, "/shader/blit.vert.glsl", true))
                            .uniformSamplerTexture("uTexture", 0)
                            .build());
            shaderCache.compile();

        }
        return shaderCache;
    }
}