package io.homo.superresolution.core.graphics.impl.pipeline;

/**
 * 管线资源的类型
 */
public enum PipelineResourceType {
    /**
     * 只读纹理
     */
    Texture,
    /**
     * 可读可写的纹理
     */
    Image,
    /**
     * UBO
     */
    UniformBuffer
}
