package io.homo.superresolution.core.graphics.impl.pipeline;

/**
 * 管线资源的类型
 */
public enum PipelineResourceType {
    /**
     * 只读纹理
     */
    SamplerTexture,
    /**
     * 可读可写的纹理
     */
    StorageTexture,
    /**
     * UBO
     */
    UniformBuffer
}
