package io.homo.superresolution.core.graphics.impl.pipeline.jobs;

import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.math.Vector4f;

public abstract class PipelineCopyTextureJob {
    protected Vector4f sourceDimensions;
    protected Vector4f destinationDimensions;
    protected ITexture source;
    protected ITexture destination;


    /**
     * 设置拷贝源
     *
     * @param source 源资源对象
     */
    public PipelineCopyTextureJob source(ITexture source) {
        this.source = source;
        return this;


    }

    /**
     * 设置拷贝目标
     *
     * @param destination 目标资源对象
     */
    public PipelineCopyTextureJob destination(ITexture destination) {
        this.destination = destination;
        return this;


    }

    /**
     * 设置拷贝尺寸
     *
     * @param src 拷贝宽度
     * @param dst 拷贝高度
     */
    public PipelineCopyTextureJob copyDimensions(Vector4f src, Vector4f dst) {
        this.sourceDimensions = src.copy();
        this.destinationDimensions = dst.copy();
        return this;

    }
}
