package io.homo.superresolution.core.graphics.impl.pipeline;

import io.homo.superresolution.core.graphics.impl.buffer.BufferUsage;
import io.homo.superresolution.core.graphics.impl.buffer.IBuffer;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.impl.texture.TextureUsage;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class PipelineJobResource<RT> {
    protected final PipelineResourceAccess access;
    protected final PipelineResourceType type;
    protected Supplier<Optional<RT>> resource;
    private final Predicate<RT> validator;

    public PipelineJobResource(
            PipelineResourceAccess access,
            PipelineResourceType type,
            Supplier<Optional<RT>> resource,
            Predicate<RT> validator
    ) {
        this.access = access;
        this.type = type;
        this.validator = validator != null ? validator : rt -> true;
        this.resource = () -> {
            if (!this.validator.test(resource.get().orElse(null))) {
                throw new IllegalArgumentException("Invalid resource provided for " + this.getClass().getSimpleName());
            } else {
                return resource.get();
            }
        };
    }

    public PipelineResourceAccess getAccess() {
        return access;
    }

    public PipelineResourceType getType() {
        return type;
    }

    public Optional<RT> getResource() {
        if (!validator.test(resource.get().orElse(null))) {
            throw new IllegalArgumentException("Invalid resource provided for " + this.getClass().getSimpleName());
        }
        return resource.get();
    }

    public void setResource(RT resource) {
        if (!validator.test(resource)) {
            throw new IllegalArgumentException("Invalid resource provided for " + this.getClass().getSimpleName());
        }
        this.resource = () -> Optional.of(resource);
    }

    public static class SamplerTexture extends PipelineJobResource<ITexture> {
        protected SamplerTexture(
                PipelineResourceAccess access,
                PipelineResourceType type,
                Supplier<Optional<ITexture>> resource
        ) {
            super(access, type, resource, texture -> texture == null || texture.getTextureUsages().getUsages().contains(TextureUsage.Sampler));
        }

        public static SamplerTexture create(Supplier<Optional<ITexture>> texture) {
            return new SamplerTexture(
                    PipelineResourceAccess.Read,
                    PipelineResourceType.SamplerTexture,
                    texture
            );
        }

        public static SamplerTexture create(ITexture texture) {
            return create(() -> Optional.of(texture));
        }
    }

    public static class StorageTexture extends PipelineJobResource<ITexture> {
        protected StorageTexture(
                PipelineResourceAccess access,
                PipelineResourceType type,
                Supplier<Optional<ITexture>> resource
        ) {
            super(access, type, resource, texture -> texture == null || texture.getTextureUsages().getUsages().contains(TextureUsage.Storage));
        }

        public static StorageTexture create(
                Supplier<Optional<ITexture>> texture,
                PipelineResourceAccess access
        ) {
            return new StorageTexture(
                    access,
                    PipelineResourceType.StorageTexture,
                    texture
            );
        }

        public static StorageTexture create(
                ITexture texture,
                PipelineResourceAccess access
        ) {
            return create(() -> Optional.of(texture), access);
        }
    }

    public static class UniformBuffer extends PipelineJobResource<IBuffer> {
        protected UniformBuffer(
                PipelineResourceAccess access,
                PipelineResourceType type,
                Supplier<Optional<IBuffer>> resource
        ) {
            super(access, type, resource, buffer -> buffer == null || buffer.getUsage() == BufferUsage.UBO);
        }

        public static UniformBuffer create(Supplier<Optional<IBuffer>> ubo) {
            return new UniformBuffer(
                    PipelineResourceAccess.Read,
                    PipelineResourceType.UniformBuffer,
                    ubo
            );
        }

        public static UniformBuffer create(IBuffer ubo) {
            if (ubo.getUsage() != BufferUsage.UBO) {
                throw new IllegalArgumentException("Buffer must have UBO usage");
            }
            return create(() -> Optional.of(ubo));
        }
    }
}