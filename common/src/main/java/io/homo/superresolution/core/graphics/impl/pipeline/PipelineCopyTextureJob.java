package io.homo.superresolution.core.graphics.impl.pipeline;

import io.homo.superresolution.core.graphics.impl.command.ICommandBuffer;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.system.IRenderSystem;
import io.homo.superresolution.core.math.Vector4i;

import java.util.Objects;

public class PipelineCopyTextureJob implements IPipelineJob {
    protected Vector4i sourceDimensions;
    protected Vector4i destinationDimensions;
    protected ITexture source;
    protected ITexture destination;

    public PipelineCopyTextureJob(
            ITexture source,
            ITexture destination,
            Vector4i sourceDimensions,
            Vector4i destinationDimensions
    ) {
        this.source = Objects.requireNonNull(source, "源纹理不能为null");
        this.destination = Objects.requireNonNull(destination, "目标纹理不能为null");
        if (sourceDimensions == null && destinationDimensions == null) {
            if (source.getWidth() != destination.getWidth() || source.getHeight() != destination.getHeight()) {
                throw new RuntimeException("复制区域未指定，默认复制全纹理，源纹理与目标纹理大小不同");
            }
        } else {
            validateDimensions(sourceDimensions, "源");
            validateDimensions(destinationDimensions, "目标");
        }

        this.sourceDimensions = sourceDimensions;
        this.destinationDimensions = destinationDimensions;
        if (!source.getTextureFormat().equals(destination.getTextureFormat())) {
            throw new IllegalArgumentException("纹理格式不兼容");
        }
    }

    private void validateDimensions(Vector4i dim, String name) {
        Objects.requireNonNull(dim, name + "维度不能为null");
        if (dim.x < 0 || dim.y < 0 || dim.z <= 0 || dim.w <= 0) {
            throw new IllegalArgumentException(name + "维度无效: " + dim);
        }
    }

    @Override
    public void execute(ICommandBuffer commandBuffer) {
        if (sourceDimensions == null && destinationDimensions == null) {
            if (source.getWidth() != destination.getWidth() || source.getHeight() != destination.getHeight()) {
                throw new RuntimeException("复制区域未指定，默认复制全纹理，源纹理与目标纹理大小不同");
            } else {
                commandBuffer.getEncoder().copyTexture(
                        commandBuffer,
                        source, destination,
                        0, 0, source.getWidth(), source.getHeight(), 0,
                        0, 0, destination.getWidth(), destination.getHeight(), 0
                );

            }
        } else {
            checkBounds(source, sourceDimensions, "源");
            checkBounds(destination, destinationDimensions, "目标");
            commandBuffer.getEncoder().copyTexture(
                    commandBuffer,
                    source, destination,
                    sourceDimensions.x, sourceDimensions.y,
                    sourceDimensions.z, sourceDimensions.w, 0,
                    destinationDimensions.x, destinationDimensions.y,
                    destinationDimensions.z, destinationDimensions.w, 0
            );
        }

    }

    private void checkTextureFormat() {
        if (source.getTextureFormat() != destination.getTextureFormat()) {
            throw new IllegalArgumentException("源纹理与目标纹理格式不同 %s %s".formatted(source.getTextureFormat(), destination.getTextureFormat()));
        }
    }

    private void checkBounds(ITexture tex, Vector4i dim, String name) {
        if (dim.x + dim.z > tex.getWidth() || dim.y + dim.w > tex.getHeight()) {
            throw new IllegalArgumentException(name + "复制区域超出纹理边界");
        }
    }

    @Override
    public void destroy() {
        source = null;
        destination = null;
    }
}