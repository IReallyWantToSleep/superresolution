package io.homo.superresolution.shadercompat;

import com.google.common.collect.ImmutableMap;
import io.homo.superresolution.shadercompat.mixin.core.CompositeRendererAccessor;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.irisshaders.iris.gl.buffer.ShaderStorageBufferHolder;
import net.irisshaders.iris.gl.image.GlImage;
import net.irisshaders.iris.gl.texture.TextureAccess;
import net.irisshaders.iris.pathways.CenterDepthSampler;
import net.irisshaders.iris.pipeline.CompositePass;
import net.irisshaders.iris.pipeline.CompositeRenderer;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.shaderpack.programs.ComputeSource;
import net.irisshaders.iris.shaderpack.programs.ProgramSource;
import net.irisshaders.iris.shaderpack.properties.PackDirectives;
import net.irisshaders.iris.shaderpack.texture.TextureStage;
import net.irisshaders.iris.shadows.ShadowRenderTargets;
import net.irisshaders.iris.targets.BufferFlipper;
import net.irisshaders.iris.targets.RenderTargets;
import net.irisshaders.iris.uniforms.FrameUpdateNotifier;
import net.irisshaders.iris.uniforms.custom.CustomUniforms;

import java.util.Set;
import java.util.function.Supplier;

public class SRCompositeRenderer extends CompositeRenderer {
    public SRCompositeRenderer(WorldRenderingPipeline pipeline, CompositePass compositePass, PackDirectives packDirectives, ProgramSource[] sources, ComputeSource[][] computes, RenderTargets renderTargets, ShaderStorageBufferHolder holder, TextureAccess noiseTexture, FrameUpdateNotifier updateNotifier, CenterDepthSampler centerDepthSampler, BufferFlipper bufferFlipper, Supplier<ShadowRenderTargets> shadowTargetsSupplier, TextureStage textureStage, Object2ObjectMap<String, TextureAccess> customTextureIds, Object2ObjectMap<String, TextureAccess> irisCustomTextures, Set<GlImage> customImages, ImmutableMap<Integer, Boolean> explicitPreFlips, CustomUniforms customUniforms) {
        super(pipeline, compositePass, packDirectives, sources, computes, renderTargets, holder, noiseTexture, updateNotifier, centerDepthSampler, bufferFlipper, shadowTargetsSupplier, textureStage, customTextureIds, irisCustomTextures, customImages, explicitPreFlips, customUniforms);
    }

    public RenderTargets getRenderTargets() {
        return ((CompositeRendererAccessor) this).getRenderTargets();
    }
    
}
