package io.homo.superresolution.core.graphics.opengl.utils;

import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.impl.shader.IShaderProgram;
import io.homo.superresolution.core.graphics.impl.shader.uniform.ShaderUniforms;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.impl.vertex.IVertexBuffer;
import io.homo.superresolution.core.graphics.impl.vertex.PrimitiveType;
import io.homo.superresolution.core.graphics.impl.vertex.VertexAttribute;
import io.homo.superresolution.core.graphics.impl.vertex.VertexBufferDescription;
import io.homo.superresolution.core.graphics.opengl.shader.GlBlitShader;
import io.homo.superresolution.core.graphics.system.IRenderSystem;

public class GlBlitRenderer {
    public static void blitToScreen(ITexture textureId, int viewWidth, int viewHeight) {
        IRenderSystem rs = RenderSystems.current();
        rs.renderState()
                .save()
                .colorMask(true, true, true, false)
                .depthTest(false)
                .depthWrite(false)
                .viewport(0, 0, viewWidth, viewHeight);
        IShaderProgram<?> blitShader = GlBlitShader.getShader();
        try (ShaderUniforms<?, ?, ?, ?, ?> uniforms = blitShader.uniforms()) {
            uniforms.samplerTexture("uTexture").set(
                    textureId
            );
        }
        rs.setShaderProgram(blitShader);
        float[] vertices = {
                -1f, -1f, 0f, 0f,
                1f, -1f, 1f, 0f,
                1f, 1f, 1f, 1f,

                -1f, -1f, 0f, 0f,
                1f, 1f, 1f, 1f,
                -1f, 1f, 0f, 1f
        };
        VertexBufferDescription desc = new VertexBufferDescription(vertices.length * Float.BYTES, false);
        IVertexBuffer vbo = rs.createVertexBuffer(desc);
        rs.uploadVertexData(vbo, vertices, 0, vertices.length);
        VertexAttribute[] attributes = new VertexAttribute[]{
                new VertexAttribute(0, 2, VertexAttribute.DataType.FLOAT, 4 * Float.BYTES, 0),
                new VertexAttribute(1, 2, VertexAttribute.DataType.FLOAT, 4 * Float.BYTES, 2 * Float.BYTES)
        };
        rs.setVertexAttributes(attributes);
        rs.draw(PrimitiveType.TRIANGLES, vbo, 0, 6);
        rs.destroyVertexBuffer(vbo);
        rs.renderState().restore();
        rs.setVertexAttributes(null);
    }
}
