package io.homo.superresolution.core.graphics.opengl;

import io.homo.superresolution.core.graphics.IRenderSystem;
import io.homo.superresolution.core.graphics.impl.shader.IShaderProgram;
import io.homo.superresolution.core.graphics.impl.shader.ShaderDescription;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.impl.texture.TextureDescription;
import io.homo.superresolution.core.graphics.impl.texture.TextureType;
import io.homo.superresolution.core.graphics.opengl.shader.GlShaderProgram;
import io.homo.superresolution.core.graphics.opengl.texture.GlTexture1D;
import io.homo.superresolution.core.graphics.opengl.texture.GlTexture2D;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL45;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL44.glClearTexImage;

public class GlRenderSystem implements IRenderSystem {
    private GlShaderProgram shaderProgram;

    @Override
    public void initRenderSystem() {
    }

    @Override
    public void destroyRenderSystem() {
    }

    @Override
    public ITexture createTexture(TextureDescription description) {
        if (description.getType() == TextureType.Texture2D) {
            return GlTexture2D.create(description);
        }
        if (description.getType() == TextureType.Texture1D) {
            return GlTexture1D.create(description);
        }
        return null;
    }

    @Override
    public GlShaderProgram createShaderProgram(ShaderDescription description) {
        return OpenGLShaderFactory.createShader(description);
    }

    @Override
    public void clearTexture(ITexture texture, int[] color) {
        if (texture == null) {
            throw new IllegalArgumentException("clearTexture: 输入的纹理对象为null");
        }

        float[] clearColor = new float[]{
                color[0] / 255.0f,
                color[1] / 255.0f,
                color[2] / 255.0f,
                color[3] / 255.0f
        };
        glClearTexImage(
                texture.getTextureId(), 0,
                texture.getTextureFormat().gl(),
                GL_FLOAT, clearColor
        );
    }

    @Override
    public void copyTexture(
            ITexture src,
            ITexture dst,
            int srcX0,
            int srcY0,
            int srcX1,
            int srcY1,
            int srcLevel,
            int dstX0,
            int dstY0,
            int dstX1,
            int dstY1,
            int dstLevel
    ) {
        if (src == null || dst == null) {
            throw new IllegalArgumentException("copyTexture: 源或目标纹理为null");
        }
        if (src.getTextureFormat() != dst.getTextureFormat()) {
            throw new IllegalArgumentException("copyTexture: 源和目标纹理格式不一致，无法拷贝：" +
                    src.getTextureFormat() + " -> " + dst.getTextureFormat());
        }
        if (src.getTextureType() != dst.getTextureType()) {
            throw new IllegalArgumentException("copyTexture: 源和目标纹理类型不一致，无法拷贝：" +
                    src.getTextureType() + " -> " + dst.getTextureType());
        }

        switch (src.getTextureType()) {
            case Texture1D:
                GL45.glCopyImageSubData(
                        src.getTextureId(), GL11.GL_TEXTURE_1D, srcLevel, srcX0, 0, 0,
                        dst.getTextureId(), GL11.GL_TEXTURE_1D, dstLevel, dstX0, 0, 0,
                        srcX1 - srcX0, 1, 1
                );
                break;
            case Texture2D:
                GL45.glCopyImageSubData(
                        src.getTextureId(), GL11.GL_TEXTURE_2D, srcLevel, srcX0, srcY0, 0,
                        dst.getTextureId(), GL11.GL_TEXTURE_2D, dstLevel, dstX0, dstY0, 0,
                        srcX1 - srcX0, srcY1 - srcY0, 1
                );
                break;
        }

    }

    @Override
    public void setViewport(int x, int y, int w, int h) {
        Gl.glViewport(x, y, w, h);
    }

    @Override
    public void setShaderProgram(IShaderProgram<?> shaderProgram) {
        this.shaderProgram = (GlShaderProgram) shaderProgram;
    }

    @Override
    public void dispatchCompute(int x, int y, int z) {
        if (this.shaderProgram == null) {
            throw new IllegalStateException("dispatchCompute: 未设置当前着色器程序");
        }
        Gl.glDispatchCompute(x, y, z);
    }
}