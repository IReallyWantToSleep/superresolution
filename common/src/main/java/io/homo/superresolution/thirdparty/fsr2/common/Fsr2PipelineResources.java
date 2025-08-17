package io.homo.superresolution.thirdparty.fsr2.common;

import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.impl.texture.*;
import io.homo.superresolution.core.graphics.opengl.buffer.GlBuffer;
import io.homo.superresolution.core.graphics.opengl.texture.GlTexture1D;
import io.homo.superresolution.core.graphics.opengl.texture.GlTexture2D;
import io.homo.superresolution.core.impl.Destroyable;
import io.homo.superresolution.core.math.Vector2f;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Fsr2PipelineResources {


    private final Map<Fsr2ResourceEntry, Fsr2PipelineResourceType> resourceEntriesMap = new HashMap<>();
    private final Map<Fsr2PipelineResourceType, Fsr2ResourceEntry> resources = new HashMap<>();
    private final Map<Fsr2PipelineResourceType, Fsr2ResourceCreateDescription> resourceCreateDescriptions = new HashMap<>();
    private final Map<String, Fsr2PipelineResourceType> shaderNameMap = new HashMap<>();

    public Fsr2PipelineResources() {
    }

    public Map<Fsr2ResourceEntry, Fsr2PipelineResourceType> resourceEntriesMap() {
        return resourceEntriesMap;
    }

    public Map<Fsr2PipelineResourceType, Fsr2ResourceEntry> resources() {
        return resources;
    }

    public Map<Fsr2PipelineResourceType, Fsr2ResourceCreateDescription> resourceCreateDescriptions() {
        return resourceCreateDescriptions;
    }

    public Map<String, Fsr2PipelineResourceType> shaderNameMap() {
        return shaderNameMap;
    }

    private void addResourceDescription(Fsr2PipelineResourceType type, Fsr2ResourceCreateDescription description) {
        if (resourceCreateDescriptions.containsKey(type) || resources.containsKey(type))
            throw new RuntimeException(type.toString());
        resourceCreateDescriptions.put(type, description);
        resources.put(type, new Fsr2ResourceEntry(description));
        resourceEntriesMap.put(resources.get(type), type);
        if (type.srvShaderName() != null) {
            shaderNameMap.put(type.srvShaderName(), type);
        }
        if (type.uavShaderName() != null) {
            shaderNameMap.put(type.uavShaderName(), type);
        }
    }

    public void init(int renderWidth, int renderHeight, int upscaledWidth, int upscaledHeight) {
        resourceCreateDescriptions.clear();
        resources.clear();
        addResourceDescription(Fsr2PipelineResourceType.INPUT_COLOR,
                new Fsr2ResourceCreateDescription(null, null, 2, "InputColor"));
        addResourceDescription(Fsr2PipelineResourceType.INPUT_OPAQUE_ONLY,
                new Fsr2ResourceCreateDescription(null, null, 2, "InputOpaqueOnly"));
        addResourceDescription(Fsr2PipelineResourceType.INPUT_MOTION_VECTORS,
                new Fsr2ResourceCreateDescription(null, null, 2, "InputMotionVectors"));
        addResourceDescription(Fsr2PipelineResourceType.INPUT_DEPTH,
                new Fsr2ResourceCreateDescription(null, null, 2, "InputDepth"));
        addResourceDescription(Fsr2PipelineResourceType.INPUT_EXPOSURE,
                new Fsr2ResourceCreateDescription(null, null, 2, "InputExposure"));
        addResourceDescription(Fsr2PipelineResourceType.INPUT_REACTIVE_MASK,
                new Fsr2ResourceCreateDescription(null, null, 2, "InputReactiveMask"));
        addResourceDescription(Fsr2PipelineResourceType.INPUT_TRANSPARENCY_AND_COMPOSITION_MASK,
                new Fsr2ResourceCreateDescription(null, null, 2, "InputTransparencyAndCompositionMask"));
        addResourceDescription(Fsr2PipelineResourceType.UPSCALED_OUTPUT,
                new Fsr2ResourceCreateDescription(null, null, 2, "upscaledOutput"));
        addResourceDescription(Fsr2PipelineResourceType.DILATED_MOTION_VECTORS,
                new Fsr2ResourceCreateDescription(null, null, 2, "DilatedMotionVectors"));
        addResourceDescription(Fsr2PipelineResourceType.PREVIOUS_DILATED_MOTION_VECTORS,
                new Fsr2ResourceCreateDescription(null, null, 2, "PreviousDilatedMotionVectors"));
        addResourceDescription(Fsr2PipelineResourceType.INTERNAL_UPSCALED_COLOR,
                new Fsr2ResourceCreateDescription(null, null, 2, "InternalUpscaledColor"));
        addResourceDescription(Fsr2PipelineResourceType.LOCK_STATUS,
                new Fsr2ResourceCreateDescription(null, null, 2, "LockStatus"));
        addResourceDescription(Fsr2PipelineResourceType.LUMA_HISTORY,
                new Fsr2ResourceCreateDescription(null, null, 2, "LumaHistory"));
        addResourceDescription(Fsr2PipelineResourceType.RCAS_INPUT,
                new Fsr2ResourceCreateDescription(null, null, 2, "RcasInput"));
        addResourceDescription(Fsr2PipelineResourceType.SCENE_LUMINANCE_MIPMAP_SHADING_CHANGE,
                new Fsr2ResourceCreateDescription(null, null, 2, "SceneLuminanceShadingChange"));
        addResourceDescription(Fsr2PipelineResourceType.SCENE_LUMINANCE_MIPMAP_5,
                new Fsr2ResourceCreateDescription(null, null, 2, "SceneLuminanceMip5"));
        addResourceDescription(Fsr2PipelineResourceType.PREV_PRE_ALPHA_COLOR,
                new Fsr2ResourceCreateDescription(null, null, 2, "PrevPreAlphaColor"));
        addResourceDescription(Fsr2PipelineResourceType.PREV_POST_ALPHA_COLOR,
                new Fsr2ResourceCreateDescription(null, null, 2, "PrevPostAlphaColor"));

        // 动态资源
        addResourceDescription(Fsr2PipelineResourceType.PREPARED_INPUT_COLOR,
                new Fsr2ResourceCreateDescription(
                        new Vector2f(renderWidth, renderHeight), TextureFormat.RGBA16F, 2, "FSR2_PreparedInputColor"));
        addResourceDescription(Fsr2PipelineResourceType.RECONSTRUCTED_PREVIOUS_NEAREST_DEPTH,
                new Fsr2ResourceCreateDescription(
                        new Vector2f(renderWidth, renderHeight), TextureFormat.R32UI, 2, "FSR2_ReconstructedPrevNearestDepth"));
        addResourceDescription(Fsr2PipelineResourceType.INTERNAL_DILATED_MOTION_VECTORS_1,
                new Fsr2ResourceCreateDescription(
                        new Vector2f(renderWidth, renderHeight), TextureFormat.RG16F, 2, "FSR2_InternalDilatedVelocity1"));
        addResourceDescription(Fsr2PipelineResourceType.INTERNAL_DILATED_MOTION_VECTORS_2,
                new Fsr2ResourceCreateDescription(
                        new Vector2f(renderWidth, renderHeight), TextureFormat.RG16F, 2, "FSR2_InternalDilatedVelocity2"));
        addResourceDescription(Fsr2PipelineResourceType.DILATED_DEPTH,
                new Fsr2ResourceCreateDescription(
                        new Vector2f(renderWidth, renderHeight), TextureFormat.R32F, 2, "FSR2_DilatedDepth"));
        addResourceDescription(Fsr2PipelineResourceType.LOCK_STATUS_1,
                new Fsr2ResourceCreateDescription(
                        new Vector2f(upscaledWidth, upscaledHeight), TextureFormat.RG16F, 2, "FSR2_LockStatus1"));
        addResourceDescription(Fsr2PipelineResourceType.LOCK_STATUS_2,
                new Fsr2ResourceCreateDescription(
                        new Vector2f(upscaledWidth, upscaledHeight), TextureFormat.RG16F, 2, "FSR2_LockStatus2"));
        addResourceDescription(Fsr2PipelineResourceType.LOCK_INPUT_LUMA,
                new Fsr2ResourceCreateDescription(
                        new Vector2f(renderWidth, renderHeight), TextureFormat.R16F, 2, "FSR2_LockInputLuma"));
        addResourceDescription(Fsr2PipelineResourceType.NEW_LOCKS,
                new Fsr2ResourceCreateDescription(
                        new Vector2f(upscaledWidth, upscaledHeight), TextureFormat.R8, 2, "FSR2_NewLocks"));
        addResourceDescription(Fsr2PipelineResourceType.INTERNAL_UPSCALED_COLOR_1,
                new Fsr2ResourceCreateDescription(
                        new Vector2f(upscaledWidth, upscaledHeight), TextureFormat.RGBA16F, 2, "FSR2_InternalUpscaled1"));
        addResourceDescription(Fsr2PipelineResourceType.INTERNAL_UPSCALED_COLOR_2,
                new Fsr2ResourceCreateDescription(
                        new Vector2f(upscaledWidth, upscaledHeight), TextureFormat.RGBA16F, 2, "FSR2_InternalUpscaled2"));
        addResourceDescription(Fsr2PipelineResourceType.SCENE_LUMINANCE,
                new Fsr2ResourceCreateDescription(
                        new Vector2f(Math.max((float) Math.ceil(renderWidth / 2f), 1), Math.max((float) Math.ceil(renderHeight / 2f), 1)), TextureFormat.R16F, 2, "FSR2_ExposureMips", 0));
        addResourceDescription(Fsr2PipelineResourceType.LUMA_HISTORY_1,
                new Fsr2ResourceCreateDescription(
                        new Vector2f(upscaledWidth, upscaledHeight), TextureFormat.RGBA8, 2, "FSR2_LumaHistory1"));
        addResourceDescription(Fsr2PipelineResourceType.LUMA_HISTORY_2,
                new Fsr2ResourceCreateDescription(
                        new Vector2f(upscaledWidth, upscaledHeight), TextureFormat.RGBA8, 2, "FSR2_LumaHistory2"));
        addResourceDescription(Fsr2PipelineResourceType.SPD_ATOMIC_COUNT,
                new Fsr2ResourceCreateDescription(
                        new Vector2f(1), TextureFormat.R32UI, 2, "FSR2_SpdAtomicCounter"));
        addResourceDescription(Fsr2PipelineResourceType.DILATED_REACTIVE_MASKS,
                new Fsr2ResourceCreateDescription(
                        new Vector2f(renderWidth, renderHeight), TextureFormat.RG8, 2, "FSR2_DilatedReactiveMasks"));
        addResourceDescription(Fsr2PipelineResourceType.LANCZOS_LUT,
                new Fsr2ResourceCreateDescription(
                        new Vector2f(128, 1), TextureFormat.R16_SNORM, 1, "FSR2_LanczosLutData"));
        addResourceDescription(Fsr2PipelineResourceType.INTERNAL_DEFAULT_REACTIVITY,
                new Fsr2ResourceCreateDescription(
                        new Vector2f(1), TextureFormat.R8, 1, "FSR2_DefaultReactiviyMask"));
        addResourceDescription(Fsr2PipelineResourceType.UPSAMPLE_MAXIMUM_BIAS_LUT,
                new Fsr2ResourceCreateDescription(
                        new Vector2f(Fsr2MaximumBias.FFX_FSR2_MAXIMUM_BIAS_TEXTURE_WIDTH, Fsr2MaximumBias.FFX_FSR2_MAXIMUM_BIAS_TEXTURE_HEIGHT), TextureFormat.R16_SNORM, 2, "FSR2_MaximumUpsampleBias"));
        addResourceDescription(Fsr2PipelineResourceType.INTERNAL_DEFAULT_EXPOSURE,
                new Fsr2ResourceCreateDescription(
                        new Vector2f(1), TextureFormat.RG32F, 1, "FSR2_DefaultExposure"));
        addResourceDescription(Fsr2PipelineResourceType.AUTO_EXPOSURE,
                new Fsr2ResourceCreateDescription(
                        new Vector2f(1), TextureFormat.RG32F, 1, "FSR2_AutoExposure"));
        addResourceDescription(Fsr2PipelineResourceType.AUTOREACTIVE,
                new Fsr2ResourceCreateDescription(
                        new Vector2f(renderWidth, renderHeight), TextureFormat.R8, 2, "FSR2_AutoReactive"));
        addResourceDescription(Fsr2PipelineResourceType.AUTOCOMPOSITION,
                new Fsr2ResourceCreateDescription(
                        new Vector2f(renderWidth, renderHeight), TextureFormat.R8, 2, "FSR2_AutoComposition"));
        addResourceDescription(Fsr2PipelineResourceType.PREV_PRE_ALPHA_COLOR_1,
                new Fsr2ResourceCreateDescription(
                        new Vector2f(renderWidth, renderHeight), TextureFormat.R11G11B10F, 2, "FSR2_PrevPreAlpha0"));
        addResourceDescription(Fsr2PipelineResourceType.PREV_POST_ALPHA_COLOR_1,
                new Fsr2ResourceCreateDescription(
                        new Vector2f(renderWidth, renderHeight), TextureFormat.R11G11B10F, 2, "FSR2_PrevPostAlpha0"));
        addResourceDescription(Fsr2PipelineResourceType.PREV_PRE_ALPHA_COLOR_2,
                new Fsr2ResourceCreateDescription(
                        new Vector2f(renderWidth, renderHeight), TextureFormat.R11G11B10F, 2, "FSR2_PrevPreAlpha1"));
        addResourceDescription(Fsr2PipelineResourceType.PREV_POST_ALPHA_COLOR_2,
                new Fsr2ResourceCreateDescription(
                        new Vector2f(renderWidth, renderHeight), TextureFormat.R11G11B10F, 2, "FSR2_PrevPostAlpha1"));

        Fsr2Context.LOGGER.info("开始初始化纹理");
        Set<Map.Entry<Fsr2PipelineResourceType, Fsr2ResourceCreateDescription>> entries = new HashSet<>(resourceCreateDescriptions.entrySet());
        for (Map.Entry<Fsr2PipelineResourceType, Fsr2ResourceCreateDescription> entry : entries) {
            final Fsr2PipelineResourceType type = entry.getKey();
            final Fsr2ResourceCreateDescription desc = entry.getValue();
            final Fsr2ResourceEntry resourceEntry = resources.get(type);
            Fsr2Context.LOGGER.info(
                    "创建纹理 {} Size:{}x{} Format:{}",
                    desc.label,
                    desc.size != null ? desc.size.x : "null",
                    desc.size != null ? desc.size.y : "null",
                    desc.format != null ? desc.format : "null"
            );

            if (desc.size == null || desc.format == null) {
                continue;
            }

            resources.get(type).setNeedDestroy(true);

            if (desc.dim == 1) {
                if (desc.size.y != 1) throw new RuntimeException(desc.label);
                GlTexture1D tex = (GlTexture1D) RenderSystems.current().device().createTexture(
                        TextureDescription.create()
                                .type(TextureType.Texture1D)
                                .usages(TextureUsages.create().storage().sampler())
                                .width((int) desc.size.x)
                                .mipmapSettings(
                                        desc.mipCount == 0 ?
                                                TextureMipmapSettings.auto() :
                                                desc.mipCount == -1 ?
                                                        TextureMipmapSettings.disabled() :
                                                        TextureMipmapSettings.manual(desc.mipCount)
                                )
                                .format(desc.format)
                                .filterMode(
                                        desc.mipCount == 0 ?
                                                TextureFilterMode.LINEAR :
                                                desc.mipCount == -1 ?
                                                        TextureFilterMode.NEAREST :
                                                        TextureFilterMode.LINEAR
                                )
                                .label(desc.label)
                                .build()
                );

                resourceEntry.setResource(tex);
            } else if (desc.dim == 2) {
                GlTexture2D tex = (GlTexture2D) RenderSystems.current().device().createTexture(
                        TextureDescription.create()
                                .type(TextureType.Texture2D)
                                .usages(TextureUsages.create().storage().sampler())
                                .width((int) desc.size.x)
                                .height((int) desc.size.y)
                                .mipmapSettings(
                                        desc.mipCount == 0 ?
                                                TextureMipmapSettings.auto() :
                                                desc.mipCount == -1 ?
                                                        TextureMipmapSettings.disabled() :
                                                        TextureMipmapSettings.manual(desc.mipCount)
                                )
                                .format(desc.format)
                                .filterMode(
                                        desc.mipCount == 0 ?
                                                TextureFilterMode.LINEAR :
                                                desc.mipCount == -1 ?
                                                        TextureFilterMode.NEAREST :
                                                        TextureFilterMode.LINEAR
                                )
                                .label(desc.label)
                                .build()
                );
                resourceEntry.setResource(tex);
            } else {
                throw new RuntimeException(desc.label);
            }
        }
    }

    public void destroy() {
        for (Fsr2ResourceEntry entry : resources.values()) {
            if (entry.getResource() != null) {
                if (entry.getResource() instanceof Destroyable && entry.needDestroy()) {
                    ((Destroyable) entry.getResource()).destroy();
                }
                entry.setResource(null);
            }
        }
    }

    public Fsr2ResourceEntry resource(Fsr2PipelineResourceType type) {
        Fsr2ResourceEntry entry = resources.get(type);
        if (entry == null) throw new RuntimeException("资源未找到: " + type);
        return entry;
    }

    public enum Fsr2ResourceType {
        TEXTURE, UBO, NULL
    }

    public static class Fsr2ResourceEntry {
        private final Fsr2ResourceCreateDescription description;
        private Object resource;
        private boolean needDestroy = false;

        public Fsr2ResourceEntry(Fsr2ResourceCreateDescription description) {
            this.description = description;
        }

        public boolean needDestroy() {
            return needDestroy;
        }

        public Fsr2ResourceEntry setNeedDestroy(boolean needDestroy) {
            this.needDestroy = needDestroy;
            return this;
        }

        public Fsr2ResourceType type() {
            return resource == null ? Fsr2ResourceType.NULL :
                    (resource instanceof GlBuffer ? Fsr2ResourceType.UBO : Fsr2ResourceType.TEXTURE);
        }

        public Fsr2ResourceCreateDescription getDescription() {
            return description;
        }

        public Object getResource() {
            return resource;
        }

        public void setResource(Object resource) {
            this.resource = resource;
        }
    }
}