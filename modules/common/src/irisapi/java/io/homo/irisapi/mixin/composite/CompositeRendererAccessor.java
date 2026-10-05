/*
 * Super Resolution
 * Copyright (c) 2025-2026. 187J3X1-114514
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

package io.homo.irisapi.mixin.composite;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.irisshaders.iris.gl.image.GlImage;
import net.irisshaders.iris.gl.texture.TextureAccess;
import net.irisshaders.iris.pathways.CenterDepthSampler;
import net.irisshaders.iris.pipeline.CompositeRenderer;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.shaderpack.texture.TextureStage;
import net.irisshaders.iris.targets.RenderTargets;
import net.irisshaders.iris.uniforms.FrameUpdateNotifier;
import net.irisshaders.iris.uniforms.custom.CustomUniforms;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;

@Mixin(CompositeRenderer.class)
public interface CompositeRendererAccessor {

    @Accessor(value = "renderTargets", remap = false)
    RenderTargets getRenderTargets();
    @Accessor(value = "renderTargets", remap = false)
    void setRenderTargets(RenderTargets renderTargets);
    @Accessor(value = "passes", remap = false)
    ImmutableList<Object> getPasses();

    @Accessor(value = "passes", remap = false)
    void setPasses(ImmutableList<Object> passes);

    @Accessor(value = "pipeline", remap = false)
    WorldRenderingPipeline getPipeline();

    @Accessor(value = "pipeline", remap = false)
    void setPipeline(WorldRenderingPipeline pipeline);

    @Accessor(value = "noiseTexture", remap = false)
    TextureAccess getNoiseTexture();

    @Accessor(value = "noiseTexture", remap = false)
    void setNoiseTexture(TextureAccess noiseTexture);

    @Accessor(value = "centerDepthSampler", remap = false)
    CenterDepthSampler getCenterDepthSampler();

    @Accessor(value = "centerDepthSampler", remap = false)
    void setCenterDepthSampler(CenterDepthSampler centerDepthSampler);

    @Accessor(value = "customTextureIds", remap = false)
    Object2ObjectMap<String, TextureAccess> getCustomTextureIds();

    @Accessor(value = "customTextureIds", remap = false)
    void setCustomTextureIds(Object2ObjectMap<String, TextureAccess> customTextureIds);

    @Accessor(value = "flippedAtLeastOnceFinal", remap = false)
    ImmutableSet<Integer> getFlippedAtLeastOnceFinal();

    @Accessor(value = "flippedAtLeastOnceFinal", remap = false)
    void setFlippedAtLeastOnceFinal(ImmutableSet<Integer> flippedAtLeastOnceFinal);

    @Accessor(value = "customUniforms", remap = false)
    CustomUniforms getCustomUniforms();

    @Accessor(value = "customUniforms", remap = false)
    void setCustomUniforms(CustomUniforms customUniforms);

    @Accessor(value = "irisCustomTextures", remap = false)
    Object2ObjectMap<String, TextureAccess> getIrisCustomTextures();

    @Accessor(value = "irisCustomTextures", remap = false)
    void setIrisCustomTextures(Object2ObjectMap<String, TextureAccess> irisCustomTextures);

    @Accessor(value = "customImages", remap = false)
    Set<GlImage> getCustomImages();

    @Accessor(value = "customImages", remap = false)
    void setCustomImages(Set<GlImage> customImages);

    @Accessor(value = "textureStage", remap = false)
    TextureStage getTextureStage();

    @Accessor(value = "textureStage", remap = false)
    void setTextureStage(TextureStage textureStage);
}