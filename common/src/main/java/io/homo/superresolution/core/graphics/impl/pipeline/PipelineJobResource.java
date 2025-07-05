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

    public static class Texture extends PipelineJobResource<ITexture> {
        protected Texture(
                PipelineResourceAccess access,
                PipelineResourceType type,
                ITexture resource
        ) {
            super(access, type, resource);
        }

        public static Texture create(
                ITexture texture
        ) {
            return new Texture(
                    PipelineResourceAccess.Read,
                    PipelineResourceType.Texture,
                    texture
            );
        }
    }

    public static class Image extends PipelineJobResource<ITexture> {
        protected Image(
                PipelineResourceAccess access,
                PipelineResourceType type,
                ITexture resource
        ) {
            super(access, type, resource);
        }

        public static Image create(
                ITexture texture,
                PipelineResourceAccess access
        ) {
            return new Image(
                    access,
                    PipelineResourceType.Texture,
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
