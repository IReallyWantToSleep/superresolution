package io.homo.superresolution.core.graphics.opengl.shader;

import io.homo.superresolution.core.graphics.impl.shader.ShaderType;
import io.homo.superresolution.core.graphics.opengl.Gl;
import io.homo.superresolution.core.impl.Destroyable;
import org.lwjgl.opengl.GL45;

public class GlShader implements Destroyable {
    private int id;

    public GlShader(ShaderType type) {
        this.id = Gl.glCreateShader(switch (type) {
            case VERTEX -> GL45.GL_VERTEX_SHADER;
            case COMPUTE -> GL45.GL_COMPUTE_SHADER;
            case FRAGMENT -> GL45.GL_FRAGMENT_SHADER;
        });
    }

    public int id() {
        return id;
    }

    @Override
    public void destroy() {
        Gl.glDeleteShader(this.id);
        this.id = -1;
    }
}
