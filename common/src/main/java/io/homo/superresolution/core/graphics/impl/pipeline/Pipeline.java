package io.homo.superresolution.core.graphics.impl.pipeline;

import io.homo.superresolution.core.graphics.impl.command.ICommandBuffer;
import io.homo.superresolution.core.graphics.system.IRenderSystem;

import java.util.HashMap;

public class Pipeline {
    private final HashMap<String, IPipelineJob> jobs = new HashMap<>();

    public IPipelineJob job(String key) {
        return this.jobs.get(key);
    }

    public Pipeline job(String key, IPipelineJob job) {
        jobs.put(key, job);
        return this;
    }

    public void execute(ICommandBuffer commandBuffer) {
        for (IPipelineJob job : jobs.values()) {
            job.execute(commandBuffer);
        }
    }

    public void executeJob(ICommandBuffer commandBuffer, String name) {
        jobs.get(name).execute(commandBuffer);
    }


    public void destroy() {
        for (IPipelineJob job : jobs.values()) {
            job.destroy();
        }
        jobs.clear();
    }
}