package io.homo.superresolution.core.graphics.opengl;

import io.homo.superresolution.core.graphics.impl.vertex.*;
import io.homo.superresolution.core.graphics.system.IRenderState;
import io.homo.superresolution.core.graphics.system.IRenderSystem;
import io.homo.superresolution.core.graphics.impl.shader.IShaderProgram;
import io.homo.superresolution.core.graphics.impl.shader.ShaderDescription;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.impl.texture.TextureDescription;
import io.homo.superresolution.core.graphics.impl.texture.TextureType;
import io.homo.superresolution.core.graphics.opengl.shader.GlShaderProgram;
import io.homo.superresolution.core.graphics.opengl.texture.GlTexture1D;
import io.homo.superresolution.core.graphics.opengl.texture.GlTexture2D;
import io.homo.superresolution.core.graphics.opengl.vertex.GlVertexBuffer;
import org.lwjgl.opengl.*;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL44.glClearTexImage;

public class GlRenderSystem implements IRenderSystem {
    private GlShaderProgram shaderProgram;
    private GlRenderState renderState;
    // 存储最近一次 setVertexAttributes 设置的属性布局
    private VertexAttribute[] pendingVertexAttributes = null;

    public GlShaderProgram getShaderProgram() {
        return shaderProgram;
    }

    @Override
    public void setShaderProgram(IShaderProgram<?> shaderProgram) {
        this.shaderProgram = (GlShaderProgram) shaderProgram;
    }

    @Override
    public void initRenderSystem() {
        this.renderState = new GlRenderState();
    }

    @Override
    public void destroyRenderSystem() {
        this.renderState = null;
        this.pendingVertexAttributes = null;
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
        float[] clearColor = new float[]{
                color[0] / 255.0f,
                color[1] / 255.0f,
                color[2] / 255.0f,
                color[3] / 255.0f
        };
        clearTexture(texture, clearColor);
    }

    @Override
    public void clearTexture(ITexture texture, float[] color) {
        if (texture == null) {
            throw new OpenGLException("clearTexture: 输入的纹理对象为null");
        }
        glClearTexImage(
                texture.getTextureId(), 0,
                texture.getTextureFormat().gl(),
                GL_FLOAT, color
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
            throw new OpenGLException("copyTexture: 源或目标纹理为null");
        }
        if (src.getTextureFormat() != dst.getTextureFormat()) {
            throw new OpenGLException("copyTexture: 源和目标纹理格式不一致，无法拷贝：" +
                    src.getTextureFormat() + " -> " + dst.getTextureFormat());
        }
        if (src.getTextureType() != dst.getTextureType()) {
            throw new OpenGLException("copyTexture: 源和目标纹理类型不一致，无法拷贝：" +
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
    public void dispatchCompute(int x, int y, int z) {
        if (this.shaderProgram == null) {
            throw new OpenGLException("dispatchCompute: 未设置当前着色器程序");
        }
        try (GlState state = new GlState()) {
            GL45.glUseProgram(this.shaderProgram.handle);
            Gl.glDispatchCompute(x, y, z);
        }
    }

    @Override
    public IVertexBuffer createVertexBuffer(VertexBufferDescription description) {
        return GlVertexBuffer.create(description);
    }

    @Override
    public void uploadVertexData(IVertexBuffer vertexBuffer, float[] data, int offset, int length) {
        if (!(vertexBuffer instanceof GlVertexBuffer)) {
            throw new IllegalArgumentException("uploadVertexData: Only GlVertexBuffer is supported");
        }
        vertexBuffer.updateData(data, offset, length);
    }

    @Override
    public void setVertexAttributes(VertexAttribute[] attributes) {
        this.pendingVertexAttributes = attributes;
    }

    private void applyVertexAttributes(VertexAttribute[] attributes) {
        if (attributes == null) return;
        for (VertexAttribute attr : attributes) {
            GL20.glEnableVertexAttribArray(attr.getLocation());
            GL20.glVertexAttribPointer(
                    attr.getLocation(),
                    attr.getComponentCount(),
                    attr.getDataType() == VertexAttribute.DataType.FLOAT ? GL11.GL_FLOAT : GL11.GL_INT,
                    false,
                    attr.getStride(),
                    attr.getOffset()
            );
        }
    }

    @Override
    public void draw(PrimitiveType primitiveType, IVertexBuffer vertexBuffer, int firstVertex, int vertexCount) {
        if (!(vertexBuffer instanceof GlVertexBuffer)) {
            throw new OpenGLException("draw: 参数vertexBuffer的类型不为GlVertexBuffer");
        }
        if (this.shaderProgram == null) {
            throw new OpenGLException("draw: 未设置当前着色器程序");
        }
        try (GlState ig = new GlState()) {
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vertexBuffer.getId());
            applyVertexAttributes(this.pendingVertexAttributes);
            int glPrimitive = switch (primitiveType) {
                case TRIANGLES -> GL11.GL_TRIANGLES;
                case LINES -> GL11.GL_LINES;
                case POINTS -> GL11.GL_POINTS;
            };
            GL11.glDrawArrays(glPrimitive, firstVertex, vertexCount);
            for (VertexAttribute attr : this.pendingVertexAttributes) {
                GL20.glDisableVertexAttribArray(attr.getLocation());
            }
        }
    }

    @Override
    public void destroyVertexBuffer(IVertexBuffer vertexBuffer) {
        if (vertexBuffer != null) {
            vertexBuffer.destroy();
        }
    }

    @Override
    public IRenderState renderState() {
        return renderState;
    }
}