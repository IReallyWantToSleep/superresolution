package io.homo.superresolution.fsr2;

import io.homo.superresolution.core.gl.buffer.GlUniformBuffer;
import io.homo.superresolution.fsr2.pipelines.*;
import io.homo.superresolution.fsr2.struct.Fsr2CBFSR2;
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
    public Fsr2CBFSR2 constants = new Fsr2CBFSR2();
    public GlUniformBuffer<Fsr2CBFSR2> constantsUBO = new GlUniformBuffer<>(constants);

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
    }

    public void init() {
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

        resize(this.dimensions);
    }

    public void dispatch(Fsr2DispatchDescription dispatchDescription) {
        resources.resource(Fsr2PipelineResourceType.INPUT_COLOR).setResource(dispatchDescription.color);
        resources.resource(Fsr2PipelineResourceType.INPUT_MOTION_VECTORS).setResource(dispatchDescription.motionVectors);
        resources.resource(Fsr2PipelineResourceType.INPUT_DEPTH).setResource(dispatchDescription.depth);
        resources.resource(Fsr2PipelineResourceType.INPUT_EXPOSURE).setResource(dispatchDescription.exposure);
        resources.resource(Fsr2PipelineResourceType.INPUT_REACTIVE_MASK).setResource(dispatchDescription.reactive);
        resources.resource(Fsr2PipelineResourceType.INPUT_TRANSPARENCY_AND_COMPOSITION_MASK).setResource(dispatchDescription.transparencyAndComposition);
        constants.update(
                this,
                dispatchDescription,
                dimensions
        );
        constantsUBO.update();
        if (dispatchDescription.reset()) {
            frameIndex = 0;
        } else {
            frameIndex++;
        }
    }


    private void swapFrameResources() {
        boolean isOdd = (frameIndex & 1) != 0;
        /*
            const uint32_t lockStatusSrvResourceIndex = isOddFrame ? FFX_FSR2_RESOURCE_IDENTIFIER_LOCK_STATUS_2 : FFX_FSR2_RESOURCE_IDENTIFIER_LOCK_STATUS_1;
            const uint32_t lockStatusUavResourceIndex = isOddFrame ? FFX_FSR2_RESOURCE_IDENTIFIER_LOCK_STATUS_1 : FFX_FSR2_RESOURCE_IDENTIFIER_LOCK_STATUS_2;
            const uint32_t upscaledColorSrvResourceIndex = isOddFrame ? FFX_FSR2_RESOURCE_IDENTIFIER_INTERNAL_UPSCALED_COLOR_2 : FFX_FSR2_RESOURCE_IDENTIFIER_INTERNAL_UPSCALED_COLOR_1;
            const uint32_t upscaledColorUavResourceIndex = isOddFrame ? FFX_FSR2_RESOURCE_IDENTIFIER_INTERNAL_UPSCALED_COLOR_1 : FFX_FSR2_RESOURCE_IDENTIFIER_INTERNAL_UPSCALED_COLOR_2;
            const uint32_t dilatedMotionVectorsResourceIndex = isOddFrame ? FFX_FSR2_RESOURCE_IDENTIFIER_INTERNAL_DILATED_MOTION_VECTORS_2 : FFX_FSR2_RESOURCE_IDENTIFIER_INTERNAL_DILATED_MOTION_VECTORS_1;
            const uint32_t previousDilatedMotionVectorsResourceIndex = isOddFrame ? FFX_FSR2_RESOURCE_IDENTIFIER_INTERNAL_DILATED_MOTION_VECTORS_1 : FFX_FSR2_RESOURCE_IDENTIFIER_INTERNAL_DILATED_MOTION_VECTORS_2;
            const uint32_t lumaHistorySrvResourceIndex = isOddFrame ? FFX_FSR2_RESOURCE_IDENTIFIER_LUMA_HISTORY_2 : FFX_FSR2_RESOURCE_IDENTIFIER_LUMA_HISTORY_1;
            const uint32_t lumaHistoryUavResourceIndex = isOddFrame ? FFX_FSR2_RESOURCE_IDENTIFIER_LUMA_HISTORY_1 : FFX_FSR2_RESOURCE_IDENTIFIER_LUMA_HISTORY_2;

            const uint32_t prevPreAlphaColorSrvResourceIndex = isOddFrame ? FFX_FSR2_RESOURCE_IDENTIFIER_PREV_PRE_ALPHA_COLOR_2 : FFX_FSR2_RESOURCE_IDENTIFIER_PREV_PRE_ALPHA_COLOR_1;
            const uint32_t prevPreAlphaColorUavResourceIndex = isOddFrame ? FFX_FSR2_RESOURCE_IDENTIFIER_PREV_PRE_ALPHA_COLOR_1 : FFX_FSR2_RESOURCE_IDENTIFIER_PREV_PRE_ALPHA_COLOR_2;
            const uint32_t prevPostAlphaColorSrvResourceIndex = isOddFrame ? FFX_FSR2_RESOURCE_IDENTIFIER_PREV_POST_ALPHA_COLOR_2 : FFX_FSR2_RESOURCE_IDENTIFIER_PREV_POST_ALPHA_COLOR_1;
            const uint32_t prevPostAlphaColorUavResourceIndex = isOddFrame ? FFX_FSR2_RESOURCE_IDENTIFIER_PREV_POST_ALPHA_COLOR_1 : FFX_FSR2_RESOURCE_IDENTIFIER_PREV_POST_ALPHA_COLOR_2;
         */

        Fsr2PipelineResourceType lockStatusSrvResourceIndex = isOdd ? Fsr2PipelineResourceType.LOCK_STATUS_2 : Fsr2PipelineResourceType.LOCK_STATUS_1;
        Fsr2PipelineResourceType lockStatusUavResourceIndex = isOdd ? Fsr2PipelineResourceType.LOCK_STATUS_1 : Fsr2PipelineResourceType.LOCK_STATUS_2;
        Fsr2PipelineResourceType upscaledColorSrvResourceIndex = isOdd ? Fsr2PipelineResourceType.INTERNAL_UPSCALED_COLOR_2 : Fsr2PipelineResourceType.INTERNAL_UPSCALED_COLOR_1;
        Fsr2PipelineResourceType upscaledColorUavResourceIndex = isOdd ? Fsr2PipelineResourceType.INTERNAL_UPSCALED_COLOR_1 : Fsr2PipelineResourceType.INTERNAL_UPSCALED_COLOR_2;
        Fsr2PipelineResourceType dilatedMotionVectorsResourceIndex = isOdd ? Fsr2PipelineResourceType.INTERNAL_DILATED_MOTION_VECTORS_2 : Fsr2PipelineResourceType.INTERNAL_DILATED_MOTION_VECTORS_1;
        Fsr2PipelineResourceType previousDilatedMotionVectorsResourceIndex = isOdd ? Fsr2PipelineResourceType.INTERNAL_DILATED_MOTION_VECTORS_1 : Fsr2PipelineResourceType.INTERNAL_DILATED_MOTION_VECTORS_2;
        Fsr2PipelineResourceType lumaHistorySrvResourceIndex = isOdd ? Fsr2PipelineResourceType.LUMA_HISTORY_2 : Fsr2PipelineResourceType.LUMA_HISTORY_1;
        Fsr2PipelineResourceType lumaHistoryUavResourceIndex = isOdd ? Fsr2PipelineResourceType.LUMA_HISTORY_1 : Fsr2PipelineResourceType.LUMA_HISTORY_2;
        Fsr2PipelineResourceType prevPreAlphaColorSrvResourceIndex = isOdd ? Fsr2PipelineResourceType.PREV_PRE_ALPHA_COLOR_2 : Fsr2PipelineResourceType.PREV_PRE_ALPHA_COLOR_1;
        Fsr2PipelineResourceType prevPreAlphaColorUavResourceIndex = isOdd ? Fsr2PipelineResourceType.PREV_PRE_ALPHA_COLOR_1 : Fsr2PipelineResourceType.PREV_PRE_ALPHA_COLOR_2;
        Fsr2PipelineResourceType prevPostAlphaColorSrvResourceIndex = isOdd ? Fsr2PipelineResourceType.PREV_POST_ALPHA_COLOR_2 : Fsr2PipelineResourceType.PREV_POST_ALPHA_COLOR_1;
        Fsr2PipelineResourceType prevPostAlphaColorUavResourceIndex = isOdd ? Fsr2PipelineResourceType.PREV_POST_ALPHA_COLOR_1 : Fsr2PipelineResourceType.PREV_POST_ALPHA_COLOR_2;

        /*
            context->srvResources[FFX_FSR2_RESOURCE_IDENTIFIER_LOCK_STATUS] = context->srvResources[lockStatusSrvResourceIndex];
            context->srvResources[FFX_FSR2_RESOURCE_IDENTIFIER_INTERNAL_UPSCALED_COLOR] = context->srvResources[upscaledColorSrvResourceIndex];
            context->uavResources[FFX_FSR2_RESOURCE_IDENTIFIER_LOCK_STATUS] = context->uavResources[lockStatusUavResourceIndex];
            context->uavResources[FFX_FSR2_RESOURCE_IDENTIFIER_INTERNAL_UPSCALED_COLOR] = context->uavResources[upscaledColorUavResourceIndex];
            context->srvResources[FFX_FSR2_RESOURCE_IDENTIFIER_RCAS_INPUT] = context->uavResources[upscaledColorUavResourceIndex];

            context->srvResources[FFX_FSR2_RESOURCE_IDENTIFIER_DILATED_MOTION_VECTORS] = context->srvResources[dilatedMotionVectorsResourceIndex];
            context->uavResources[FFX_FSR2_RESOURCE_IDENTIFIER_DILATED_MOTION_VECTORS] = context->uavResources[dilatedMotionVectorsResourceIndex];
            context->srvResources[FFX_FSR2_RESOURCE_IDENTIFIER_PREVIOUS_DILATED_MOTION_VECTORS] = context->srvResources[previousDilatedMotionVectorsResourceIndex];

            context->uavResources[FFX_FSR2_RESOURCE_IDENTIFIER_LUMA_HISTORY] = context->uavResources[lumaHistoryUavResourceIndex];
            context->srvResources[FFX_FSR2_RESOURCE_IDENTIFIER_LUMA_HISTORY] = context->srvResources[lumaHistorySrvResourceIndex];

            context->srvResources[FFX_FSR2_RESOURCE_IDENTIFIER_PREV_PRE_ALPHA_COLOR] = context->srvResources[prevPreAlphaColorSrvResourceIndex];
            context->uavResources[FFX_FSR2_RESOURCE_IDENTIFIER_PREV_PRE_ALPHA_COLOR] = context->uavResources[prevPreAlphaColorUavResourceIndex];
            context->srvResources[FFX_FSR2_RESOURCE_IDENTIFIER_PREV_POST_ALPHA_COLOR] = context->srvResources[prevPostAlphaColorSrvResourceIndex];
            context->uavResources[FFX_FSR2_RESOURCE_IDENTIFIER_PREV_POST_ALPHA_COLOR] = context->uavResources[prevPostAlphaColorUavResourceIndex];

         */

        resources.resource(Fsr2PipelineResourceType.LOCK_STATUS).setResource(
                resources.resource(lockStatusSrvResourceIndex).getResource()
        );
        resources.resource(Fsr2PipelineResourceType.LOCK_STATUS).setResource(
                resources.resource(lockStatusUavResourceIndex).getResource()
        );

        resources.resource(Fsr2PipelineResourceType.INTERNAL_UPSCALED_COLOR).setResource(
                resources.resource(upscaledColorSrvResourceIndex).getResource()
        );
        resources.resource(Fsr2PipelineResourceType.INTERNAL_UPSCALED_COLOR).setResource(
                resources.resource(upscaledColorUavResourceIndex).getResource()
        );

        resources.resource(Fsr2PipelineResourceType.RCAS_INPUT).setResource(
                resources.resource(upscaledColorUavResourceIndex).getResource()
        );

        resources.resource(Fsr2PipelineResourceType.DILATED_MOTION_VECTORS).setResource(
                resources.resource(dilatedMotionVectorsResourceIndex).getResource()
        );
        resources.resource(Fsr2PipelineResourceType.DILATED_MOTION_VECTORS).setResource(
                resources.resource(dilatedMotionVectorsResourceIndex).getResource()
        );
        resources.resource(Fsr2PipelineResourceType.PREVIOUS_DILATED_MOTION_VECTORS).setResource(
                resources.resource(previousDilatedMotionVectorsResourceIndex).getResource()
        );
        resources.resource(Fsr2PipelineResourceType.LUMA_HISTORY).setResource(
                resources.resource(lumaHistorySrvResourceIndex).getResource()
        );
        resources.resource(Fsr2PipelineResourceType.LUMA_HISTORY).setResource(
                resources.resource(lumaHistoryUavResourceIndex).getResource()
        );

        resources.resource(Fsr2PipelineResourceType.PREV_PRE_ALPHA_COLOR).setResource(
                resources.resource(prevPreAlphaColorSrvResourceIndex).getResource()
        );
        resources.resource(Fsr2PipelineResourceType.PREV_PRE_ALPHA_COLOR).setResource(
                resources.resource(prevPreAlphaColorUavResourceIndex).getResource()
        );
        resources.resource(Fsr2PipelineResourceType.PREV_POST_ALPHA_COLOR).setResource(
                resources.resource(prevPostAlphaColorSrvResourceIndex).getResource()
        );
        resources.resource(Fsr2PipelineResourceType.PREV_POST_ALPHA_COLOR).setResource(
                resources.resource(prevPostAlphaColorUavResourceIndex).getResource()
        );
    }
}