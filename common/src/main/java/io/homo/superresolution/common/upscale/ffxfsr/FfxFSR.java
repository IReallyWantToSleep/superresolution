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
import io.homo.superresolution.core.graphics.impl.pipeline.Pipeline;
import io.homo.superresolution.core.graphics.impl.pipeline.PipelineJobBuilders;
import io.homo.superresolution.core.graphics.impl.pipeline.PipelineJobResource;
import io.homo.superresolution.core.graphics.impl.shader.ShaderDescription;
import io.homo.superresolution.core.graphics.impl.shader.ShaderSource;
import io.homo.superresolution.core.graphics.impl.shader.ShaderType;
import io.homo.superresolution.core.graphics.impl.texture.*;
import io.homo.superresolution.core.graphics.opengl.framebuffer.GlFrameBuffer;
import io.homo.superresolution.core.graphics.opengl.shader.GlShaderProgram;
import io.homo.superresolution.core.graphics.opengl.texture.GlImportableTexture2D;
import io.homo.superresolution.core.graphics.opengl.texture.GlTexture2D;
import io.homo.superresolution.core.graphics.vulkan.VulkanDevice;
import io.homo.superresolution.core.graphics.vulkan.command.VulkanCommandBuffer;
import io.homo.superresolution.core.graphics.vulkan.texture.VulkanTexture;
import io.homo.superresolution.core.math.Vector2f;
import io.homo.superresolution.core.math.Vector2i;
import io.homo.superresolution.srapi.*;
import net.minecraft.client.Minecraft;

import java.nio.file.Path;
import java.util.Optional;

public class FfxFSR extends AbstractAlgorithm {
    private SRUpscaleContext context;
    private GlShaderProgram copyShader;
    private GlFrameBuffer copyFramebuffer;

    private Pipeline copyPipeline;

    private ITexture srcInputColorGlTexture;
    private ITexture srcInputDepthGlTexture;
    private ITexture srcMotionVectorsGlTexture;


    private GlImportableTexture2D inputColorGlTexture;
    private VulkanTexture inputColorVkTexture;

    private GlImportableTexture2D inputDepthGlTexture;
    private VulkanTexture inputDepthVkTexture;

    private GlImportableTexture2D inputMotionVectorsGlTexture;
    private VulkanTexture inputMotionVectorsVkTexture;

    private GlImportableTexture2D outputColorGlTexture;
    private VulkanTexture outputColorVkTexture;

    private GlFrameBuffer outputFrameBuffer;

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
                ((VulkanDevice) RenderSystems.vulkan().device()).getVkDevice(),
                ((VulkanDevice) RenderSystems.vulkan().device()).getPhysicalDevice(),
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

    protected void destroySharedTexture() {
        if (this.inputColorVkTexture != null) this.inputColorVkTexture.destroy();
        if (this.inputColorGlTexture != null) this.inputColorGlTexture.destroy();
        if (this.inputDepthVkTexture != null) this.inputDepthVkTexture.destroy();
        if (this.inputDepthGlTexture != null) this.inputDepthGlTexture.destroy();
        if (this.outputColorVkTexture != null) this.outputColorVkTexture.destroy();
        if (this.outputFrameBuffer != null) this.outputFrameBuffer.destroy();
        if (this.copyFramebuffer != null) this.copyFramebuffer.destroy();

    }

    protected void createSharedTexture() {
        this.srcInputColorGlTexture = MinecraftRenderHandle.getRenderTarget().getTexture(FrameBufferAttachmentType.Color);
        this.inputColorVkTexture = new VulkanTexture(
                (VulkanDevice) RenderSystems.vulkan().device(),
                TextureDescription.create()
                        .usages(TextureUsages.create().sampler())
                        .format(this.srcInputColorGlTexture.getTextureFormat())
                        .type(this.srcInputColorGlTexture.getTextureType())
                        .width(this.srcInputColorGlTexture.getWidth())
                        .height(this.srcInputColorGlTexture.getHeight())
                        .build(),
                false,
                0,
                true
        );
        this.inputColorGlTexture = new GlImportableTexture2D(this.inputColorVkTexture);

        this.srcInputDepthGlTexture = MinecraftRenderHandle.getRenderTarget().getTexture(FrameBufferAttachmentType.AnyDepth);
        this.inputDepthVkTexture = new VulkanTexture(
                (VulkanDevice) RenderSystems.vulkan().device(),
                TextureDescription.create()
                        .usages(TextureUsages.create().sampler())
                        .format(TextureFormat.R16F)
                        .type(this.srcInputDepthGlTexture.getTextureType())
                        .width(this.srcInputDepthGlTexture.getWidth())
                        .height(this.srcInputDepthGlTexture.getHeight())
                        .build(),
                false,
                0,
                true
        );
        this.inputDepthGlTexture = new GlImportableTexture2D(this.inputColorVkTexture);

        this.srcMotionVectorsGlTexture = AlgorithmManager.getMotionVectorsFrameBuffer().getTexture(FrameBufferAttachmentType.Color);
        this.inputMotionVectorsVkTexture = new VulkanTexture(
                (VulkanDevice) RenderSystems.vulkan().device(),
                TextureDescription.create()
                        .usages(TextureUsages.create().sampler())
                        .format(TextureFormat.RG16F)
                        .type(this.srcMotionVectorsGlTexture.getTextureType())
                        .width(this.srcMotionVectorsGlTexture.getWidth())
                        .height(this.srcMotionVectorsGlTexture.getHeight())
                        .build(),
                false,
                0,
                true
        );
        this.inputMotionVectorsGlTexture = new GlImportableTexture2D(this.inputMotionVectorsVkTexture);


        this.outputColorVkTexture = new VulkanTexture(
                (VulkanDevice) RenderSystems.vulkan().device(),
                TextureDescription.create()
                        .type(TextureType.Texture2D)
                        .usages(TextureUsages.create().sampler().storage())
                        .format(TextureFormat.RGBA8)
                        .width(MinecraftRenderHandle.getScreenWidth())
                        .height(MinecraftRenderHandle.getScreenHeight())
                        .build(),
                false,
                0,
                true
        );
        this.outputColorGlTexture = new GlImportableTexture2D(this.inputColorVkTexture);
        this.outputFrameBuffer = GlFrameBuffer.create(this.outputColorGlTexture, null);
        copyFramebuffer = GlFrameBuffer.create(
                this.inputDepthGlTexture,
                null
        );
    }

    @Override
    public void init() {
        if (!updateFsr()) {
            throw new RuntimeException();
        }
        createSharedTexture();
        copyShader = RenderSystems.current().device().createShaderProgram(
                ShaderDescription.graphics(
                                new ShaderSource(ShaderType.FRAGMENT, "/shader/depth_to_r16f.frag.glsl", true),
                                new ShaderSource(ShaderType.VERTEX, "/shader/depth_to_r16f.vert.glsl", true)
                        )
                        .name("ffxfsr_copy_depth_to_color")
                        .uniformSamplerTexture("tex", 0)
                        .build()
        );
        copyShader.compile();
        copyPipeline = new Pipeline();
        copyPipeline.job("copy",
                PipelineJobBuilders.graphics(copyShader)
                        .resource(
                                "tex",
                                PipelineJobResource.SamplerTexture.create(
                                        () -> Optional.of(this.inputDepthGlTexture)
                                )
                        )
                        .targetFramebuffer(copyFramebuffer)
                        .build()
        );
    }

    @Override
    public boolean dispatch(DispatchResource dispatchResource) {
        super.dispatch(dispatchResource);

        if (context == null || context.nativePtr < 1) {
            return false;
        }
        RenderSystems.opengl().device().commendEncoder().begin();
        RenderSystems.opengl().device().commendEncoder().copyTexture(
                this.srcInputColorGlTexture,
                this.inputColorGlTexture
        );
        RenderSystems.opengl().device().commendEncoder().copyTexture(
                this.srcMotionVectorsGlTexture,
                this.inputMotionVectorsGlTexture
        );
        //copyPipeline.execute(RenderSystems.opengl().device().commendEncoder().getCommandBuffer());
        RenderSystems.opengl().device().commendEncoder().end().submit(RenderSystems.opengl().device());
        RenderSystems.opengl().finish();

        RenderSystems.vulkan().device().commendEncoder().begin();
        VulkanCommandBuffer commandBuffer = (VulkanCommandBuffer) RenderSystems.vulkan().device().commendEncoder().getCommandBuffer();
        SRDispatchUpscaleDesc desc = new SRDispatchUpscaleDesc();
        desc.setCommandList(commandBuffer.getNativeCommandBuffer().address());
        desc.setColor(new SRTextureResource(this.inputColorVkTexture));
        desc.setDepth(new SRTextureResource(this.inputDepthVkTexture));
        desc.setMotionVectors(new SRTextureResource(this.inputMotionVectorsVkTexture));
        desc.setOutput(new SRTextureResource(this.outputColorVkTexture));
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
        desc.setCameraNear(dispatchResource.cameraNear());
        desc.setCameraFar(dispatchResource.cameraFar());
        desc.setCameraFovAngleVertical((float) Math.toRadians(dispatchResource.verticalFov()));
        desc.setViewSpaceToMetersFactor(0.0f);
        desc.setReset(false);
        desc.setFlags(0);

        SRReturnCode code = SuperResolutionNativeAPI.srDispatchUpscale(
                context,
                desc
        );

        RenderSystems.vulkan().device().commendEncoder().end();
        commandBuffer.submit(RenderSystems.vulkan().device());
        RenderSystems.vulkan().finish();
        return true;
    }

    @Override
    public void destroy() {
        destroySharedTexture();
        if (context != null && context.nativePtr > 0)
            SuperResolutionNativeAPI.srDestroyUpscaleContext(context);
    }

    @Override
    public void resize(int width, int height) {
        updateFsr();
        destroySharedTexture();
        createSharedTexture();
    }

    @Override
    public int getOutputTextureId() {
        return Math.toIntExact(outputColorGlTexture.handle());
    }

    @Override
    public IFrameBuffer getOutputFrameBuffer() {
        return outputFrameBuffer;
    }
}
