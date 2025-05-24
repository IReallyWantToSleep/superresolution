package io.homo.superresolution.common.gui.effect;

import io.homo.superresolution.common.debug.imgui.ImguiMain;
import io.homo.superresolution.common.platform.Platform;
import io.homo.superresolution.common.minecraft.MinecraftRenderHandle;
import io.homo.superresolution.core.gl.Gl;
import io.homo.superresolution.core.gl.GlState;
import io.homo.superresolution.core.gl.framebuffer.GlFrameBuffer;
import io.homo.superresolution.core.gl.shader.GlGeneralShaderProgram;
import io.homo.superresolution.core.gl.texture.GlTexture2D;
import io.homo.superresolution.core.gl.vertex.GlVertexArray;
import io.homo.superresolution.core.gl.vertex.GlVertexBuffer;
import io.homo.superresolution.core.impl.framebuffer.FrameBufferAttachmentType;
import io.homo.superresolution.core.impl.shader.ShaderSource;
import io.homo.superresolution.core.impl.texture.ITexture;
import io.homo.superresolution.core.impl.texture.TextureFormat;
import io.homo.superresolution.core.impl.framebuffer.FrameBufferBindPoint;
import org.lwjgl.opengl.GL45;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.glDepthMask;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;

public class BlurRenderer {
    public static final GlGeneralShaderProgram BLUR;
    public static final GlFrameBuffer blurFrameBuffer;
    public static final GlTexture2D blurTempTexture;
    public static final int MAX_LEVELS = 8;
    private static final float[] blurWeights = new float[MAX_LEVELS];

    static {
        BLUR = GlGeneralShaderProgram.create()
                .addShaderSource(new ShaderSource(ShaderSource.Type.FRAGMENT, "/shader/gui_blur/blur.frag.glsl", true))
                .addShaderSource(new ShaderSource(ShaderSource.Type.VERTEX, "/shader/gui_blur/blur.vert.glsl", true))
                .setShaderName("gui-blur")
                .build();
        blurFrameBuffer = GlFrameBuffer.create(
                TextureFormat.RGBA8,
                null,
                MinecraftRenderHandle.getScreenWidth(),
                MinecraftRenderHandle.getScreenHeight()
        );
        blurTempTexture = GlTexture2D.create(
                MinecraftRenderHandle.getScreenWidth(),
                MinecraftRenderHandle.getScreenHeight(),
                TextureFormat.RGBA8,
                GlTexture2D.AUTO_MIPMAP_LEVEL
        );
    }

    public static void compileShader() {
        if (!BLUR.compiled) BLUR.compileShader();
    }

    public static void copyTextureAndGenMipmap() {
        if (
                MinecraftRenderHandle.getOriginRenderTarget().getWidth() != blurTempTexture.getWidth() ||
                        MinecraftRenderHandle.getOriginRenderTarget().getHeight() != blurTempTexture.getHeight()
        ) {
            blurTempTexture.resize(
                    MinecraftRenderHandle.getOriginRenderTarget().getWidth(),
                    MinecraftRenderHandle.getOriginRenderTarget().getHeight()
            );
        }
        if (
                MinecraftRenderHandle.getOriginRenderTarget().getWidth() != blurFrameBuffer.getWidth() ||
                        MinecraftRenderHandle.getOriginRenderTarget().getHeight() != blurFrameBuffer.getHeight()
        ) {
            blurFrameBuffer.resizeFrameBuffer(
                    MinecraftRenderHandle.getOriginRenderTarget().getWidth(),
                    MinecraftRenderHandle.getOriginRenderTarget().getHeight()
            );
        }
        GlTexture2D dst = blurTempTexture;
        ITexture src = MinecraftRenderHandle.getOriginRenderTarget().getTexture(FrameBufferAttachmentType.COLOR);
        dst.copyFromTex(src.getTextureId());
        dst.generateMipmap();
    }

    public static void genBlurWeights(double phi, double m) {
        double a = (1 / (m * (Math.sqrt(2 * Math.PI))));
        double sum = 0.0;
        for (int i = 0; i < MAX_LEVELS; ++i) {
            double b = Math.pow(
                    Math.E,
                    -(Math.pow(i - phi, 2) / (2 * m * m))
            );
            blurWeights[i] = (float) (a * b);
            sum += blurWeights[i];
        }
        for (int i = 0; i < MAX_LEVELS; ++i) {
            blurWeights[i] /= (float) sum;
        }
    }

    public static void renderBlur() {
        compileShader();
        GlGeneralShaderProgram blurShader = BLUR;
        if (Platform.currentPlatform.isDevelopmentEnvironment()) {
            genBlurWeights(
                    ImguiMain.getInstance().imguiLayer.blurPhi[0],
                    ImguiMain.getInstance().imguiLayer.blurM[0]
            );
        } else {
            genBlurWeights(
                    3.716,
                    0.69
            );
        }
        copyTextureAndGenMipmap();
        try (GlState ignored = new GlState()) {
            glDisable(GL_DEPTH_TEST);
            glDepthMask(false);
            blurShader.use();
            blurFrameBuffer.bind(FrameBufferBindPoint.ALL);
            try (GlVertexArray vaoObj = new GlVertexArray();
                 GlVertexBuffer vbo = new GlVertexBuffer()) {
                float[] vertices = {
                        -1f, -1f, 0f, 0f,
                        1f, -1f, 1f, 0f,
                        1f, 1f, 1f, 1f,
                        -1f, 1f, 0f, 1f
                };
                int vao = vaoObj.id();
                Gl.DSA.vertexArrayVertexBuffer(
                        vao,
                        0,
                        vbo.getId(),
                        0,
                        4 * Float.BYTES
                );

                Gl.DSA.vertexArrayAttribFormat(
                        vao,
                        0,
                        2,
                        GL_FLOAT,
                        false,
                        0
                );
                Gl.DSA.enableVertexArrayAttrib(vao, 0);
                Gl.DSA.vertexArrayAttribBinding(vao, 0, 0);
                Gl.DSA.vertexArrayAttribFormat(
                        vao,
                        1,
                        2,
                        GL_FLOAT,
                        false,
                        2 * Float.BYTES
                );
                Gl.DSA.enableVertexArrayAttrib(vao, 1);
                Gl.DSA.vertexArrayAttribBinding(vao, 1, 0);

                vbo.uploadData(vertices, GL_STATIC_DRAW);

                Gl.DSA.bindVertexArray(vao);
                blurShader.uniforms()
                        .strictTexture("uTexture").value(blurTempTexture)
                        .strictVec4("weightA").value(blurWeights[0], blurWeights[1], blurWeights[2], blurWeights[3])
                        .strictVec4("weightB").value(blurWeights[4], blurWeights[5], blurWeights[6], blurWeights[7]);
                glDrawArrays(GL_TRIANGLE_FAN, 0, 4);
            }
            blurShader.clear();
            glDepthMask(true);
        }
    }
}
