package io.homo.superresolution.fsr2.v221.pipelines;

import io.homo.superresolution.core.graphics.GraphicsCapabilities;
import io.homo.superresolution.core.graphics.opengl.pipeline.GlPipeline;
import io.homo.superresolution.fsr2.v221.Fsr2Context;
import io.homo.superresolution.fsr2.v221.Fsr2Dimensions;
import io.homo.superresolution.fsr2.v221.Fsr2PipelineDispatchResource;
import io.homo.superresolution.fsr2.v221.Fsr2PipelineResources;

import java.util.HashMap;
import java.util.Map;

public abstract class Fsr2BasePipeline {
    protected final Fsr2Context context;
    public GlPipeline pipeline = new GlPipeline();

    public Fsr2BasePipeline(Fsr2Context context) {
        this.context = context;
    }


    public abstract void resize(Fsr2Dimensions size);

    public abstract void destroy();

    public abstract void init();

    protected Fsr2PipelineResources.Fsr2ResourceEntry getResourcesDescription(String name) {
        if (!context.resources.shaderNameMap().containsKey(name)) throw new RuntimeException(name);
        return context.resources.resource(context.resources.shaderNameMap().get(name));
    }

    private String bool(boolean b) {
        return String.valueOf(b ? 1 : 0);
    }

    public Map<String, String> getShaderDefines(Map<String, String> override) {
        Map<String, String> defines = new HashMap<>();
        defines.put("FFX_FSR2_OPTION_HDR_COLOR_INPUT", bool(context.config.flags.isEnableHighDynamicRange()));
        defines.put("FFX_FSR2_OPTION_LOW_RESOLUTION_MOTION_VECTORS", bool(!context.config.flags.isEnableDisplayResolutionMotionVectors()));
        defines.put("FFX_FSR2_OPTION_JITTERED_MOTION_VECTORS", bool(context.config.flags.isEnableMotionVectorsJitterCancellation()));
        defines.put("FFX_FSR2_OPTION_INVERTED_DEPTH", bool(context.config.flags.isEnableDepthInverted()));

        defines.put("FFX_FSR2_OPTION_APPLY_SHARPENING", bool(false));
        defines.put("FFX_FSR2_OPTION_REPROJECT_USE_LANCZOS_TYPE", bool(false));

        defines.put("FFX_FSR2_OPTION_UPSAMPLE_SAMPLERS_USE_DATA_HALF", "0");
        defines.put("FFX_FSR2_OPTION_ACCUMULATE_SAMPLERS_USE_DATA_HALF", "0");
        defines.put("FFX_FSR2_OPTION_REPROJECT_SAMPLERS_USE_DATA_HALF", "1");
        defines.put("FFX_FSR2_OPTION_POSTPROCESSLOCKSTATUS_SAMPLERS_USE_DATA_HALF", "0");
        defines.put("FFX_FSR2_OPTION_UPSAMPLE_USE_LANCZOS_TYPE", "2");
        defines.put("FFX_GLSL", bool(true));
        defines.put("FFX_GPU", bool(true));
        defines.put("FFX_HALF", bool(GraphicsCapabilities.isFp16Supported()));
        if (override != null) defines.putAll(override);
        return defines;
    }

    public abstract void execute(Fsr2PipelineDispatchResource dispatchResource);
}

