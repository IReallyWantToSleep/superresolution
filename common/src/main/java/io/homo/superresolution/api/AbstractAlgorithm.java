package io.homo.superresolution.api;

import io.homo.superresolution.core.impl.Destroyable;
import io.homo.superresolution.core.impl.Resizable;
import io.homo.superresolution.core.graphics.impl.framebuffer.FrameBufferAttachmentType;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.common.upscale.DispatchResource;

public abstract class AbstractAlgorithm implements Resizable, Destroyable {
    /**
     * 输入帧缓冲区
     */
    protected IFrameBuffer input;

    /**
     * 输出帧缓冲区
     */
    protected IFrameBuffer output;

    public AbstractAlgorithm() {

    }

    /**
     * 初始化算法。
     */
    public abstract void init();

    /**
     * 运行算法。
     *
     * @param dispatchResource 运行算法所需资源。
     * @return 如果运行成功返回true，否则返回false。
     */
    public abstract boolean dispatch(DispatchResource dispatchResource);

    /**
     * 销毁算法，释放资源。
     */
    @Override
    public abstract void destroy();

    /**
     * 调整帧缓冲区的大小。
     *
     * @param width  新的宽度(游戏屏幕宽度)。
     * @param height 新的高度(游戏屏幕高度)。
     */
    @Override
    public abstract void resize(int width, int height);

    /**
     * 获取输入帧缓冲区。
     *
     * @return 输入帧缓冲区。
     */
    public IFrameBuffer getInputFrameBuffer() {
        return input;
    }

    /**
     * 设置输入帧缓冲区。
     *
     * @param input 输入帧缓冲区。
     */
    public void setInputFrameBuffer(IFrameBuffer input) {
        this.input = input;
    }

    /**
     * 获取输出帧缓冲区。
     *
     * @return 输出帧缓冲区。
     */
    public IFrameBuffer getOutputFrameBuffer() {
        return output;
    }

    /**
     * 设置输出帧缓冲区。
     *
     * @param output 输出帧缓冲区。
     */
    public void setOutputFrameBuffer(IFrameBuffer output) {
        this.output = output;
    }

    /**
     * 获取输入帧缓冲区的颜色纹理ID。
     *
     * @return 输入帧缓冲区的颜色纹理ID。
     */
    public int getInputTextureId() {
        return input.getTextureId(FrameBufferAttachmentType.Color);
    }

    /**
     * 获取输出帧缓冲区的颜色纹理ID。
     *
     * @return 输出帧缓冲区的颜色纹理ID。
     */
    public int getOutputTextureId() {
        return output.getTextureId(FrameBufferAttachmentType.Color);
    }
}