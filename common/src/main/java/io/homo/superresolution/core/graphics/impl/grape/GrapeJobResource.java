/*
 * Super Resolution
 * Copyright (c) 2025. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.homo.superresolution.core.graphics.impl.grape;

import io.homo.superresolution.core.graphics.impl.buffer.BufferUsage;
import io.homo.superresolution.core.graphics.impl.buffer.IBuffer;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.impl.texture.TextureUsage;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class GrapeJobResource<RT> {
    protected final GrapeResourceAccess access;
    protected final GrapeResourceType type;
    protected Supplier<Optional<RT>> resource;
    private final Predicate<RT> validator;

    public GrapeJobResource(
            GrapeResourceAccess access,
            GrapeResourceType type,
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

    public GrapeResourceAccess getAccess() {
        return access;
    }

    public GrapeResourceType getType() {
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

    public static class SamplerTexture extends GrapeJobResource<ITexture> {
        protected SamplerTexture(
                GrapeResourceAccess access,
                GrapeResourceType type,
                Supplier<Optional<ITexture>> resource
        ) {
            super(access, type, resource, texture -> texture == null || texture.getTextureUsages().getUsages().contains(TextureUsage.Sampler));
        }

        public static SamplerTexture create(Supplier<Optional<ITexture>> texture) {
            return new SamplerTexture(
                    GrapeResourceAccess.Read,
                    GrapeResourceType.SamplerTexture,
                    texture
            );
        }

        public static SamplerTexture create(ITexture texture) {
            return create(() -> Optional.of(texture));
        }
    }

    public static class StorageTexture extends GrapeJobResource<ITexture> {
        protected StorageTexture(
                GrapeResourceAccess access,
                GrapeResourceType type,
                Supplier<Optional<ITexture>> resource
        ) {
            super(access, type, resource, texture -> texture == null || texture.getTextureUsages().getUsages().contains(TextureUsage.Storage));
        }

        public static StorageTexture create(
                Supplier<Optional<ITexture>> texture,
                GrapeResourceAccess access
        ) {
            return new StorageTexture(
                    access,
                    GrapeResourceType.StorageTexture,
                    texture
            );
        }

        public static StorageTexture create(
                ITexture texture,
                GrapeResourceAccess access
        ) {
            return create(() -> Optional.of(texture), access);
        }
    }

    public static class UniformBuffer extends GrapeJobResource<IBuffer> {
        protected UniformBuffer(
                GrapeResourceAccess access,
                GrapeResourceType type,
                Supplier<Optional<IBuffer>> resource
        ) {
            super(access, type, resource, buffer -> buffer == null || buffer.getUsage() == BufferUsage.Ubo);
        }

        public static UniformBuffer create(Supplier<Optional<IBuffer>> ubo) {
            return new UniformBuffer(
                    GrapeResourceAccess.Read,
                    GrapeResourceType.UniformBuffer,
                    ubo
            );
        }

        public static UniformBuffer create(IBuffer ubo) {
            if (ubo.getUsage() != BufferUsage.Ubo) {
                throw new IllegalArgumentException("Buffer must have UBO usage");
            }
            return create(() -> Optional.of(ubo));
        }
    }
}