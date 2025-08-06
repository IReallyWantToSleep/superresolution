package io.homo.superresolution.thirdparty.fsr2.common;

import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.impl.buffer.BufferDescription;
import io.homo.superresolution.core.graphics.impl.buffer.BufferUsage;
import io.homo.superresolution.core.graphics.opengl.Gl;
import io.homo.superresolution.core.graphics.opengl.buffer.GlBuffer;
import io.homo.superresolution.core.graphics.opengl.texture.GlTexture2D;
import io.homo.superresolution.thirdparty.fsr2.common.struct.Fsr2CBFSR2;
import io.homo.superresolution.thirdparty.fsr2.common.struct.Fsr2CBRcas;
import io.homo.superresolution.thirdparty.fsr2.common.struct.Fsr2CBSpd;
import io.homo.superresolution.thirdparty.fsr2.v221.*;
import io.homo.superresolution.thirdparty.fsr2.v233.*;
import org.lwjgl.opengl.GL41;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glTexSubImage2D;

public class Fsr2Context {
    public static final Logger LOGGER = LoggerFactory.getLogger("SuperResolution-FSR2");

    //////////////////////
    public Fsr2Pipeline accumulatePipeline;
    public Fsr2Pipeline rcasPipeline;
    public Fsr2Pipeline accumulateSharpenPipeline;
    public Fsr2Pipeline computeLuminancePyramidPipeline;
    public Fsr2Pipeline depthClipPipeline;
    public Fsr2Pipeline lockPipeline;
    public Fsr2Pipeline reconstructPreviousDepthPipeline;
    //////////////////////

    public Fsr2PipelineResources resources = new Fsr2PipelineResources();
    public Fsr2ContextConfig config;
    public Fsr2Dimensions dimensions;
    public Fsr2CBFSR2 fsr2Constants = new Fsr2CBFSR2();
    public GlBuffer fsr2ConstantsUBO;
    public Fsr2CBSpd fsr2SpdConstants = new Fsr2CBSpd();
    public GlBuffer fsr2SpdConstantsUBO;
    public Fsr2CBRcas fsr2RcasConstants = new Fsr2CBRcas();
    public GlBuffer fsr2RcasConstantsUBO;
    private int frameIndex = 0;
    private boolean maximumBiasTextureUploaded = false;

    public Fsr2Context(
            Fsr2ContextConfig config,
            Fsr2Dimensions dimensions
    ) {
        this.config = config;
        this.dimensions = dimensions;
    }

    public void resize(Fsr2Dimensions dimensions) {
        this.dimensions = dimensions;
        resources.destroy();
        resources = null;
        resources = new Fsr2PipelineResources();
        resources.init(
                dimensions.renderWidth(),
                dimensions.renderHeight(),
                dimensions.screenWidth(),
                dimensions.screenHeight()
        );

        accumulatePipeline.resize(dimensions);
        rcasPipeline.resize(dimensions);
        accumulateSharpenPipeline.resize(dimensions);
        computeLuminancePyramidPipeline.resize(dimensions);
        depthClipPipeline.resize(dimensions);
        lockPipeline.resize(dimensions);
        reconstructPreviousDepthPipeline.resize(dimensions);
        maximumBiasTextureUploaded = false;
    }

    public void destroy() {
        resources.destroy();
        accumulatePipeline.destroy();
        rcasPipeline.destroy();
        accumulateSharpenPipeline.destroy();
        computeLuminancePyramidPipeline.destroy();
        depthClipPipeline.destroy();
        lockPipeline.destroy();
        reconstructPreviousDepthPipeline.destroy();

        fsr2Constants.free();
        fsr2SpdConstants.free();
        fsr2RcasConstants.free();
        fsr2ConstantsUBO.destroy();
        fsr2SpdConstantsUBO.destroy();
        fsr2RcasConstantsUBO.destroy();
    }

    public void init() {
        this.fsr2RcasConstantsUBO = RenderSystems.current().device().createBuffer(
                BufferDescription.create()
                        .size(this.fsr2RcasConstants.size())
                        .usage(BufferUsage.UBO)
                        .build()
        );
        this.fsr2SpdConstantsUBO = RenderSystems.current().device().createBuffer(
                BufferDescription.create()
                        .size(this.fsr2SpdConstants.size())
                        .usage(BufferUsage.UBO)
                        .build()
        );
        this.fsr2ConstantsUBO = RenderSystems.current().device().createBuffer(
                BufferDescription.create()
                        .size(this.fsr2Constants.size())
                        .usage(BufferUsage.UBO)
                        .build()
        );
        this.fsr2SpdConstantsUBO.setBufferData(this.fsr2SpdConstants);
        this.fsr2ConstantsUBO.setBufferData(this.fsr2Constants);
        this.fsr2RcasConstantsUBO.setBufferData(this.fsr2RcasConstants);

        resources = new Fsr2PipelineResources();
        resources.init(
                dimensions.renderWidth(),
                dimensions.renderHeight(),
                dimensions.screenWidth(),
                dimensions.screenHeight()
        );

        switch (config.getVersion()) {
            case V221 -> {
                accumulatePipeline = new Fsr2v221AccumulatePipeline(this);
                rcasPipeline = new Fsr2v221RCASPipeline(this);
                accumulateSharpenPipeline = new Fsr2v221AccumulateSharpenPipeline(this);
                computeLuminancePyramidPipeline = new Fsr2v221ComputeLuminancePyramidPipeline(this);
                depthClipPipeline = new Fsr2v221DepthClipPipeline(this);
                lockPipeline = new Fsr2v221LockPipeline(this);
                reconstructPreviousDepthPipeline = new Fsr2v221ReconstructPreviousDepthPipeline(this);

            }
            case V233 -> {
                accumulatePipeline = new Fsr2v233AccumulatePipeline(this);
                rcasPipeline = new Fsr2v233RCASPipeline(this);
                accumulateSharpenPipeline = new Fsr2v233AccumulateSharpenPipeline(this);
                computeLuminancePyramidPipeline = new Fsr2v233ComputeLuminancePyramidPipeline(this);
                depthClipPipeline = new Fsr2v233DepthClipPipeline(this);
                lockPipeline = new Fsr2v233LockPipeline(this);
                reconstructPreviousDepthPipeline = new Fsr2v233ReconstructPreviousDepthPipeline(this);
            }
        }
        accumulatePipeline.init();
        rcasPipeline.init();
        accumulateSharpenPipeline.init();
        computeLuminancePyramidPipeline.init();
        depthClipPipeline.init();
        lockPipeline.init();
        reconstructPreviousDepthPipeline.init();

        resize(this.dimensions);
        maximumBiasTextureUploaded = false;
    }
    

    public void dispatch(Fsr2DispatchDescription dispatchDescription) {
        if (!maximumBiasTextureUploaded) {
            GlTexture2D maximumBiasTexture = ((GlTexture2D) resources.resource(Fsr2PipelineResourceType.UPSAMPLE_MAXIMUM_BIAS_LUT).getResource());
            if (maximumBiasTexture != null) {
                int textureSize = Fsr2MaximumBias.FFX_FSR2_MAXIMUM_BIAS_TEXTURE_WIDTH * Fsr2MaximumBias.FFX_FSR2_MAXIMUM_BIAS_TEXTURE_HEIGHT;
                short[] data = new short[textureSize];
                for (int dataIndex = 0; dataIndex < data.length; dataIndex++) {
                    short converted = (short) Math.round(Fsr2MaximumBias.ffxFsr2MaximumBiasData[dataIndex] / 2.0f * 32767.0f);
                    data[dataIndex] = converted;
                }
                int prevTex = glGetInteger(GL_TEXTURE_BINDING_2D);
                glBindTexture(GL_TEXTURE_2D, (int) maximumBiasTexture.handle());
                glTexSubImage2D(
                        GL_TEXTURE_2D,
                        0,
                        0,
                        0,
                        maximumBiasTexture.getWidth(),
                        maximumBiasTexture.getHeight(),
                        GL41.GL_RED,
                        GL41.GL_SHORT,
                        data
                );
                glBindTexture(GL_TEXTURE_2D, prevTex);
                maximumBiasTextureUploaded = true;
            }
        }

        resources.resource(Fsr2PipelineResourceType.INPUT_COLOR).setResource(dispatchDescription.color);
        resources.resource(Fsr2PipelineResourceType.INPUT_MOTION_VECTORS).setResource(dispatchDescription.motionVectors);
        resources.resource(Fsr2PipelineResourceType.INPUT_DEPTH).setResource(dispatchDescription.depth);
        resources.resource(Fsr2PipelineResourceType.INPUT_EXPOSURE).setResource(
                dispatchDescription.exposure == null ?
                        resources.resource(Fsr2PipelineResourceType.INTERNAL_DEFAULT_EXPOSURE).getResource() :
                        dispatchDescription.exposure
        );
        resources.resource(Fsr2PipelineResourceType.INPUT_REACTIVE_MASK).setResource(
                dispatchDescription.reactive == null ?
                        resources.resource(Fsr2PipelineResourceType.INTERNAL_DEFAULT_REACTIVITY).getResource() :
                        dispatchDescription.reactive
        );
        resources.resource(Fsr2PipelineResourceType.INPUT_TRANSPARENCY_AND_COMPOSITION_MASK).setResource(
                dispatchDescription.transparencyAndComposition == null ?
                        resources.resource(Fsr2PipelineResourceType.INTERNAL_DEFAULT_REACTIVITY).getResource() : //fsr2原版用的也是INTERNAL_DEFAULT_REACTIVITY
                        dispatchDescription.transparencyAndComposition
        );
        resources.resource(Fsr2PipelineResourceType.UPSCALED_OUTPUT).setResource(dispatchDescription.output);

        fsr2Constants.update(this, dispatchDescription, dimensions);
        fsr2SpdConstants.update(this, dispatchDescription, dimensions);
        fsr2RcasConstants.update(this, dispatchDescription, dimensions);
        fsr2ConstantsUBO.upload();
        fsr2SpdConstantsUBO.upload();
        fsr2RcasConstantsUBO.upload();

        Fsr2PipelineDispatchResource pipelineDispatchResource = new Fsr2PipelineDispatchResource(
                resources,
                config,
                dimensions,
                dispatchDescription,
                dispatchDescription.commandBuffer
        );

        computeLuminancePyramidPipeline.execute(pipelineDispatchResource);
        reconstructPreviousDepthPipeline.execute(pipelineDispatchResource);
        depthClipPipeline.execute(pipelineDispatchResource);
        lockPipeline.execute(pipelineDispatchResource);
        if (dispatchDescription.enableSharpening()) {
            accumulateSharpenPipeline.execute(pipelineDispatchResource);
            rcasPipeline.execute(pipelineDispatchResource);
        } else {
            accumulatePipeline.execute(pipelineDispatchResource);
        }

        if (dispatchDescription.reset()) {
            frameIndex = 0;
        } else {
            frameIndex++;
            frameIndex = frameIndex % 16;
        }
    }

    public boolean isOddFrame() {
        return (frameIndex & 1) != 0;
    }
}