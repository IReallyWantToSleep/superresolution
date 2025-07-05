package io.homo.superresolution.core.graphics.impl.pipeline;

import io.homo.superresolution.core.graphics.impl.buffer.IBuffer;
import io.homo.superresolution.core.graphics.system.IRenderSystem;

public class PipelineCopyBufferJob implements IPipelineJob {
    protected long srcOffset = 0;
    protected long dstOffset = 0;
    protected long size = 0;
    protected IBuffer source;
    protected IBuffer destination;

    public PipelineCopyBufferJob(
            IBuffer source,
            IBuffer destination,
            long srcOffset,
            long dstOffset,
            long size
    ) {
        this.srcOffset = srcOffset;
        this.dstOffset = dstOffset;
        this.size = size;
        this.source = source;
        this.destination = destination;
    }

    /**
     * 设置拷贝源
     *
     * @param source 源资源对象
     */
    public PipelineCopyBufferJob source(IBuffer source) {
        this.source = source;
        return this;
    }

    /**
     * 设置拷贝目标
     *
     * @param destination 目标资源对象
     */
    public PipelineCopyBufferJob destination(IBuffer destination) {
        this.destination = destination;
        return this;
    }

    /**
     * 设置拷贝区域
     *
     * @param srcOffset 源偏移量
     * @param dstOffset 目标偏移量
     * @param size      拷贝数据大小
     */
    public PipelineCopyBufferJob copyRegion(long srcOffset, long dstOffset, long size) {
        this.srcOffset = srcOffset;
        this.dstOffset = dstOffset;
        this.size = size;
        return this;
    }

    public long getSrcOffset() {
        return srcOffset;
    }

    public long getDstOffset() {
        return dstOffset;
    }

    public long getSize() {
        return size;
    }

    public IBuffer getSource() {
        return source;
    }

    public IBuffer getDestination() {
        return destination;
    }

    @Override
    public void execute(IRenderSystem renderSystem) {

    }

    @Override
    public void destroy() {

    }
}
