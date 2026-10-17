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
import io.homo.irisapi.mixin.composite.CompositeRendererAccessor;
import io.homo.irisapi.mixin.composite.IrisRenderingPipelineAccessor;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.irisshaders.iris.gl.image.GlImage;
import net.irisshaders.iris.gl.texture.TextureAccess;
import net.irisshaders.iris.pathways.CenterDepthSampler;
import net.irisshaders.iris.pipeline.CompositeRenderer;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.shaderpack.texture.TextureStage;
import net.irisshaders.iris.targets.RenderTargets;
import net.irisshaders.iris.uniforms.custom.CustomUniforms;

import java.util.Set;

public class CompositeRendererAccessorImpl_After1201 implements ICompositeRendererAccessor{
    private final CompositeRenderer compositeRenderer;

    public CompositeRendererAccessorImpl_After1201(CompositeRenderer compositeRenderer) {
        this.compositeRenderer = compositeRenderer;
    }

    @Override
    public boolean isSameInstance(ICompositeRendererAccessor instance) {
        if (!(instance instanceof CompositeRendererAccessorImpl_After1201)) return false;
        if (((CompositeRendererAccessorImpl_After1201)instance).compositeRenderer.equals(this.compositeRenderer))return true;
        return false;
    }

    @Override
    public IrisCompositeRenderingPhase getPhase() {
        if (((IrisRenderingPipelineAccessor) getPipeline()).sr_irisapi$getCompositeRenderer() == compositeRenderer) {
            return IrisCompositeRenderingPhase.Composite;
        } else if (((IrisRenderingPipelineAccessor) getPipeline()).sr_irisapi$getDeferredRenderer() == compositeRenderer) {
            return IrisCompositeRenderingPhase.Deferred;
        } else if (((IrisRenderingPipelineAccessor) getPipeline()).sr_irisapi$getPrepareRenderer() == compositeRenderer) {
            return IrisCompositeRenderingPhase.Prepare;
        } else if (((IrisRenderingPipelineAccessor) getPipeline()).sr_irisapi$getBeginRenderer() == compositeRenderer) {
            return IrisCompositeRenderingPhase.Begin;
        }
        return IrisCompositeRenderingPhase.Unknown;
    }

    @Override
    public RenderTargets getRenderTargets() {
        return ((CompositeRendererAccessor)compositeRenderer).sr_irisapi$getRenderTargets();
    }

    @Override
    public void setRenderTargets(RenderTargets renderTargets) {
        ((CompositeRendererAccessor)compositeRenderer).sr_irisapi$setRenderTargets(renderTargets);
    }

    @Override
    public ImmutableList<NamedCompositePass> getPasses() {
        return (ImmutableList<NamedCompositePass>)((Object)ImmutableList.copyOf(((CompositeRendererAccessor)compositeRenderer).sr_irisapi$getPasses()));
    }

    @Override
    public void setPasses(ImmutableList<NamedCompositePass> passes) {
        throw new UnsupportedOperationException();
    }

    @Override
    public WorldRenderingPipeline getPipeline() {
        return ((CompositeRendererAccessor)compositeRenderer).sr_irisapi$getPipeline();
    }

    @Override
    public void setPipeline(WorldRenderingPipeline pipeline) {
        ((CompositeRendererAccessor)compositeRenderer).sr_irisapi$setPipeline(pipeline);
    }

    @Override
    public TextureAccess getNoiseTexture() {
        return ((CompositeRendererAccessor)compositeRenderer).sr_irisapi$getNoiseTexture();
    }

    @Override
    public void setNoiseTexture(TextureAccess noiseTexture) {
        ((CompositeRendererAccessor)compositeRenderer).sr_irisapi$setNoiseTexture(noiseTexture);
    }

    @Override
    public CenterDepthSampler getCenterDepthSampler() {
        return ((CompositeRendererAccessor)compositeRenderer).sr_irisapi$getCenterDepthSampler();
    }

    @Override
    public void setCenterDepthSampler(CenterDepthSampler centerDepthSampler) {
        ((CompositeRendererAccessor)compositeRenderer).sr_irisapi$setCenterDepthSampler(centerDepthSampler);
    }

    @Override
    public Object2ObjectMap<String, TextureAccess> getCustomTextureIds() {
        return ((CompositeRendererAccessor)compositeRenderer).sr_irisapi$getCustomTextureIds();
    }

    @Override
    public void setCustomTextureIds(Object2ObjectMap<String, TextureAccess> customTextureIds) {
        ((CompositeRendererAccessor)compositeRenderer).sr_irisapi$setCustomTextureIds(customTextureIds);
    }

    @Override
    public ImmutableSet<Integer> getFlippedAtLeastOnceFinal() {
        return ((CompositeRendererAccessor)compositeRenderer).sr_irisapi$getFlippedAtLeastOnceFinal();
    }

    @Override
    public void setFlippedAtLeastOnceFinal(ImmutableSet<Integer> flippedAtLeastOnceFinal) {
        ((CompositeRendererAccessor)compositeRenderer).sr_irisapi$setFlippedAtLeastOnceFinal(flippedAtLeastOnceFinal);
    }

    @Override
    public CustomUniforms getCustomUniforms() {
        return ((CompositeRendererAccessor)compositeRenderer).sr_irisapi$getCustomUniforms();
    }

    @Override
    public void setCustomUniforms(CustomUniforms customUniforms) {
        ((CompositeRendererAccessor)compositeRenderer).sr_irisapi$setCustomUniforms(customUniforms);
    }

    @Override
    public Object2ObjectMap<String, TextureAccess> getIrisCustomTextures() {
        return ((CompositeRendererAccessor)compositeRenderer).sr_irisapi$getIrisCustomTextures();
    }

    @Override
    public void setIrisCustomTextures(Object2ObjectMap<String, TextureAccess> irisCustomTextures) {
        ((CompositeRendererAccessor)compositeRenderer).sr_irisapi$setIrisCustomTextures(irisCustomTextures);
    }

    @Override
    public Set<GlImage> getCustomImages() {
        return ((CompositeRendererAccessor)compositeRenderer).sr_irisapi$getCustomImages();
    }

    @Override
    public void setCustomImages(Set<GlImage> customImages) {
        ((CompositeRendererAccessor)compositeRenderer).sr_irisapi$setCustomImages(customImages);
    }

    @Override
    public TextureStage getTextureStage() {
        return ((CompositeRendererAccessor)compositeRenderer).sr_irisapi$getTextureStage();
    }

    @Override
    public void setTextureStage(TextureStage textureStage) {
        ((CompositeRendererAccessor)compositeRenderer).sr_irisapi$setTextureStage(textureStage);
    }
}
