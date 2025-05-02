package io.homo.superresolution.core.gl.pipeline;

import java.util.LinkedHashMap;
import java.util.Map;

public class GlPipeline {
    private final Map<String, PipelineJob> jobs = new LinkedHashMap<>();
    private final PipelineResourceDescriptions globalResources = new PipelineResourceDescriptions();

    public GlPipeline() {
    }

    public static GlPipeline create() {
        return new GlPipeline();
    }

    public PipelineResourceDescriptions getGlobalResources() {
        return globalResources;
    }

    public GlPipeline addJob(String jobName, PipelineJob job) {
        jobs.put(jobName, job);
        job.bindPipeline(this);
        return this;
    }

    public PipelineJob getJob(String jobName) {
        return jobs.get(jobName);

    }

    public GlPipeline addResource(PipelineResourceDescriptions descriptions) {
        globalResources.resource.putAll(descriptions.resource);
        return this;
    }

    public GlPipeline addResource(PipelineResourceDescription description) {
        globalResources.resource.put(description.name(), description);
        return this;
    }

    public void scheduleJobs(PipelineJobDispatchResource dispatchResource) {
        jobs.values().forEach((job) -> job.schedule(dispatchResource));
    }

    public void executeJobs(PipelineJobDispatchResource dispatchResource) {
        jobs.values().forEach((job) -> job.execute(dispatchResource));
    }

    public void scheduleJob(String name, PipelineJobDispatchResource dispatchResource) {
        jobs.get(name).schedule(dispatchResource);
    }

    public void executeJob(String name, PipelineJobDispatchResource dispatchResource) {
        jobs.get(name).execute(dispatchResource);
    }

    public void executeAll(PipelineJobDispatchResource dispatchResource) {
        scheduleJobs(dispatchResource);
        executeJobs(dispatchResource);
    }

    public void execute(String name, PipelineJobDispatchResource dispatchResource) {
        scheduleJob(name, dispatchResource);
        executeJob(name, dispatchResource);
    }

    public PipelineResourceDescription findResource(String name, boolean searchJobs) {
        if (globalResources.resource.get(name) != null) {
            return globalResources.resource.get(name);
        } else if (searchJobs) {
            for (PipelineJob job : jobs.values()) {
                if (job.resourcesMap.resource.get(name) != null) {
                    return job.resourcesMap.resource.get(name);
                }
            }
        }
        return null;
    }

    public PipelineResourceDescription findResource(String name) {
        return findResource(name, true);
    }
}