package io.homo.superresolution.common.gui.effect;

import io.homo.superresolution.common.debug.imgui.ImguiMain;
import io.homo.superresolution.common.platform.Platform;
import io.homo.superresolution.common.minecraft.MinecraftRenderHandle;
import io.homo.superresolution.core.gl.GlState;
import io.homo.superresolution.core.gl.framebuffer.GlFrameBuffer;
import io.homo.superresolution.core.gl.shader.GlGeneralShaderProgram;
import io.homo.superresolution.core.gl.texture.GlTexture;
import io.homo.superresolution.core.gl.vertex.VertexArray;
import io.homo.superresolution.core.gl.vertex.VertexBuffer;
import io.homo.superresolution.core.impl.framebuffer.FrameBufferAttachmentType;
import io.homo.superresolution.core.impl.texture.ITexture;
import io.homo.superresolution.core.impl.texture.TextureFormat;
import io.homo.superresolution.core.impl.framebuffer.FrameBufferBindPoint;
import io.homo.superresolution.core.utils.FileReadHelper;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.glDepthMask;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;

public class BlurRenderer {
    public static final GlGeneralShaderProgram BLUR;
    public static final GlFrameBuffer blurFrameBuffer;
    public static final GlTexture blurTempTexture;
    public static final int MAX_LEVELS = 8;
    private static final float[] blurWeights = new float[MAX_LEVELS];

    static {
        BLUR = GlGeneralShaderProgram.create()
                .addAllFragShaderTextList(FileReadHelper.readText("/shader/gui_blur/blur.frag.glsl"))
                .addAllVertShaderTextList(FileReadHelper.readText("/shader/gui_blur/blur.vert.glsl"))
                .setShaderName("gui-blur")
                .build();
        blurFrameBuffer = GlFrameBuffer.create(
                TextureFormat.RGBA8,
                null,
                MinecraftRenderHandle.getScreenWidth(),
                MinecraftRenderHandle.getScreenHeight()
        );
        blurTempTexture = GlTexture.create(
                MinecraftRenderHandle.getScreenWidth(),
                MinecraftRenderHandle.getScreenHeight(),
                TextureFormat.RGBA8,
                true
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
        GlTexture dst = blurTempTexture;
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
            try (VertexArray vao = new VertexArray();
                 VertexBuffer vbo = new VertexBuffer()) {
                float[] vertices = {
                        -1f, -1f, 0f, 0f,
                        1f, -1f, 1f, 0f,
                        1f, 1f, 1f, 1f,
                        -1f, 1f, 0f, 1f
                };
                vao.bind();
                vbo.bind(GL_ARRAY_BUFFER);
                vbo.uploadData(vertices, GL_STATIC_DRAW);
                int stride = 4 * Float.BYTES;
                glEnableVertexAttribArray(0);
                glVertexAttribPointer(0, 2, GL_FLOAT, false, stride, 0);
                glEnableVertexAttribArray(1);
                glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 2 * Float.BYTES);
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
