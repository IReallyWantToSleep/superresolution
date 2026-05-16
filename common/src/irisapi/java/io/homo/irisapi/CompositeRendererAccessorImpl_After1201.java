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
import io.homo.irisapi.pipeline.mc1201.NewCompositeRenderer;
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
        if (((IrisRenderingPipelineAccessor) getPipeline()).getCompositeRenderer() == compositeRenderer) {
            return IrisCompositeRenderingPhase.Composite;
        } else if (((IrisRenderingPipelineAccessor) getPipeline()).getDeferredRenderer() == compositeRenderer) {
            return IrisCompositeRenderingPhase.Deferred;
        } else if (((IrisRenderingPipelineAccessor) getPipeline()).getPrepareRenderer() == compositeRenderer) {
            return IrisCompositeRenderingPhase.Prepare;
        } else if (((IrisRenderingPipelineAccessor) getPipeline()).getBeginRenderer() == compositeRenderer) {
            return IrisCompositeRenderingPhase.Begin;
        }
        return IrisCompositeRenderingPhase.Unknown;
    }

    @Override
    public RenderTargets getRenderTargets() {
        return ((CompositeRendererAccessor)compositeRenderer).getRenderTargets();
    }

    @Override
    public void setRenderTargets(RenderTargets renderTargets) {
        ((CompositeRendererAccessor)compositeRenderer).setRenderTargets(renderTargets);
    }

    @Override
    public ImmutableList<NamedCompositePass> getPasses() {
        return (ImmutableList<NamedCompositePass>)((Object)ImmutableList.copyOf(((CompositeRendererAccessor)compositeRenderer).getPasses()));
    }

    @Override
    public void setPasses(ImmutableList<NamedCompositePass> passes) {
        throw new UnsupportedOperationException();
    }

    @Override
    public WorldRenderingPipeline getPipeline() {
        return ((CompositeRendererAccessor)compositeRenderer).getPipeline();
    }

    @Override
    public void setPipeline(WorldRenderingPipeline pipeline) {
        ((CompositeRendererAccessor)compositeRenderer).setPipeline(pipeline);
    }

    @Override
    public TextureAccess getNoiseTexture() {
        return ((CompositeRendererAccessor)compositeRenderer).getNoiseTexture();
    }

    @Override
    public void setNoiseTexture(TextureAccess noiseTexture) {
        ((CompositeRendererAccessor)compositeRenderer).setNoiseTexture(noiseTexture);
    }

    @Override
    public CenterDepthSampler getCenterDepthSampler() {
        return ((CompositeRendererAccessor)compositeRenderer).getCenterDepthSampler();
    }

    @Override
    public void setCenterDepthSampler(CenterDepthSampler centerDepthSampler) {
        ((CompositeRendererAccessor)compositeRenderer).setCenterDepthSampler(centerDepthSampler);
    }

    @Override
    public Object2ObjectMap<String, TextureAccess> getCustomTextureIds() {
        return ((CompositeRendererAccessor)compositeRenderer).getCustomTextureIds();
    }

    @Override
    public void setCustomTextureIds(Object2ObjectMap<String, TextureAccess> customTextureIds) {
        ((CompositeRendererAccessor)compositeRenderer).setCustomTextureIds(customTextureIds);
    }

    @Override
    public ImmutableSet<Integer> getFlippedAtLeastOnceFinal() {
        return ((CompositeRendererAccessor)compositeRenderer).getFlippedAtLeastOnceFinal();
    }

    @Override
    public void setFlippedAtLeastOnceFinal(ImmutableSet<Integer> flippedAtLeastOnceFinal) {
        ((CompositeRendererAccessor)compositeRenderer).setFlippedAtLeastOnceFinal(flippedAtLeastOnceFinal);
    }

    @Override
    public CustomUniforms getCustomUniforms() {
        return ((CompositeRendererAccessor)compositeRenderer).getCustomUniforms();
    }

    @Override
    public void setCustomUniforms(CustomUniforms customUniforms) {
        ((CompositeRendererAccessor)compositeRenderer).setCustomUniforms(customUniforms);
    }

    @Override
    public Object2ObjectMap<String, TextureAccess> getIrisCustomTextures() {
        return ((CompositeRendererAccessor)compositeRenderer).getIrisCustomTextures();
    }

    @Override
    public void setIrisCustomTextures(Object2ObjectMap<String, TextureAccess> irisCustomTextures) {
        ((CompositeRendererAccessor)compositeRenderer).setIrisCustomTextures(irisCustomTextures);
    }

    @Override
    public Set<GlImage> getCustomImages() {
        return ((CompositeRendererAccessor)compositeRenderer).getCustomImages();
    }

    @Override
    public void setCustomImages(Set<GlImage> customImages) {
        ((CompositeRendererAccessor)compositeRenderer).setCustomImages(customImages);
    }

    @Override
    public TextureStage getTextureStage() {
        return ((CompositeRendererAccessor)compositeRenderer).getTextureStage();
    }

    @Override
    public void setTextureStage(TextureStage textureStage) {
        ((CompositeRendererAccessor)compositeRenderer).setTextureStage(textureStage);
    }
}
