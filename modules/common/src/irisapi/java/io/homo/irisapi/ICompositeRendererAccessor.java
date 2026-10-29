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

package io.homo.irisapi;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.irisshaders.iris.gl.image.GlImage;
import net.irisshaders.iris.gl.texture.TextureAccess;
import net.irisshaders.iris.pathways.CenterDepthSampler;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.shaderpack.texture.TextureStage;
import net.irisshaders.iris.targets.RenderTargets;
import net.irisshaders.iris.uniforms.custom.CustomUniforms;

import java.util.Set;

public interface ICompositeRendererAccessor {
    boolean isSameInstance(ICompositeRendererAccessor instance);
    IrisCompositeRenderingPhase getPhase();
    RenderTargets getRenderTargets();
    void setRenderTargets(RenderTargets renderTargets);
    ImmutableList<NamedCompositePass> getPasses();
    void setPasses(ImmutableList<NamedCompositePass> passes);
    WorldRenderingPipeline getPipeline();
    void setPipeline(WorldRenderingPipeline pipeline);
    TextureAccess getNoiseTexture();
    void setNoiseTexture(TextureAccess noiseTexture);

    CenterDepthSampler getCenterDepthSampler();
    void setCenterDepthSampler(CenterDepthSampler centerDepthSampler);
    Object2ObjectMap<String, TextureAccess> getCustomTextureIds();
    void setCustomTextureIds(Object2ObjectMap<String, TextureAccess> customTextureIds);
    ImmutableSet<Integer> getFlippedAtLeastOnceFinal();
    void setFlippedAtLeastOnceFinal(ImmutableSet<Integer> flippedAtLeastOnceFinal);
    CustomUniforms getCustomUniforms();
    void setCustomUniforms(CustomUniforms customUniforms);
    Object2ObjectMap<String, TextureAccess> getIrisCustomTextures();
    void setIrisCustomTextures(Object2ObjectMap<String, TextureAccess> irisCustomTextures);
    Set<GlImage> getCustomImages();
    void setCustomImages(Set<GlImage> customImages);
    TextureStage getTextureStage();
    void setTextureStage(TextureStage textureStage);
}
