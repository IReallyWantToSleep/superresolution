package io.homo.superresolution.forge.mixin.compat.embeddium;

import me.jellysquid.mods.sodium.client.render.chunk.DefaultChunkRenderer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = DefaultChunkRenderer.class, remap = false)
public abstract class DefaultChunkRendererMixin
        //implements ShaderChunkRendererExt
{/*
    @Unique
    private final Pair<Matrix4f, Matrix4f> lastMatrix = Pair.of(
            new Matrix4f().identity(),
            new Matrix4f().identity()
    );
    @Unique
    protected GlProgram<ChunkShaderInterface> activeProgram;

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lme/jellysquid/mods/sodium/client/render/chunk/shader/ChunkShaderInterface;setModelViewMatrix(Lorg/joml/Matrix4fc;)V"))
    public void setupUniform(ChunkRenderMatrices matrices, CommandList commandList, ChunkRenderListIterable renderLists, TerrainRenderPass renderPass, CameraTransform camera, CallbackInfo ci) {

        setMatrix4("u_lastProjectionMatrix", lastMatrix.first);
        setMatrix4("u_lastModelViewMatrix", lastMatrix.second);
        lastMatrix = Pair.of(
                new Matrix4f(matrices.projection()),
                new Matrix4f(matrices.modelView())
        );
    }


    @Unique
    private int getUniformLocation(String name) {
        return glGetUniformLocation(this.iris$getOverride().handle(), name);
    }

    @Unique
    public void setMatrix4(String name, Matrix4f x) {
        float[] data = new float[16];
        x.get(data);
        glUniformMatrix4fv(getUniformLocation(name), false, data);
    }*/
}
