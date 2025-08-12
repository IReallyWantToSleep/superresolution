package io.homo.superresolution.core.graphics.opengl.utils;

import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.impl.CopyOperation;
import io.homo.superresolution.core.graphics.impl.DrawObject;
import io.homo.superresolution.core.graphics.impl.framebuffer.FrameBufferAttachmentType;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.core.graphics.impl.shader.ShaderDescription;
import io.homo.superresolution.core.graphics.impl.shader.ShaderSource;
import io.homo.superresolution.core.graphics.impl.shader.ShaderType;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.opengl.GlDebug;
import io.homo.superresolution.core.graphics.opengl.framebuffer.GlFrameBuffer;
import io.homo.superresolution.core.graphics.opengl.shader.GlShaderProgram;
import io.homo.superresolution.core.graphics.system.IRenderState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GlTextureCopier {
    private static final Map<String, GlShaderProgram> programMap = new HashMap<>();

    private static GlShaderProgram getOrCreateProgram(CopyOperation copyOperation) {
        String key = mappingKey(copyOperation.getMappings());
        if (programMap.containsKey(key))
            return programMap.get(key);
        ShaderDescription.Builder builder =
                ShaderDescription.graphics(
                        new ShaderSource(ShaderType.FRAGMENT, "/shader/copy.frag.glsl", true),
                        new ShaderSource(ShaderType.VERTEX, "/shader/copy.vert.glsl", true)
                );

        builder.addDefine("COPY_CHANCEL", String.valueOf(copyOperation.getMappings().size()));
        for (int i = 0; i < copyOperation.getMappings().size(); i++) {
            CopyOperation.ChannelMapping map = copyOperation.getMappings().get(i);
            builder.addDefine("COPY_SRC_CHANCEL" + i, String.valueOf(map.src.ordinal()));
            builder.addDefine("COPY_DST_CHANCEL" + i, String.valueOf(map.dst.ordinal()));
        }

        builder.uniformSamplerTexture("tex", 0);

        GlShaderProgram program = RenderSystems.opengl().device().createShaderProgram(builder.build());
        program.compile();
        programMap.put(key, program);
        return program;
    }

    private static String mappingKey(List<CopyOperation.ChannelMapping> mappings) {
        return mappings.stream()
                .map(m -> m.src.ordinal() + "->" + m.dst.ordinal())
                .collect(Collectors.joining(","));
    }


    public static void copy(CopyOperation copyOperation) {
        GlDebug.pushGroup(GlDebug.nextCopyId(), "CopyTexture");
        GlShaderProgram program = getOrCreateProgram(copyOperation);
        IRenderState.StateSnapshot stateSnapshot = RenderSystems.opengl().device().commendEncoder().renderState().get();
        GlFrameBuffer dstFbo = GlFrameBuffer.create(copyOperation.getDstTexture(), null);

        RenderSystems.opengl().device().commendEncoder()
                .begin()
                .renderState()
                .colorMask(true, true, true, true)
                .depthTest(false)
                .depthWrite(false)
                .cullFace(false)
                .viewport(0, 0, copyOperation.getDstTexture().getWidth(), copyOperation.getDstTexture().getHeight());

        program.uniforms().samplerTexture("tex").set(copyOperation.getSrcTexture());

        RenderSystems.opengl().device().commendEncoder()
                .draw(
                        program,
                        dstFbo,
                        DrawObject.fullscreenQuad(RenderSystems.opengl().device()).once(),
                        0,
                        DrawObject.fullscreenQuadVertexCount()
                );
        RenderSystems.opengl().device().commendEncoder().renderState().apply(stateSnapshot);
        RenderSystems.opengl().device().submitCommandBuffer(RenderSystems.opengl().device().commendEncoder().end());
        dstFbo.destroy();
        GlDebug.popGroup();
    }
}