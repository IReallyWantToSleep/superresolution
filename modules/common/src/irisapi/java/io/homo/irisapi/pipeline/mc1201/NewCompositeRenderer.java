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

package io.homo.irisapi.pipeline.mc1201;

#if MC_VER < MC_1_21_1
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import io.homo.irisapi.*;
import io.homo.irisapi.handlers.IrisRenderingPipelineHandler;
import io.homo.irisapi.mixin.composite.before1_21_1.GlStateManagerTextureStateAccessor;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.irisshaders.iris.features.FeatureFlags;
import net.irisshaders.iris.gl.GLDebug;
import net.irisshaders.iris.gl.IrisRenderSystem;
import net.irisshaders.iris.gl.blending.BlendModeOverride;
import net.irisshaders.iris.gl.buffer.ShaderStorageBufferHolder;
import net.irisshaders.iris.gl.framebuffer.GlFramebuffer;
import net.irisshaders.iris.gl.framebuffer.ViewportData;
import net.irisshaders.iris.gl.image.GlImage;
import net.irisshaders.iris.gl.program.ComputeProgram;
import net.irisshaders.iris.gl.program.Program;
import net.irisshaders.iris.gl.program.ProgramBuilder;
import net.irisshaders.iris.gl.program.ProgramSamplers;
import net.irisshaders.iris.gl.program.ProgramUniforms;
import net.irisshaders.iris.gl.sampler.SamplerLimits;
import net.irisshaders.iris.gl.shader.ShaderCompileException;
import net.irisshaders.iris.gl.state.FogMode;
import net.irisshaders.iris.gl.texture.TextureAccess;
import net.irisshaders.iris.mixin.GlStateManagerAccessor;
import net.irisshaders.iris.pathways.CenterDepthSampler;
import net.irisshaders.iris.pathways.FullScreenQuadRenderer;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.pipeline.transform.PatchShaderType;
import net.irisshaders.iris.pipeline.transform.ShaderPrinter;
import net.irisshaders.iris.pipeline.transform.TransformPatcher;
import net.irisshaders.iris.samplers.IrisImages;
import net.irisshaders.iris.samplers.IrisSamplers;
import net.irisshaders.iris.shaderpack.FilledIndirectPointer;
import net.irisshaders.iris.shaderpack.programs.ComputeSource;
import net.irisshaders.iris.shaderpack.programs.ProgramSource;
import net.irisshaders.iris.shaderpack.properties.PackDirectives;
import net.irisshaders.iris.shaderpack.properties.PackRenderTargetDirectives;
import net.irisshaders.iris.shaderpack.properties.ProgramDirectives;
import net.irisshaders.iris.shaderpack.texture.TextureStage;
import net.irisshaders.iris.shadows.ShadowRenderTargets;
import net.irisshaders.iris.targets.BufferFlipper;
import net.irisshaders.iris.targets.RenderTarget;
import net.irisshaders.iris.targets.RenderTargets;
import net.irisshaders.iris.uniforms.CommonUniforms;
import net.irisshaders.iris.uniforms.FrameUpdateNotifier;
import net.irisshaders.iris.uniforms.custom.CustomUniforms;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.opengl.GL43C;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
#endif

public class NewCompositeRenderer {
    #if MC_VER < MC_1_21_1
    private RenderTargets renderTargets;
    private ImmutableList<Pass> passes;
    private TextureAccess noiseTexture;
    private CenterDepthSampler centerDepthSampler;
    private Object2ObjectMap<String, TextureAccess> customTextureIds;
    private ImmutableSet<Integer> flippedAtLeastOnceFinal;
    private CustomUniforms customUniforms;
    private Object2ObjectMap<String, TextureAccess> irisCustomTextures;
    private Set<GlImage> customImages;
    private TextureStage textureStage;
    private WorldRenderingPipeline pipeline;
    private CompositePass compositePass;

    public NewCompositeRenderer(WorldRenderingPipeline pipeline, CompositePass compositePass, PackDirectives packDirectives, ProgramSource[] sources, ComputeSource[][] computes, RenderTargets renderTargets, ShaderStorageBufferHolder holder,
                                TextureAccess noiseTexture, FrameUpdateNotifier updateNotifier,
                                CenterDepthSampler centerDepthSampler, BufferFlipper bufferFlipper,
                                Supplier<ShadowRenderTargets> shadowTargetsSupplier, TextureStage textureStage,
                                Object2ObjectMap<String, TextureAccess> customTextureIds, Object2ObjectMap<String, TextureAccess> irisCustomTextures, Set<GlImage> customImages, ImmutableMap<Integer, Boolean> explicitPreFlips,
                                CustomUniforms customUniforms) {
        this.pipeline = pipeline;
        this.compositePass = compositePass;
        this.noiseTexture = noiseTexture;
        this.centerDepthSampler = centerDepthSampler;
        this.renderTargets = renderTargets;
        this.customTextureIds = customTextureIds;
        this.customUniforms = customUniforms;
        this.irisCustomTextures = irisCustomTextures;
        this.customImages = customImages;
        this.textureStage = textureStage;

        final PackRenderTargetDirectives renderTargetDirectives = packDirectives.getRenderTargetDirectives();
        final Map<Integer, PackRenderTargetDirectives.RenderTargetSettings> renderTargetSettings =
                renderTargetDirectives.getRenderTargetSettings();

        final ImmutableList.Builder<Pass> passes = ImmutableList.builder();
        final ImmutableSet.Builder<Integer> flippedAtLeastOnce = new ImmutableSet.Builder<>();

        explicitPreFlips.forEach((buffer, shouldFlip) -> {
            if (shouldFlip) {
                bufferFlipper.flip(buffer);
            }
        });

        for (int i = 0; i < sources.length; i++) {
            ProgramSource source = sources[i];

            ImmutableSet<Integer> flipped = bufferFlipper.snapshot();
            ImmutableSet<Integer> flippedAtLeastOnceSnapshot = flippedAtLeastOnce.build();

            if (source == null || !source.isValid()) {
                if (computes.length != 0 && computes[i] != null && computes[i].length > 0) {
                    ComputeOnlyPass pass = new ComputeOnlyPass();
                    pass.name = computes[i].length > 0 ? Arrays.stream(computes[i]).filter(Objects::nonNull).findFirst().map(ComputeSource::getName).orElse("unknown") : "unknown";
                    pass.computes = createComputes(computes[i], flipped, flippedAtLeastOnceSnapshot, shadowTargetsSupplier, holder);
                    passes.add(pass);
                }
                continue;
            }

            Pass pass = new Pass();
            ProgramDirectives directives = source.getDirectives();

            pass.name = source.getName();
            pass.program = createProgram(source, flipped, flippedAtLeastOnceSnapshot, shadowTargetsSupplier);
            pass.blendModeOverride = source.getDirectives().getBlendModeOverride().orElse(null);
            if (computes.length != 0) {
                pass.computes = createComputes(computes[i], flipped, flippedAtLeastOnceSnapshot, shadowTargetsSupplier, holder);
            } else {
                pass.computes = new ComputeProgram[0];
            }
            int[] drawBuffers = directives.getDrawBuffers();

            int passWidth = 0, passHeight = 0;
            ImmutableMap<Integer, Boolean> explicitFlips = directives.getExplicitFlips();

            GlFramebuffer framebuffer = renderTargets.createColorFramebuffer(flipped, drawBuffers);

            for (int buffer : drawBuffers) {
                RenderTarget target = renderTargets.get(buffer);
                if ((passWidth > 0 && passWidth != target.getWidth()) || (passHeight > 0 && passHeight != target.getHeight())) {
                    throw new IllegalStateException("Pass sizes must match for drawbuffers " + Arrays.toString(drawBuffers) + "\nOriginal width: " + passWidth + " New width: " + target.getWidth() + " Original height: " + passHeight + " New height: " + target.getHeight());
                }
                passWidth = target.getWidth();
                passHeight = target.getHeight();

                if (explicitFlips.get(buffer) == Boolean.FALSE) {
                    continue;
                }

                bufferFlipper.flip(buffer);
                flippedAtLeastOnce.add(buffer);
            }

            explicitFlips.forEach((buffer, shouldFlip) -> {
                if (shouldFlip) {
                    bufferFlipper.flip(buffer);
                    flippedAtLeastOnce.add(buffer);
                }
            });

            pass.drawBuffers = directives.getDrawBuffers();
            pass.viewWidth = passWidth;
            pass.viewHeight = passHeight;
            pass.stageReadsFromAlt = flipped;
            pass.framebuffer = framebuffer;
            pass.viewportScale = directives.getViewportScale();
            pass.mipmappedBuffers = directives.getMipmappedBuffers();
            pass.flippedAtLeastOnce = flippedAtLeastOnceSnapshot;

            passes.add(pass);
        }

        this.passes = passes.build();
        this.flippedAtLeastOnceFinal = flippedAtLeastOnce.build();

        GlStateManager._glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, 0);
    }


    private Pass superresolution$getPass(int passIndex) {
        if (passIndex >= 0 && passIndex < this.passes.size()) {
            return this.passes.get(passIndex);
        }
        return null;
    }

    private IrisCompositePassType getCompositePassType(Object object) {
        Class<?> objClazz = object.getClass();
        if (object instanceof ComputeOnlyPass) {
            return IrisCompositePassType.ComputeOnly;
        }

        if (object instanceof Pass) {
            Object[] computes = ((Pass)object).computes;
            if (computes != null && computes.length > 0) {
                return IrisCompositePassType.Mixed;
            } else {
                return IrisCompositePassType.Common;
            }
        }

        throw new IllegalArgumentException("Unknown pass type: " + objClazz.getName());
    }
    private void superresolution$handlePassEvent(int passIndex, PassEventHandler handler) {
        Pass pass = superresolution$getPass(passIndex);
        Objects.requireNonNull(pass);
        handler.handle(
                new CompositeRendererAccessorImpl(this), // 原 Mixin: (CompositeRenderer) (Object) this
                pass,
                getCompositePassType(pass)
        );
    }

    private static void setupMipmapping(RenderTarget target, boolean readFromAlt) {
        if (target == null) return;

        int texture = readFromAlt ? target.getAltTexture() : target.getMainTexture();
        IrisRenderSystem.generateMipmaps(texture, GL20C.GL_TEXTURE_2D);

        int filter = GL20C.GL_LINEAR_MIPMAP_LINEAR;
        if (target.getInternalFormat().getPixelFormat().isInteger()) {
            filter = GL20C.GL_NEAREST_MIPMAP_NEAREST;
        }

        IrisRenderSystem.texParameteri(texture, GL20C.GL_TEXTURE_2D, GL20C.GL_TEXTURE_MIN_FILTER, filter);
    }

    private boolean hasComputes(ComputeSource[][] computes) {
        boolean hasCompute = false;

        for (int i = 0; i < computes.length; i++) {
            if (computes[i].length > 0) {
                for (int j = 0; j < computes[i].length; j++) {
                    if (computes[i][j] != null) {
                        hasCompute = true;
                        break;
                    }
                }
            }
        }

        return hasCompute;
    }

    public ImmutableSet<Integer> getFlippedAtLeastOnceFinal() {
        return this.flippedAtLeastOnceFinal;
    }

    public void recalculateSizes() {
        for (Pass pass : passes) {
            if (pass instanceof ComputeOnlyPass) {
                continue;
            }
            int passWidth = 0, passHeight = 0;
            for (int buffer : pass.drawBuffers) {
                RenderTarget target = renderTargets.get(buffer);
                if ((passWidth > 0 && passWidth != target.getWidth()) || (passHeight > 0 && passHeight != target.getHeight())) {
                    throw new IllegalStateException("Pass widths must match");
                }
                passWidth = target.getWidth();
                passHeight = target.getHeight();
            }
            renderTargets.destroyFramebuffer(pass.framebuffer);
            pass.framebuffer = renderTargets.createColorFramebuffer(pass.stageReadsFromAlt, pass.drawBuffers);
            pass.viewWidth = passWidth;
            pass.viewHeight = passHeight;
        }
    }

    public void renderAll() {
        GLDebug.pushGroup(20 + compositePass.ordinal(), compositePass.name().toLowerCase(Locale.ROOT));
        RenderSystem.disableBlend();

        FullScreenQuadRenderer.INSTANCE.begin();
        com.mojang.blaze3d.pipeline.RenderTarget main = Minecraft.getInstance().getMainRenderTarget();

        for (int i = 0, passesSize = passes.size(); i < passesSize; i++) {
            Pass renderPass = passes.get(i);

            // =========== PassStart (mixin: onPassStart) ===========
            superresolution$handlePassEvent(i, IrisRenderingPipelineHandler::onCompositePassStart);

            GLDebug.pushGroup(20 * compositePass.ordinal() + i, renderPass.name);

            // =========== BeforeRender (mixin: onBeforeRender / onBeforeRenderA) ===========
            IrisCompositePassType passType = getCompositePassType(renderPass);
            // 对于非 Common 类型（ComputeOnly 等），在 pushGroup 后立即触发 dispatchBefore
            if (passType != IrisCompositePassType.Common) {
                // 对应 mixin: onBeforeRender
                superresolution$handlePassEvent(i, IrisRenderingPipelineHandler::onCompositePassDispatchBefore);
            }

            boolean ranCompute = false;
            for (ComputeProgram computeProgram : renderPass.computes) {
                if (computeProgram != null) {
                    ranCompute = true;
                    computeProgram.use();
                    this.customUniforms.push(computeProgram);
                    computeProgram.dispatch(main.width, main.height);
                }
            }

            if (ranCompute) {
                IrisRenderSystem.memoryBarrier(GL43C.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT | GL43C.GL_TEXTURE_FETCH_BARRIER_BIT | GL43C.GL_SHADER_STORAGE_BARRIER_BIT);
            }

            Program.unbind();

            // =========== AfterRender (mixin: onAfterRender - ComputeOnly) ===========
            if (passType == IrisCompositePassType.ComputeOnly) {
                // 对应 mixin: onAfterRender
                superresolution$handlePassEvent(i, IrisRenderingPipelineHandler::onCompositePassDispatchAfter);
            }

            if (renderPass instanceof ComputeOnlyPass) {
                GLDebug.popGroup();

                // =========== PassEnd (mixin: onPassEnd - ComputeOnly) ===========
                if (passType == IrisCompositePassType.ComputeOnly) {
                    // 对应 mixin: onPassEnd
                    superresolution$handlePassEvent(i, IrisRenderingPipelineHandler::onCompositePassEnd);
                }
                continue;
            }

            // =========== BeforeRenderA (mixin: onBeforeRenderA - Common) ===========
            if (passType == IrisCompositePassType.Common) {
                // 对应 mixin: onBeforeRenderA，在 renderQuad 之前
                superresolution$handlePassEvent(i, IrisRenderingPipelineHandler::onCompositePassDispatchBefore);
            }

            if (!renderPass.mipmappedBuffers.isEmpty()) {
                RenderSystem.activeTexture(GL15C.GL_TEXTURE0);
                for (int index : renderPass.mipmappedBuffers) {
                    setupMipmapping(this.renderTargets.get(index), renderPass.stageReadsFromAlt.contains(index));
                }
            }

            float scaledWidth = renderPass.viewWidth * renderPass.viewportScale.scale();
            float scaledHeight = renderPass.viewHeight * renderPass.viewportScale.scale();
            int beginWidth = (int) (renderPass.viewWidth * renderPass.viewportScale.viewportX());
            int beginHeight = (int) (renderPass.viewHeight * renderPass.viewportScale.viewportY());
            RenderSystem.viewport(beginWidth, beginHeight, (int) scaledWidth, (int) scaledHeight);

            renderPass.framebuffer.bind();
            renderPass.program.use();
            if (renderPass.blendModeOverride != null) {
                renderPass.blendModeOverride.apply();
            } else {
                RenderSystem.disableBlend();
            }

            this.customUniforms.push(renderPass.program);
            FullScreenQuadRenderer.INSTANCE.renderQuad();

            BlendModeOverride.restore();

            // =========== AfterRenderA (mixin: onAfterRenderA - 非 ComputeOnly) ===========
            if (passType != IrisCompositePassType.ComputeOnly) {
                // 对应 mixin: onAfterRenderA
                superresolution$handlePassEvent(i, IrisRenderingPipelineHandler::onCompositePassDispatchAfter);
            }

            GLDebug.popGroup();

            // =========== PassEndA (mixin: onPassEndA - 非 ComputeOnly) ===========
            if (passType != IrisCompositePassType.ComputeOnly) {
                // 对应 mixin: onPassEndA
                superresolution$handlePassEvent(i, IrisRenderingPipelineHandler::onCompositePassEnd);
            }
        }

        FullScreenQuadRenderer.INSTANCE.end();

        Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
        ProgramUniforms.clearActiveUniforms();
        ProgramSamplers.clearActiveSamplers();
        GlStateManager._glUseProgram(0);

        for (int i = 0; i < SamplerLimits.get().getMaxTextureUnits(); i++) {
            if (((GlStateManagerTextureStateAccessor) GlStateManagerAccessor.getTEXTURES()[i]).getBinding() != 0) {
                RenderSystem.activeTexture(GL15C.GL_TEXTURE0 + i);
                RenderSystem.bindTexture(0);
            }
        }

        RenderSystem.activeTexture(GL15C.GL_TEXTURE0);
        GLDebug.popGroup();
    }

    // TODO: Don't just copy this from DeferredWorldRenderingPipeline
    private Program createProgram(ProgramSource source, ImmutableSet<Integer> flipped, ImmutableSet<Integer> flippedAtLeastOnceSnapshot,
                                  Supplier<ShadowRenderTargets> shadowTargetsSupplier) {
        // TODO: Properly handle empty shaders
        Map<PatchShaderType, String> transformed = TransformPatcher.patchComposite(
                source.getName(),
                source.getVertexSource().orElseThrow(NullPointerException::new),
                source.getGeometrySource().orElse(null),
                source.getFragmentSource().orElseThrow(NullPointerException::new), textureStage, pipeline.getTextureMap());
        String vertex = transformed.get(PatchShaderType.VERTEX);
        String geometry = transformed.get(PatchShaderType.GEOMETRY);
        String fragment = transformed.get(PatchShaderType.FRAGMENT);

        ShaderPrinter.printProgram(source.getName()).addSources(transformed).print();

        Objects.requireNonNull(flipped);
        ProgramBuilder builder;

        try {
            builder = ProgramBuilder.begin(source.getName(), vertex, geometry, fragment,
                    IrisSamplers.COMPOSITE_RESERVED_TEXTURE_UNITS);
        } catch (ShaderCompileException e) {
            throw e;
        } catch (RuntimeException e) {
            // TODO: Better error handling
            throw new RuntimeException("Shader compilation failed for " + source.getName() + "!", e);
        }


        CommonUniforms.addDynamicUniforms(builder, FogMode.OFF);
        this.customUniforms.assignTo(builder);

        ProgramSamplers.CustomTextureSamplerInterceptor customTextureSamplerInterceptor = ProgramSamplers.customTextureSamplerInterceptor(builder, customTextureIds, flippedAtLeastOnceSnapshot);

        IrisSamplers.addRenderTargetSamplers(customTextureSamplerInterceptor, () -> flipped, renderTargets, true, pipeline);
        IrisSamplers.addCustomTextures(builder, irisCustomTextures);
        IrisSamplers.addCustomImages(customTextureSamplerInterceptor, customImages);

        IrisImages.addRenderTargetImages(builder, () -> flipped, renderTargets);
        IrisImages.addCustomImages(builder, customImages);

        IrisSamplers.addNoiseSampler(customTextureSamplerInterceptor, noiseTexture);
        IrisSamplers.addCompositeSamplers(customTextureSamplerInterceptor, renderTargets);

        if (IrisSamplers.hasShadowSamplers(customTextureSamplerInterceptor)) {
            IrisSamplers.addShadowSamplers(customTextureSamplerInterceptor, shadowTargetsSupplier.get(), null, pipeline.hasFeature(FeatureFlags.SEPARATE_HARDWARE_SAMPLERS));
            IrisImages.addShadowColorImages(builder, shadowTargetsSupplier.get(), null);
        }

        // TODO: Don't duplicate this with FinalPassRenderer
        centerDepthSampler.setUsage(builder.addDynamicSampler(centerDepthSampler::getCenterDepthTexture, "iris_centerDepthSmooth"));

        Program build = builder.build();

        // tell the customUniforms that those locations belong to this pass
        // this is just an object to index the internal map
        this.customUniforms.mapholderToPass(builder, build);

        return build;
    }

    private ComputeProgram[] createComputes(ComputeSource[] compute, ImmutableSet<Integer> flipped, ImmutableSet<Integer> flippedAtLeastOnceSnapshot, Supplier<ShadowRenderTargets> shadowTargetsSupplier, ShaderStorageBufferHolder holder) {
        ComputeProgram[] programs = new ComputeProgram[compute.length];
        for (int i = 0; i < programs.length; i++) {
            ComputeSource source = compute[i];
            if (source == null || source.getSource().isEmpty()) {
            } else {
                // TODO: Properly handle empty shaders
                Objects.requireNonNull(flipped);
                ProgramBuilder builder;

                try {
                    String transformed = TransformPatcher.patchCompute(source.getName(), source.getSource().orElse(null), textureStage, pipeline.getTextureMap());

                    ShaderPrinter.printProgram(source.getName()).addSource(PatchShaderType.COMPUTE, transformed).print();

                    builder = ProgramBuilder.beginCompute(source.getName(), transformed, IrisSamplers.COMPOSITE_RESERVED_TEXTURE_UNITS);
                } catch (ShaderCompileException e) {
                    throw e;
                } catch (RuntimeException e) {
                    // TODO: Better error handling
                    throw new RuntimeException("Shader compilation failed for compute " + source.getName() + "!", e);
                }

                ProgramSamplers.CustomTextureSamplerInterceptor customTextureSamplerInterceptor = ProgramSamplers.customTextureSamplerInterceptor(builder, customTextureIds, flippedAtLeastOnceSnapshot);

                CommonUniforms.addDynamicUniforms(builder, FogMode.OFF);

                customUniforms.assignTo(builder);

                IrisSamplers.addRenderTargetSamplers(customTextureSamplerInterceptor, () -> flipped, renderTargets, true, pipeline);
                IrisSamplers.addCustomTextures(builder, irisCustomTextures);
                IrisSamplers.addCustomImages(customTextureSamplerInterceptor, customImages);

                IrisImages.addRenderTargetImages(builder, () -> flipped, renderTargets);
                IrisImages.addCustomImages(builder, customImages);

                IrisSamplers.addNoiseSampler(customTextureSamplerInterceptor, noiseTexture);
                IrisSamplers.addCompositeSamplers(customTextureSamplerInterceptor, renderTargets);

                if (IrisSamplers.hasShadowSamplers(customTextureSamplerInterceptor)) {
                    IrisSamplers.addShadowSamplers(customTextureSamplerInterceptor, shadowTargetsSupplier.get(), null, pipeline.hasFeature(FeatureFlags.SEPARATE_HARDWARE_SAMPLERS));
                    IrisImages.addShadowColorImages(builder, shadowTargetsSupplier.get(), null);
                }

                // TODO: Don't duplicate this with FinalPassRenderer
                centerDepthSampler.setUsage(builder.addDynamicSampler(centerDepthSampler::getCenterDepthTexture, "iris_centerDepthSmooth"));

                programs[i] = builder.buildCompute();

                customUniforms.mapholderToPass(builder, programs[i]);

                programs[i].setWorkGroupInfo(source.getWorkGroupRelative(), source.getWorkGroups(), FilledIndirectPointer.basedOff(holder, source.getIndirectPointer()));
            }
        }


        return programs;
    }

    public void destroy() {
        for (Pass renderPass : passes) {
            renderPass.destroy();
        }
    }

    private static class Pass implements NamedCompositePass {
        int[] drawBuffers;
        int viewWidth;
        int viewHeight;
        String name;
        Program program;
        BlendModeOverride blendModeOverride;
        ComputeProgram[] computes;
        GlFramebuffer framebuffer;
        ImmutableSet<Integer> flippedAtLeastOnce;
        ImmutableSet<Integer> stageReadsFromAlt;
        ImmutableSet<Integer> mipmappedBuffers;
        ViewportData viewportScale;

        @Override
        public String superresolution$getName() {
            return name;
        }

        @Override
        public void superresolution$setName(String name) {
            this.name = name;
        }

        protected void destroy() {
            this.program.destroy();
            for (ComputeProgram compute : this.computes) {
                if (compute != null) {
                    compute.destroy();
                }
            }
        }
    }

    private static class ComputeOnlyPass extends Pass {
        @Override
        protected void destroy() {
            for (ComputeProgram compute : this.computes) {
                if (compute != null) {
                    compute.destroy();
                }
            }
        }
    }

    private static class CompositeRendererAccessorImpl implements ICompositeRendererAccessor{
        public CompositeRendererAccessorImpl(NewCompositeRenderer renderer) {
            this.renderer = renderer;
        }

        private final NewCompositeRenderer renderer;

        @Override
        public boolean isSameInstance(ICompositeRendererAccessor instance) {
            if (!(instance instanceof CompositeRendererAccessorImpl other)) return false;
            return other.renderer == this.renderer;
        }

        @Override
        public IrisCompositeRenderingPhase getPhase() {
            return switch (renderer.compositePass){
                case BEGIN -> IrisCompositeRenderingPhase.Begin;
                case PREPARE -> IrisCompositeRenderingPhase.Prepare;
                case DEFERRED -> IrisCompositeRenderingPhase.Deferred;
                case COMPOSITE -> IrisCompositeRenderingPhase.Composite;
            };
        }

        @Override
        public RenderTargets getRenderTargets() {
            return renderer.renderTargets;
        }

        @Override
        public void setRenderTargets(RenderTargets _renderTargets) {
            renderer.renderTargets = _renderTargets;
        }

        @Override
        public ImmutableList<NamedCompositePass> getPasses() {
            return ImmutableList.copyOf(renderer.passes);
        }

        @Override
        public void setPasses(ImmutableList<NamedCompositePass> _passes) {
            throw new UnsupportedOperationException();
        }

        @Override
        public WorldRenderingPipeline getPipeline() {
            return renderer.pipeline;
        }

        @Override
        public void setPipeline(WorldRenderingPipeline _pipeline) {
            renderer.pipeline = _pipeline;
        }

        @Override
        public TextureAccess getNoiseTexture() {
            return renderer.noiseTexture;
        }

        @Override
        public void setNoiseTexture(TextureAccess _noiseTexture) {
            renderer.noiseTexture = _noiseTexture;
        }

        @Override
        public CenterDepthSampler getCenterDepthSampler() {
            return renderer.centerDepthSampler;
        }

        @Override
        public void setCenterDepthSampler(CenterDepthSampler _centerDepthSampler) {
            renderer.centerDepthSampler = _centerDepthSampler;
        }

        @Override
        public Object2ObjectMap<String, TextureAccess> getCustomTextureIds() {
            return renderer.customTextureIds;
        }

        @Override
        public void setCustomTextureIds(Object2ObjectMap<String, TextureAccess> _customTextureIds) {
            renderer.customTextureIds = _customTextureIds;
        }

        @Override
        public ImmutableSet<Integer> getFlippedAtLeastOnceFinal() {
            return renderer.flippedAtLeastOnceFinal;
        }

        @Override
        public void setFlippedAtLeastOnceFinal(ImmutableSet<Integer> _flippedAtLeastOnceFinal) {
            renderer.flippedAtLeastOnceFinal = _flippedAtLeastOnceFinal;
        }

        @Override
        public CustomUniforms getCustomUniforms() {
            return renderer.customUniforms;
        }

        @Override
        public void setCustomUniforms(CustomUniforms _customUniforms) {
            renderer.customUniforms = _customUniforms;
        }

        @Override
        public Object2ObjectMap<String, TextureAccess> getIrisCustomTextures() {
            return renderer.irisCustomTextures;
        }

        @Override
        public void setIrisCustomTextures(Object2ObjectMap<String, TextureAccess> _irisCustomTextures) {
            renderer.irisCustomTextures = _irisCustomTextures;
        }

        @Override
        public Set<GlImage> getCustomImages() {
            return renderer.customImages;
        }

        @Override
        public void setCustomImages(Set<GlImage> _customImages) {
            renderer.customImages = _customImages;
        }

        @Override
        public TextureStage getTextureStage() {
            return renderer.textureStage;
        }

        @Override
        public void setTextureStage(TextureStage _textureStage) {
            renderer.textureStage = _textureStage;
        }
    }
    #endif
}
