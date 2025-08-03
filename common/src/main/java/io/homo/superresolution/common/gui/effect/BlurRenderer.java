package io.homo.superresolution.common.gui.effect;

import io.homo.superresolution.common.minecraft.MinecraftRenderHandle;
import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.impl.buffer.StructuredUniformBuffer;
import io.homo.superresolution.core.graphics.impl.texture.*;
import io.homo.superresolution.core.graphics.opengl.framebuffer.GlFrameBuffer;
import io.homo.superresolution.core.graphics.opengl.shader.GlShaderProgram;
import io.homo.superresolution.core.graphics.opengl.texture.GlTexture2D;
import io.homo.superresolution.core.graphics.impl.framebuffer.FrameBufferAttachmentType;

public class BlurRenderer {
    public static final GlShaderProgram BLUR;
    public static final GlFrameBuffer blurFrameBuffer;
    public static final GlTexture2D blurTempTexture;
    public static final int MAX_LEVELS = 8;
    private static final float[] blurWeights = new float[MAX_LEVELS];
    protected static StructuredUniformBuffer uniformBlock;

    static {

        BLUR = null;/*GlGeneralShaderProgram.create()
                .addShaderSource(new ShaderSource(ShaderType.FRAGMENT, "/shader/gui_blur/blur.frag.glsl", true))
                .addShaderSource(new ShaderSource(ShaderType.VERTEX, "/shader/gui_blur/blur.vert.glsl", true))
                .setShaderName("gui-blur")
                .build();*/
        blurFrameBuffer = GlFrameBuffer.create(
                TextureFormat.RGBA8,
                null,
                MinecraftRenderHandle.getScreenWidth(),
                MinecraftRenderHandle.getScreenHeight()
        );
        blurTempTexture = (GlTexture2D) RenderSystems.current().device().createTexture(
                TextureDescription
                        .create()
                        .type(TextureType.Texture2D)
                        .width(MinecraftRenderHandle.getScreenWidth())
                        .height(MinecraftRenderHandle.getScreenHeight())
                        .format(TextureFormat.RGBA8)
                        .usages(TextureUsages.create().sampler().attachmentColor())
                        .mipmapsAuto()
                        .build()
        );
    }

    public static void compileShader() {
        if (!BLUR.isCompiled()) BLUR.compile();
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
        ITexture src = MinecraftRenderHandle.getOriginRenderTarget().getTexture(FrameBufferAttachmentType.Color);
        dst.copyFromTex((int) src.handle());
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
        /*
        compileShader();
        GlShaderProgram blurShader = BLUR;
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
            Gl.glUseProgram(BLUR.handle);
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
                //blurShader.uniforms()
                //        .safeTexture("uTexture").value(blurTempTexture)
                //        .safeVec4("weightA").value(blurWeights[0], blurWeights[1], blurWeights[2], blurWeights[3])
                //        .safeVec4("weightB").value(blurWeights[4], blurWeights[5], blurWeights[6], blurWeights[7]);
                glDrawArrays(GL_TRIANGLE_FAN, 0, 4);
            }
            //blurShader.clear();
            glDepthMask(true);
        }
        */

    }
}
