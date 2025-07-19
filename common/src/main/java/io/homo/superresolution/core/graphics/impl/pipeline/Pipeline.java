package io.homo.superresolution.core.graphics.impl.pipeline;

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

    public void execute(IRenderSystem renderSystem) {
        for (IPipelineJob job : jobs.values()) {
            job.execute(renderSystem);
        }
    }

    public void executeJob(IRenderSystem renderSystem, String name) {
        jobs.get(name).execute(renderSystem);
    }


    public void destroy() {
        for (IPipelineJob job : jobs.values()) {
            job.destroy();
        }
        jobs.clear();
    }
}