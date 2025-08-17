package io.homo.superresolution.common.upscale.ffxfsr;

import io.homo.superresolution.api.AbstractAlgorithm;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.minecraft.MinecraftRenderHandle;
import io.homo.superresolution.common.upscale.AlgorithmManager;
import io.homo.superresolution.common.upscale.DispatchResource;
import io.homo.superresolution.core.NativeLibManager;
import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.impl.framebuffer.FrameBufferAttachmentType;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.core.graphics.impl.texture.*;
import io.homo.superresolution.core.graphics.opengl.GlState;
import io.homo.superresolution.core.graphics.opengl.framebuffer.GlFrameBuffer;
import io.homo.superresolution.core.math.Vector2f;
import io.homo.superresolution.core.math.Vector2i;
import io.homo.superresolution.srapi.*;
import net.minecraft.client.Minecraft;

import java.nio.file.Path;

public class FfxFSROgl extends AbstractAlgorithm {
    private SRUpscaleContext context;
    private ITexture srcInputColorGlTexture;
    private ITexture srcInputDepthGlTexture;
    private ITexture srcMotionVectorsGlTexture;
    private ITexture outputColorGlTexture;
    private IFrameBuffer outputColorFrameBuffer;

    public boolean updateFsr() {
        if (NativeLibManager.LIB_SUPER_RESOLUTION_FSRGL == null) return false;
        Path lib = NativeLibManager.LIB_SUPER_RESOLUTION_FSRGL.getTargetPath(Minecraft.getInstance().gameDirectory.toPath());
        if (!(lib.toFile().isFile() && lib.toFile().canRead())) return false;
        if (context != null) {
            if (context.nativePtr > 0) {
                SuperResolutionNativeAPI.srDestroyUpscaleContext(context);
            }
        }
        SuperResolutionNativeAPI.srLoadUpscaleProvidersFromLibrary(
                lib.toAbsolutePath().toString(),
                "srGetFfxFSRUpscaleProviders",
                "srGetFfxFSRUpscaleProvidersCount"
        );
        SRUpscaleProvider provider = new SRUpscaleProvider(0);
        SuperResolutionNativeAPI.srGetUpscaleProvider(provider, 0x8000006);
        this.context = new SRUpscaleContext(0);
        SRCreateUpscaleContextDesc upscaleContextDesc = new SRCreateUpscaleContextDesc(
                null,
                null,
                new Vector2i(MinecraftRenderHandle.getScreenWidth(), MinecraftRenderHandle.getScreenHeight()),
                new Vector2i(MinecraftRenderHandle.getRenderWidth(), MinecraftRenderHandle.getRenderHeight()),
                0
        );
        SRReturnCode code = SuperResolutionNativeAPI.srCreateUpscaleContext(
                context,
                provider,
                upscaleContextDesc
        );
        SuperResolution.LOGGER.info(String.valueOf(code.value));
        SuperResolution.LOGGER.info(String.valueOf(context.nativePtr));
        SuperResolution.LOGGER.info(String.valueOf(provider.nativePtr));
        return true;
    }

    protected void destroyTexture() {
        if (this.outputColorGlTexture != null) this.outputColorGlTexture.destroy();
        if (this.outputColorFrameBuffer != null) this.outputColorFrameBuffer.destroy();

    }

    protected void createTexture() {
        this.srcInputColorGlTexture = MinecraftRenderHandle.getRenderTarget().getTexture(FrameBufferAttachmentType.Color);
        this.srcInputDepthGlTexture = MinecraftRenderHandle.getRenderTarget().getTexture(FrameBufferAttachmentType.AnyDepth);
        this.srcMotionVectorsGlTexture = AlgorithmManager.getMotionVectorsFrameBuffer().getTexture(FrameBufferAttachmentType.Color);
        this.outputColorGlTexture = RenderSystems.opengl().device().createTexture(
                TextureDescription.create()
                        .type(TextureType.Texture2D)
                        .usages(TextureUsages.create().sampler().storage())
                        .mipmapsDisabled()
                        .filterMode(TextureFilterMode.LINEAR)
                        .format(TextureFormat.RGBA8)
                        .size(MinecraftRenderHandle.getScreenWidth(), MinecraftRenderHandle.getScreenHeight())
                        .label("SRFSR2-Output")
                        .build()
        );
        this.outputColorFrameBuffer = GlFrameBuffer.create(this.outputColorGlTexture, null);
    }

    @Override
    public void init() {
        if (!updateFsr()) {
            throw new RuntimeException();
        }
        createTexture();
    }

    @Override
    public boolean dispatch(DispatchResource dispatchResource) {
        super.dispatch(dispatchResource);

        if (context == null || context.nativePtr < 1) {
            return false;
        }
        SRDispatchUpscaleDesc desc = new SRDispatchUpscaleDesc();
        desc.setCommandList(0);
        desc.setColor(new SRTextureResource(dispatchResource.resources().colorTexture()));
        desc.setDepth(new SRTextureResource(dispatchResource.resources().depthTexture()));
        desc.setMotionVectors(new SRTextureResource(dispatchResource.resources().motionVectorsTexture()));
        desc.setOutput(new SRTextureResource(this.outputColorGlTexture));
        desc.setJitterOffset(new Vector2f(0));
        desc.setMotionVectorScale(new Vector2f(1));
        desc.setRenderSize(
                new Vector2i(
                        dispatchResource.renderWidth(),
                        dispatchResource.renderHeight()
                )
        );
        desc.setUpscaleSize(
                new Vector2i(
                        dispatchResource.screenWidth(),
                        dispatchResource.screenHeight()
                )
        );
        desc.setFrameTimeDelta(dispatchResource.frameTimeDelta());
        desc.setEnableSharpening(true);
        desc.setSharpness(SuperResolutionConfig.getSharpness());
        desc.setPreExposure(1.0f);
        desc.setCameraNear(0.8f);
        desc.setCameraFar(dispatchResource.cameraFar());
        desc.setCameraFovAngleVertical((float) Math.toRadians(dispatchResource.verticalFov()));
        desc.setViewSpaceToMetersFactor(0.0f);
        desc.setReset(false);
        desc.setFlags(0);
        try (GlState state = new GlState()) {
            SRReturnCode code = SuperResolutionNativeAPI.srDispatchUpscale(
                    context,
                    desc
            );
        }
        return true;
    }

    @Override
    public void destroy() {
        destroyTexture();
        if (context != null && context.nativePtr > 0)
            SuperResolutionNativeAPI.srDestroyUpscaleContext(context);
    }

    @Override
    public void resize(int width, int height) {
        updateFsr();
        destroyTexture();
        createTexture();
    }

    @Override
    public int getOutputTextureId() {
        return Math.toIntExact(outputColorGlTexture.handle());
    }

    @Override
    public IFrameBuffer getOutputFrameBuffer() {
        return outputColorFrameBuffer;
    }
}
