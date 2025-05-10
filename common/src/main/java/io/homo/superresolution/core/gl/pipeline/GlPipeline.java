package io.homo.superresolution.core.gl.pipeline;

import java.util.LinkedHashMap;
import java.util.Map;

public class GlPipeline {
    private final Map<String, GlPipelineJob> jobs = new LinkedHashMap<>();
    private final GlPipelineResourceDescriptions globalResources = new GlPipelineResourceDescriptions();

    public GlPipeline() {
    }

    public static GlPipeline create() {
        return new GlPipeline();
    }

    public GlPipelineResourceDescriptions getGlobalResources() {
        return globalResources;
    }

    public GlPipeline addJob(String jobName, GlPipelineJob job) {
        jobs.put(jobName, job);
        job.bindPipeline(this);
        return this;
    }

    public GlPipelineJob getJob(String jobName) {
        return jobs.get(jobName);

    }

    public GlPipeline addResource(GlPipelineResourceDescriptions descriptions) {
        globalResources.resource.putAll(descriptions.resource);
        return this;
    }

    public GlPipeline addResource(GlPipelineResourceDescription description) {
        globalResources.resource.put(description.name(), description);
        return this;
    }

    public void scheduleJobs(GlPipelineJobDispatchResource dispatchResource) {
        jobs.values().forEach((job) -> job.schedule(dispatchResource));
    }

    public void executeJobs(GlPipelineJobDispatchResource dispatchResource) {
        jobs.values().forEach((job) -> job.execute(dispatchResource));
    }

    public void scheduleJob(String name, GlPipelineJobDispatchResource dispatchResource) {
        jobs.get(name).schedule(dispatchResource);
    }

    public void executeJob(String name, GlPipelineJobDispatchResource dispatchResource) {
        jobs.get(name).execute(dispatchResource);
    }

    public void executeAll(GlPipelineJobDispatchResource dispatchResource) {
        scheduleJobs(dispatchResource);
        executeJobs(dispatchResource);
    }

    public void execute(String name, GlPipelineJobDispatchResource dispatchResource) {
        scheduleJob(name, dispatchResource);
        executeJob(name, dispatchResource);
    }

    public GlPipelineResourceDescription findResource(String name, boolean searchJobs) {
        if (globalResources.resource.get(name) != null) {
            return globalResources.resource.get(name);
        } else if (searchJobs) {
            for (GlPipelineJob job : jobs.values()) {
                if (job.resourcesMap.resource.get(name) != null) {
                    return job.resourcesMap.resource.get(name);
                }
            }
        }
        return null;
    }

    public GlPipelineResourceDescription findResource(String name) {
        return findResource(name, true);
    }
}