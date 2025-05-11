package io.homo.superresolution.fsr2;

import io.homo.superresolution.core.gl.buffer.GlUniformBuffer;
import io.homo.superresolution.fsr2.pipelines.*;
import io.homo.superresolution.fsr2.struct.Fsr2CBFSR2;
import io.homo.superresolution.core.impl.Vec2;

public class Fsr2Context {
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
        resources.inputColor.texture(dispatchDescription.color);
        resources.inputMotionVectors.texture(dispatchDescription.motionVectors);
        resources.inputDepth.texture(dispatchDescription.depth);
        resources.inputExposure.texture(dispatchDescription.exposure);
        resources.inputReactiveMask.texture(dispatchDescription.reactive);
        resources.inputTransparencyAndCompositionMask.texture(dispatchDescription.transparencyAndComposition);
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

/*
    private void swapFrameResources() {
        final boolean isOdd = false;
        resources.lockStatus.texture(isOdd ?
                resources.lockStatus2.texture() :
                resources.lockStatus1.texture());
        resources.lockStatusUav.texture(isOdd ?
                resources.lockStatus1.texture() :
                resources.lockStatus2.texture());
        resources.dilatedMotionVectors.texture(isOdd ?
                resources.internalDilatedMotionVectors2.texture() :
                resources.internalDilatedMotionVectors1.texture());
        resources.previousDilatedMotionVectors.texture(isOdd ?
                resources.internalDilatedMotionVectors1.texture() :
                resources.internalDilatedMotionVectors2.texture());
        resources.lumaHistory.texture(isOdd ?
                resources.lumaHistory2.texture() :
                resources.lumaHistory1.texture());
        resources.lumaHistoryUav.texture(isOdd ?
                resources.lumaHistory1.texture() :
                resources.lumaHistory2.texture());
        resources.prevPreAlphaColor.texture(isOdd ?
                resources.prevPreAlphaColor2.texture() :
                resources.prevPreAlphaColor1.texture());
        resources.prevPostAlphaColor.texture(isOdd ?
                resources.prevPostAlphaColor2.texture() :
                resources.prevPostAlphaColor1.texture());
    }*/
}