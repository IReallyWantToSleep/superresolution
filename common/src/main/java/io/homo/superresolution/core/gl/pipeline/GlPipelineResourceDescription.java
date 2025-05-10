package io.homo.superresolution.core.gl.pipeline;

import io.homo.superresolution.core.gl.texture.GlSampler;
import io.homo.superresolution.core.impl.texture.ITexture;

public record GlPipelineResourceDescription(
        GlPipelineResourceType type,
        String name,
        ITexture src,
        GlPipelineResourceAccess access,
        GlSampler sampler,
        int unit
) {
    public GlPipelineResourceDescription {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("资源名称不能为空");
        }
        if (type == null || access == null) {
            throw new IllegalArgumentException("类型和访问模式必须指定");
        }
    }

    @Override
    public String toString() {
        return "PipelineResourceDescription{" +
                "type=" + type +
                ", name='" + name + '\'' +
                ", src=" + src.string() +
                ", access=" + access +
                ", sampler=" + sampler +
                ", unit=" + unit +
                '}';
    }
}
