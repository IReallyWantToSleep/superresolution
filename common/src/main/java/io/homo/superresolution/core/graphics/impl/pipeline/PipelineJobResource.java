package io.homo.superresolution.core.graphics.impl.pipeline;

import io.homo.superresolution.core.graphics.impl.buffer.BufferUsage;
import io.homo.superresolution.core.graphics.impl.buffer.IBuffer;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;

public class PipelineJobResource<RT> {
    protected final PipelineResourceAccess access;
    protected final PipelineResourceType type;
    protected RT resource;

    public PipelineJobResource(
            PipelineResourceAccess access,
            PipelineResourceType type,
            RT resource
    ) {
        this.access = access;
        this.type = type;
        this.resource = resource;
    }

    public PipelineResourceAccess getAccess() {
        return access;
    }

    public PipelineResourceType getType() {
        return type;
    }

    public RT getResource() {
        return resource;
    }

    public void setResource(RT resource) {
        this.resource = resource;
    }

    public static class SamplerTexture extends PipelineJobResource<ITexture> {
        protected SamplerTexture(
                PipelineResourceAccess access,
                PipelineResourceType type,
                ITexture resource
        ) {
            super(access, type, resource);
        }

        public static SamplerTexture create(
                ITexture texture
        ) {
            return new SamplerTexture(
                    PipelineResourceAccess.Read,
                    PipelineResourceType.SamplerTexture,
                    texture
            );
        }
    }

    public static class StorageTexture extends PipelineJobResource<ITexture> {
        protected StorageTexture(
                PipelineResourceAccess access,
                PipelineResourceType type,
                ITexture resource
        ) {
            super(access, type, resource);
        }

        public static StorageTexture create(
                ITexture texture,
                PipelineResourceAccess access
        ) {
            return new StorageTexture(
                    access,
                    PipelineResourceType.StorageTexture,
                    texture
            );
        }
    }

    public static class UniformBuffer extends PipelineJobResource<IBuffer> {
        protected UniformBuffer(
                PipelineResourceAccess access,
                PipelineResourceType type,
                IBuffer resource
        ) {
            super(access, type, resource);
        }

        public static UniformBuffer create(
                IBuffer ubo
        ) {
            if (ubo.getUsage() != BufferUsage.UBO) throw new RuntimeException();
            return new UniformBuffer(
                    PipelineResourceAccess.Read,
                    PipelineResourceType.UniformBuffer,
                    ubo
            );
        }
    }
}
