package io.homo.superresolution.fsr2;

import io.homo.superresolution.core.gl.texture.GlTexture1D;
import io.homo.superresolution.core.gl.texture.GlTexture2D;
import io.homo.superresolution.core.impl.texture.TextureFormat;

import java.util.HashMap;
import java.util.Map;

public class Fsr2PipelineResources {

    public final Fsr2PipelineResourcesDescription inputColor =
            new Fsr2PipelineResourcesDescription()
                    .name("inputColor")
                    .shaderName("r_input_color_jittered")
                    .writable(false);

    public final Fsr2PipelineResourcesDescription inputOpaqueOnly =
            new Fsr2PipelineResourcesDescription()
                    .name("inputOpaqueOnly")
                    .shaderName("r_input_opaque_only")
                    .writable(false);

    public final Fsr2PipelineResourcesDescription inputMotionVectors =
            new Fsr2PipelineResourcesDescription()
                    .name("inputMotionVectors")
                    .shaderName("r_input_motion_vectors")
                    .writable(false);

    public final Fsr2PipelineResourcesDescription inputDepth =
            new Fsr2PipelineResourcesDescription()
                    .name("inputDepth")
                    .shaderName("r_input_depth")
                    .writable(false);

    public final Fsr2PipelineResourcesDescription inputExposure =
            new Fsr2PipelineResourcesDescription()
                    .name("inputExposure")
                    .shaderName("r_input_exposure")
                    .writable(false);

    public final Fsr2PipelineResourcesDescription autoExposure =
            new Fsr2PipelineResourcesDescription()
                    .name("autoExposure")
                    .shaderName("r_auto_exposure")
                    .writable(false);

    public final Fsr2PipelineResourcesDescription inputReactiveMask =
            new Fsr2PipelineResourcesDescription()
                    .name("inputReactiveMask")
                    .shaderName("r_reactive_mask")
                    .writable(false);

    public final Fsr2PipelineResourcesDescription inputTransparencyAndCompositionMask =
            new Fsr2PipelineResourcesDescription()
                    .name("inputTransparencyAndCompositionMask")
                    .shaderName("r_transparency_and_composition_mask")
                    .writable(false);

    public final Fsr2PipelineResourcesDescription reconstructedPreviousNearestDepth =
            new Fsr2PipelineResourcesDescription()
                    .name("reconstructedPreviousNearestDepth")
                    .shaderName("r_reconstructed_previous_nearest_depth")
                    .writable(false);

    public final Fsr2PipelineResourcesDescription dilatedMotionVectors =
            new Fsr2PipelineResourcesDescription()
                    .name("dilatedMotionVectors")
                    .shaderName("r_dilated_motion_vectors")
                    .writable(false);

    public final Fsr2PipelineResourcesDescription previousDilatedMotionVectors =
            new Fsr2PipelineResourcesDescription()
                    .name("previousDilatedMotionVectors")
                    .shaderName("r_previous_dilated_motion_vectors")
                    .writable(false);

    public final Fsr2PipelineResourcesDescription dilatedDepth =
            new Fsr2PipelineResourcesDescription()
                    .name("dilatedDepth")
                    .shaderName("r_dilatedDepth")
                    .writable(false);

    public final Fsr2PipelineResourcesDescription internalUpscaledColor =
            new Fsr2PipelineResourcesDescription()
                    .name("internalUpscaledColor")
                    .shaderName("r_internal_upscaled_color")
                    .writable(false);

    public final Fsr2PipelineResourcesDescription lockStatus =
            new Fsr2PipelineResourcesDescription()
                    .name("lockStatus")
                    .shaderName("r_lock_status")
                    .writable(false);

    public final Fsr2PipelineResourcesDescription preparedInputColor =
            new Fsr2PipelineResourcesDescription()
                    .name("preparedInputColor")
                    .shaderName("r_prepared_input_color")
                    .writable(false);

    public final Fsr2PipelineResourcesDescription lumaHistory =
            new Fsr2PipelineResourcesDescription()
                    .name("lumaHistory")
                    .shaderName("r_luma_history")
                    .writable(false);

    public final Fsr2PipelineResourcesDescription rcasInput =
            new Fsr2PipelineResourcesDescription()
                    .name("rcasInput")
                    .shaderName("r_rcas_input")
                    .writable(false);

    public final Fsr2PipelineResourcesDescription lanczosLut =
            new Fsr2PipelineResourcesDescription()
                    .name("lanczosLut")
                    .shaderName("r_lanczos_lut")
                    .writable(false);

    public final Fsr2PipelineResourcesDescription sceneLuminance =
            new Fsr2PipelineResourcesDescription()
                    .name("sceneLuminance")
                    .shaderName("r_imgMips")
                    .writable(false);

    public final Fsr2PipelineResourcesDescription sceneLuminanceMipmapShadingChange =
            new Fsr2PipelineResourcesDescription()
                    .name("sceneLuminanceMipmapShadingChange")
                    .shaderName("r_img_mip_shading_change")
                    .writable(false);

    public final Fsr2PipelineResourcesDescription sceneLuminanceMipmap5 =
            new Fsr2PipelineResourcesDescription()
                    .name("sceneLuminanceMipmap5")
                    .shaderName("r_img_mip_5")
                    .writable(false);

    public final Fsr2PipelineResourcesDescription upsampleMaximumBiasLut =
            new Fsr2PipelineResourcesDescription()
                    .name("upsampleMaximumBiasLut")
                    .shaderName("r_upsample_maximum_bias_lut")
                    .writable(false);

    public final Fsr2PipelineResourcesDescription dilatedReactiveMasks =
            new Fsr2PipelineResourcesDescription()
                    .name("dilatedReactiveMasks")
                    .shaderName("r_dilated_reactive_masks")
                    .writable(false);

    public final Fsr2PipelineResourcesDescription newLocks =
            new Fsr2PipelineResourcesDescription()
                    .name("newLocks")
                    .shaderName("r_new_locks")
                    .writable(false);

    public final Fsr2PipelineResourcesDescription lockInputLuma =
            new Fsr2PipelineResourcesDescription()
                    .name("lockInputLuma")
                    .shaderName("r_lock_input_luma")
                    .writable(false);

    public final Fsr2PipelineResourcesDescription prevPreAlphaColor =
            new Fsr2PipelineResourcesDescription()
                    .name("prevPreAlphaColor")
                    .shaderName("r_input_prev_color_pre_alpha")
                    .writable(false);

    public final Fsr2PipelineResourcesDescription prevPostAlphaColor =
            new Fsr2PipelineResourcesDescription()
                    .name("prevPostAlphaColor")
                    .shaderName("r_input_prev_color_post_alpha")
                    .writable(false);

    public final Fsr2PipelineResourcesDescription reconstructedPreviousNearestDepthUav =
            new Fsr2PipelineResourcesDescription()
                    .name("reconstructedPreviousNearestDepth")
                    .shaderName("rw_reconstructed_previous_nearest_depth")
                    .writable(true);

    public final Fsr2PipelineResourcesDescription dilatedMotionVectorsUav =
            new Fsr2PipelineResourcesDescription()
                    .name("dilatedMotionVectors")
                    .shaderName("rw_dilated_motion_vectors")
                    .writable(true);

    public final Fsr2PipelineResourcesDescription dilatedDepthUav =
            new Fsr2PipelineResourcesDescription()
                    .name("dilatedDepth")
                    .shaderName("rw_dilatedDepth")
                    .writable(true);

    public final Fsr2PipelineResourcesDescription internalUpscaledColorUav =
            new Fsr2PipelineResourcesDescription()
                    .name("internalUpscaledColor")
                    .shaderName("rw_internal_upscaled_color")
                    .writable(true);

    public final Fsr2PipelineResourcesDescription lockStatusUav =
            new Fsr2PipelineResourcesDescription()
                    .name("lockStatus")
                    .shaderName("rw_lock_status")
                    .writable(true);

    public final Fsr2PipelineResourcesDescription preparedInputColorUav =
            new Fsr2PipelineResourcesDescription()
                    .name("preparedInputColor")
                    .shaderName("rw_prepared_input_color")
                    .writable(true);

    public final Fsr2PipelineResourcesDescription lumaHistoryUav =
            new Fsr2PipelineResourcesDescription()
                    .name("lumaHistory")
                    .shaderName("rw_luma_history")
                    .writable(true);

    public final Fsr2PipelineResourcesDescription upscaledOutput =
            new Fsr2PipelineResourcesDescription()
                    .name("upscaledOutput")
                    .shaderName("rw_upscaled_output")
                    .writable(true);

    public final Fsr2PipelineResourcesDescription sceneLuminanceMipmapShadingChangeUav =
            new Fsr2PipelineResourcesDescription()
                    .name("sceneLuminanceMipmapShadingChange")
                    .shaderName("rw_img_mip_shading_change")
                    .writable(true);

    public final Fsr2PipelineResourcesDescription sceneLuminanceMipmap5Uav =
            new Fsr2PipelineResourcesDescription()
                    .name("sceneLuminanceMipmap5")
                    .shaderName("rw_img_mip_5")
                    .writable(true);

    public final Fsr2PipelineResourcesDescription dilatedReactiveMasksUav =
            new Fsr2PipelineResourcesDescription()
                    .name("dilatedReactiveMasks")
                    .shaderName("rw_dilated_reactive_masks")
                    .writable(true);

    public final Fsr2PipelineResourcesDescription autoExposureUav =
            new Fsr2PipelineResourcesDescription()
                    .name("autoExposure")
                    .shaderName("rw_auto_exposure")
                    .writable(true);

    public final Fsr2PipelineResourcesDescription spdAtomicCount =
            new Fsr2PipelineResourcesDescription()
                    .name("spdAtomicCount")
                    .shaderName("rw_spd_global_atomic")
                    .writable(true);

    public final Fsr2PipelineResourcesDescription newLocksUav =
            new Fsr2PipelineResourcesDescription()
                    .name("newLocks")
                    .shaderName("rw_new_locks")
                    .writable(true);

    public final Fsr2PipelineResourcesDescription lockInputLumaUav =
            new Fsr2PipelineResourcesDescription()
                    .name("lockInputLuma")
                    .shaderName("rw_lock_input_luma")
                    .writable(true);

    public final Fsr2PipelineResourcesDescription autoReactive =
            new Fsr2PipelineResourcesDescription()
                    .name("autoReactive")
                    .shaderName("rw_output_autoreactive")
                    .writable(true);

    public final Fsr2PipelineResourcesDescription autoComposition =
            new Fsr2PipelineResourcesDescription()
                    .name("autoComposition")
                    .shaderName("rw_output_autocomposition")
                    .writable(true);

    public final Fsr2PipelineResourcesDescription prevPreAlphaColorUav =
            new Fsr2PipelineResourcesDescription()
                    .name("prevPreAlphaColor")
                    .shaderName("rw_output_prev_color_pre_alpha")
                    .writable(true);

    public final Fsr2PipelineResourcesDescription prevPostAlphaColorUav =
            new Fsr2PipelineResourcesDescription()
                    .name("prevPostAlphaColor")
                    .shaderName("rw_output_prev_color_post_alpha")
                    .writable(true);
    public final Fsr2PipelineResourcesDescription lockStatus1 =
            new Fsr2PipelineResourcesDescription()
                    .name("lockStatus1")
                    .shaderName("")  // 不在绑定表中，无需映射
                    .writable(true);

    public final Fsr2PipelineResourcesDescription lockStatus2 =
            new Fsr2PipelineResourcesDescription()
                    .name("lockStatus2")
                    .shaderName("")
                    .writable(true);

    public final Fsr2PipelineResourcesDescription internalDilatedMotionVectors1 =
            new Fsr2PipelineResourcesDescription()
                    .name("internalDilatedMotionVectors1")
                    .shaderName("")
                    .writable(true);

    public final Fsr2PipelineResourcesDescription internalDilatedMotionVectors2 =
            new Fsr2PipelineResourcesDescription()
                    .name("internalDilatedMotionVectors2")
                    .shaderName("")
                    .writable(true);

    public final Fsr2PipelineResourcesDescription lumaHistory1 =
            new Fsr2PipelineResourcesDescription()
                    .name("lumaHistory1")
                    .shaderName("")
                    .writable(true);

    public final Fsr2PipelineResourcesDescription lumaHistory2 =
            new Fsr2PipelineResourcesDescription()
                    .name("lumaHistory2")
                    .shaderName("")
                    .writable(true);

    public final Fsr2PipelineResourcesDescription prevPreAlphaColor1 =
            new Fsr2PipelineResourcesDescription()
                    .name("prevPreAlphaColor1")
                    .shaderName("")
                    .writable(true);

    public final Fsr2PipelineResourcesDescription prevPostAlphaColor1 =
            new Fsr2PipelineResourcesDescription()
                    .name("prevPostAlphaColor1")
                    .shaderName("")
                    .writable(true);

    public final Fsr2PipelineResourcesDescription prevPreAlphaColor2 =
            new Fsr2PipelineResourcesDescription()
                    .name("prevPreAlphaColor2")
                    .shaderName("")
                    .writable(true);

    public final Fsr2PipelineResourcesDescription prevPostAlphaColor2 =
            new Fsr2PipelineResourcesDescription()
                    .name("prevPostAlphaColor2")
                    .shaderName("")
                    .writable(true);

    private final Map<String, Fsr2PipelineResourcesDescription> shaderNameResourceMap = createShaderNameResourceMap();

    public Fsr2PipelineResources() {
    }

    public Map<String, Fsr2PipelineResourcesDescription> getShaderNameResourceMap() {
        return shaderNameResourceMap;
    }

    public void init(int renderWidth, int renderHeight, int upscaledWidth, int upscaledHeight) {
        TextureFormat colorFormat = TextureFormat.RGBA16F;
        TextureFormat r16fFormat = TextureFormat.R16F;
        TextureFormat r32fFormat = TextureFormat.R32F;
        TextureFormat rg16fFormat = TextureFormat.RG16F;
        TextureFormat rg32fFormat = TextureFormat.RG32F;

        this.reconstructedPreviousNearestDepthUav.texture(
                GlTexture2D.create(renderWidth, renderHeight, r16fFormat)
        );
        this.dilatedMotionVectorsUav.texture(
                GlTexture2D.create(renderWidth, renderHeight, rg16fFormat)
        );
        this.dilatedDepthUav.texture(
                GlTexture2D.create(renderWidth, renderHeight, r16fFormat)
        );
        this.internalUpscaledColorUav.texture(
                GlTexture2D.create(upscaledWidth, upscaledHeight, colorFormat)
        );
        this.lockStatusUav.texture(
                GlTexture2D.create(renderWidth, renderHeight, r32fFormat)
        );
        this.preparedInputColorUav.texture(
                GlTexture2D.create(renderWidth, renderHeight, colorFormat)
        );
        this.lumaHistoryUav.texture(
                GlTexture2D.create(renderWidth, renderHeight, r16fFormat)
        );
        this.upscaledOutput.texture(
                GlTexture2D.create(upscaledWidth, upscaledHeight, colorFormat)
        );
        this.sceneLuminanceMipmapShadingChangeUav.texture(
                GlTexture2D.create(renderWidth, renderHeight, r16fFormat)
        );
        this.sceneLuminanceMipmap5Uav.texture(
                GlTexture2D.create(renderWidth, renderHeight, r16fFormat)
        );
        this.dilatedReactiveMasksUav.texture(
                GlTexture2D.create(renderWidth, renderHeight, r16fFormat)
        );
        this.autoExposureUav.texture(
                GlTexture2D.create(1, 1, r32fFormat)
        );
        this.spdAtomicCount.texture(
                GlTexture1D.create(1, r32fFormat)
        );
        this.newLocksUav.texture(
                GlTexture2D.create(renderWidth, renderHeight, r32fFormat)
        );
        this.lockInputLumaUav.texture(
                GlTexture2D.create(renderWidth, renderHeight, r16fFormat)
        );
        this.autoReactive.texture(
                GlTexture2D.create(renderWidth, renderHeight, r16fFormat)
        );
        this.autoComposition.texture(
                GlTexture2D.create(renderWidth, renderHeight, r16fFormat)
        );
        this.prevPreAlphaColorUav.texture(
                GlTexture2D.create(renderWidth, renderHeight, colorFormat)
        );
        this.prevPostAlphaColorUav.texture(
                GlTexture2D.create(renderWidth, renderHeight, colorFormat)
        );
        this.lanczosLut.texture(
                GlTexture1D.create(128, r16fFormat)
        );
        this.upsampleMaximumBiasLut.texture(
                GlTexture1D.create(128, r16fFormat)
        );
        this.internalDilatedMotionVectors1.texture(
                GlTexture2D.create(renderWidth, renderHeight, TextureFormat.RG16F)
        );
        this.internalDilatedMotionVectors2.texture(
                GlTexture2D.create(renderWidth, renderHeight, TextureFormat.RG16F)
        );
        this.lockStatus1.texture(
                GlTexture2D.create(upscaledWidth, upscaledHeight, TextureFormat.RG16F)
        );
        this.lockStatus2.texture(
                GlTexture2D.create(upscaledWidth, upscaledHeight, TextureFormat.RG16F)
        );
        this.lumaHistory1.texture(
                GlTexture2D.create(upscaledWidth, upscaledHeight, TextureFormat.RGBA8)
        );
        this.lumaHistory2.texture(
                GlTexture2D.create(upscaledWidth, upscaledHeight, TextureFormat.RG16F)
        );
        this.prevPreAlphaColor1.texture(
                GlTexture2D.create(renderWidth, renderHeight, TextureFormat.R11G11B10F)
        );
        this.prevPostAlphaColor1.texture(
                GlTexture2D.create(renderWidth, renderHeight, TextureFormat.R11G11B10F)
        );
        this.prevPreAlphaColor2.texture(
                GlTexture2D.create(renderWidth, renderHeight, TextureFormat.R11G11B10F)
        );
        this.prevPostAlphaColor2.texture(
                GlTexture2D.create(renderWidth, renderHeight, TextureFormat.R11G11B10F)
        );
        int sceneLuminanceWidth = renderWidth / 2;
        int sceneLuminanceHeight = renderHeight / 2;
        this.sceneLuminance.texture(
                GlTexture2D.create(sceneLuminanceWidth, sceneLuminanceHeight, TextureFormat.R16F, GlTexture2D.AUTO_MIPMAP_LEVEL)
        );

        this.lanczosLut.texture(
                GlTexture1D.create(128, TextureFormat.R16_SNORM)
        );
        this.upsampleMaximumBiasLut.texture(
                GlTexture2D.create(32, 32, TextureFormat.R16_SNORM)
        );
    }

    public void destroy() {
        destroyResource(reconstructedPreviousNearestDepthUav);
        destroyResource(dilatedMotionVectorsUav);
        destroyResource(dilatedDepthUav);
        destroyResource(internalUpscaledColorUav);
        destroyResource(lockStatusUav);
        destroyResource(preparedInputColorUav);
        destroyResource(lumaHistoryUav);
        destroyResource(upscaledOutput);
        destroyResource(sceneLuminanceMipmapShadingChangeUav);
        destroyResource(sceneLuminanceMipmap5Uav);
        destroyResource(dilatedReactiveMasksUav);
        destroyResource(autoExposureUav);
        destroyResource(spdAtomicCount);
        destroyResource(newLocksUav);
        destroyResource(lockInputLumaUav);
        destroyResource(autoReactive);
        destroyResource(autoComposition);
        destroyResource(prevPreAlphaColorUav);
        destroyResource(prevPostAlphaColorUav);
        destroyResource(lanczosLut);
        destroyResource(upsampleMaximumBiasLut);
        destroyResource(sceneLuminance);
        destroyResource(lockStatus1);
        destroyResource(lockStatus2);
        destroyResource(internalDilatedMotionVectors1);
        destroyResource(internalDilatedMotionVectors2);
        destroyResource(lumaHistory1);
        destroyResource(lumaHistory2);
        destroyResource(prevPreAlphaColor1);
        destroyResource(prevPostAlphaColor1);
        destroyResource(prevPreAlphaColor2);
        destroyResource(prevPostAlphaColor2);
    }

    private void destroyResource(Fsr2PipelineResourcesDescription desc) {
        if (desc != null && desc.texture() != null) {
            desc.texture().destroy();
            desc.texture(null);
        }
    }

    public Map<String, Fsr2PipelineResourcesDescription> createShaderNameResourceMap() {
        Map<String, Fsr2PipelineResourcesDescription> map = new HashMap<>();
        map.put(inputColor.shaderName(), inputColor);
        map.put(inputOpaqueOnly.shaderName(), inputOpaqueOnly);
        map.put(inputMotionVectors.shaderName(), inputMotionVectors);
        map.put(inputDepth.shaderName(), inputDepth);
        map.put(inputExposure.shaderName(), inputExposure);
        map.put(autoExposure.shaderName(), autoExposure);
        map.put(inputReactiveMask.shaderName(), inputReactiveMask);
        map.put(inputTransparencyAndCompositionMask.shaderName(), inputTransparencyAndCompositionMask);
        map.put(reconstructedPreviousNearestDepth.shaderName(), reconstructedPreviousNearestDepth);
        map.put(dilatedMotionVectors.shaderName(), dilatedMotionVectors);
        map.put(previousDilatedMotionVectors.shaderName(), previousDilatedMotionVectors);
        map.put(dilatedDepth.shaderName(), dilatedDepth);
        map.put(internalUpscaledColor.shaderName(), internalUpscaledColor);
        map.put(lockStatus.shaderName(), lockStatus);
        map.put(preparedInputColor.shaderName(), preparedInputColor);
        map.put(lumaHistory.shaderName(), lumaHistory);
        map.put(rcasInput.shaderName(), rcasInput);
        map.put(lanczosLut.shaderName(), lanczosLut);
        map.put(sceneLuminance.shaderName(), sceneLuminance);
        map.put(sceneLuminanceMipmapShadingChange.shaderName(), sceneLuminanceMipmapShadingChange);
        map.put(sceneLuminanceMipmap5.shaderName(), sceneLuminanceMipmap5);
        map.put(upsampleMaximumBiasLut.shaderName(), upsampleMaximumBiasLut);
        map.put(dilatedReactiveMasks.shaderName(), dilatedReactiveMasks);
        map.put(newLocks.shaderName(), newLocks);
        map.put(lockInputLuma.shaderName(), lockInputLuma);
        map.put(prevPreAlphaColor.shaderName(), prevPreAlphaColor);
        map.put(prevPostAlphaColor.shaderName(), prevPostAlphaColor);
        map.put(reconstructedPreviousNearestDepthUav.shaderName(), reconstructedPreviousNearestDepthUav);
        map.put(dilatedMotionVectorsUav.shaderName(), dilatedMotionVectorsUav);
        map.put(dilatedDepthUav.shaderName(), dilatedDepthUav);
        map.put(internalUpscaledColorUav.shaderName(), internalUpscaledColorUav);
        map.put(lockStatusUav.shaderName(), lockStatusUav);
        map.put(preparedInputColorUav.shaderName(), preparedInputColorUav);
        map.put(lumaHistoryUav.shaderName(), lumaHistoryUav);
        map.put(upscaledOutput.shaderName(), upscaledOutput);
        map.put(sceneLuminanceMipmapShadingChangeUav.shaderName(), sceneLuminanceMipmapShadingChangeUav);
        map.put(sceneLuminanceMipmap5Uav.shaderName(), sceneLuminanceMipmap5Uav);
        map.put(dilatedReactiveMasksUav.shaderName(), dilatedReactiveMasksUav);
        map.put(autoExposureUav.shaderName(), autoExposureUav);
        map.put(spdAtomicCount.shaderName(), spdAtomicCount);
        map.put(newLocksUav.shaderName(), newLocksUav);
        map.put(lockInputLumaUav.shaderName(), lockInputLumaUav);
        map.put(autoReactive.shaderName(), autoReactive);
        map.put(autoComposition.shaderName(), autoComposition);
        map.put(prevPreAlphaColorUav.shaderName(), prevPreAlphaColorUav);
        map.put(prevPostAlphaColorUav.shaderName(), prevPostAlphaColorUav);
        return map;
    }
}