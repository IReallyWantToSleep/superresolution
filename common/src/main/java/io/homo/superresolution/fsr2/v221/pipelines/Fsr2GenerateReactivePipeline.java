package io.homo.superresolution.fsr2.v221.pipelines;

import io.homo.superresolution.fsr2.v221.Fsr2Context;
import io.homo.superresolution.fsr2.v221.Fsr2Dimensions;
import io.homo.superresolution.fsr2.v221.Fsr2PipelineDispatchResource;

public class Fsr2GenerateReactivePipeline extends Fsr2BasePipeline {


    public Fsr2GenerateReactivePipeline(Fsr2Context context) {
        super(context);
    }

    @Override
    public void resize(Fsr2Dimensions size) {

    }

    @Override
    public void destroy() {

    }

    @Override
    public void init() {

    }

    @Override
    public void execute(Fsr2PipelineDispatchResource dispatchResource) {
        pipeline.scheduleJobs();
        pipeline.executeJobs();
    }

}
