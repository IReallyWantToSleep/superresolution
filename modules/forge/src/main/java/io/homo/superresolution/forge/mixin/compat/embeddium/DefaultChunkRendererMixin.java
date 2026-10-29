/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
