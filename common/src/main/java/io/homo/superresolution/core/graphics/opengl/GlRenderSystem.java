package io.homo.superresolution.core.graphics.opengl;

import io.homo.superresolution.core.graphics.impl.DrawObject;
import io.homo.superresolution.core.graphics.impl.buffer.IBuffer;
import io.homo.superresolution.core.graphics.impl.framebuffer.FrameBufferBindPoint;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.core.graphics.impl.shader.IShaderProgram;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.impl.texture.TextureFormat;
import io.homo.superresolution.core.graphics.impl.texture.TextureType;
import io.homo.superresolution.core.graphics.opengl.framebuffer.GlFrameBuffer;
import io.homo.superresolution.core.graphics.opengl.shader.GlShaderProgram;
import io.homo.superresolution.core.graphics.opengl.shader.uniform.GlShaderBaseUniform;
import io.homo.superresolution.core.graphics.opengl.shader.uniform.GlShaderUniformBuffer;
import io.homo.superresolution.core.graphics.opengl.shader.uniform.GlShaderUniformSamplerTexture;
import io.homo.superresolution.core.graphics.opengl.shader.uniform.GlShaderUniformStorageTexture;
import io.homo.superresolution.core.graphics.system.IRenderState;
import io.homo.superresolution.core.graphics.system.IRenderSystem;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GL44;

import java.util.Map;

import static org.lwjgl.opengl.GL41.*;

public class GlRenderSystem implements IRenderSystem {
    private GlDevice device;
    public boolean supportsARBClearTexture;

    @Override
    public void initRenderSystem() {
        this.device = new GlDevice();
        supportsARBClearTexture = GL.getCapabilities().GL_ARB_clear_texture || GL.getCapabilities().OpenGL44;
    }

    @Override
    public void destroyRenderSystem() {
    }

    @Override
    public GlDevice device() {
        return device;
    }


    @Override
    public void finish() {
        glFinish();
    }
}