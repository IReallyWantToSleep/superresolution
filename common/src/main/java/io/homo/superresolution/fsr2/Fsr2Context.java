package io.homo.superresolution.fsr2;

import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.impl.buffer.BufferDescription;
import io.homo.superresolution.core.graphics.impl.buffer.BufferUsage;
import io.homo.superresolution.core.graphics.opengl.buffer.GlBuffer;
import io.homo.superresolution.core.graphics.opengl.buffer.GlUniformBuffer;
import io.homo.superresolution.fsr2.pipelines.*;
import io.homo.superresolution.fsr2.struct.Fsr2CBFSR2;
import io.homo.superresolution.fsr2.struct.Fsr2CBRcas;
import io.homo.superresolution.fsr2.struct.Fsr2CBSpd;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Fsr2Context {
    public static final Logger LOGGER = LoggerFactory.getLogger("SuperResolution-FSR2");

    //////////////////////
    public Fsr2AccumulatePipeline accumulatePipeline;
    public Fsr2RCASPipeline rcasPipeline;
    public Fsr2AccumulateSharpenPipeline accumulateSharpenPipeline;
    public Fsr2ComputeLuminancePyramidPipeline computeLuminancePyramidPipeline;
    public Fsr2DepthClipPipeline depthClipPipeline;
    public Fsr2GenerateReactivePipeline generateReactivePipeline;
    public Fsr2LockPipeline lockPipeline;
    public Fsr2ReconstructPreviousDepthPipeline reconstructPreviousDepthPipeline;
    public Fsr2TcrAutogeneratePipeline tcrAutogeneratePipeline;
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
        accumulatePipeline = new Fsr2AccumulatePipeline(this);
        rcasPipeline = new Fsr2RCASPipeline(this);
        accumulateSharpenPipeline = new Fsr2AccumulateSharpenPipeline(this);
        computeLuminancePyramidPipeline = new Fsr2ComputeLuminancePyramidPipeline(this);
        depthClipPipeline = new Fsr2DepthClipPipeline(this);
        generateReactivePipeline = new Fsr2GenerateReactivePipeline(this);
        lockPipeline = new Fsr2LockPipeline(this);
        reconstructPreviousDepthPipeline = new Fsr2ReconstructPreviousDepthPipeline(this);
        tcrAutogeneratePipeline = new Fsr2TcrAutogeneratePipeline(this);

        accumulatePipeline.init();
        rcasPipeline.init();
        accumulateSharpenPipeline.init();
        computeLuminancePyramidPipeline.init();
        depthClipPipeline.init();
        generateReactivePipeline.init();
        lockPipeline.init();
        reconstructPreviousDepthPipeline.init();
        tcrAutogeneratePipeline.init();

        accumulatePipeline.resize(dimensions);
        rcasPipeline.resize(dimensions);
        accumulateSharpenPipeline.resize(dimensions);
        computeLuminancePyramidPipeline.resize(dimensions);
        depthClipPipeline.resize(dimensions);
        generateReactivePipeline.resize(dimensions);
        lockPipeline.resize(dimensions);
        reconstructPreviousDepthPipeline.resize(dimensions);
        tcrAutogeneratePipeline.resize(dimensions);
    }

    public void destroy() {
        resources.destroy();
        accumulatePipeline.destroy();
        rcasPipeline.destroy();
        accumulateSharpenPipeline.destroy();
        computeLuminancePyramidPipeline.destroy();
        depthClipPipeline.destroy();
        generateReactivePipeline.destroy();
        lockPipeline.destroy();
        reconstructPreviousDepthPipeline.destroy();
        tcrAutogeneratePipeline.destroy();

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

        resize(this.dimensions);
    }

    public void dispatch(Fsr2DispatchDescription dispatchDescription) {
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
                dispatchDescription
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