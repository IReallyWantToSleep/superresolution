package io.homo.superresolution.common.upscale;

import io.homo.superresolution.common.impl.Destroyable;
import io.homo.superresolution.common.impl.Resizable;
import io.homo.superresolution.common.render.gl.framebuffer.GlFrameBuffer;
import net.minecraft.client.Minecraft;

public abstract class AbstractAlgorithm implements Resizable, Destroyable {
    /**
     * 是否支持该算法
     */
    public boolean isSupport = true;

    /**
     * 输入帧缓冲区
     */
    protected GlFrameBuffer input;

    /**
     * 输出帧缓冲区
     */
    protected GlFrameBuffer output;

    /**
     * 创建一个算法实例
     *
     * @return 返回一个算法实例
     */
    public static AbstractAlgorithm create() {
        throw new RuntimeException();
    }

    /**
     * 是否支持该算法。
     *
     * @return 如果支持返回true，否则返回false。
     */
    protected boolean isSupport() {
        return true;
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
     * 将结果绘制到屏幕上。
     *
     * @param width  绘制区域的宽度(游戏屏幕宽度)。
     * @param height 绘制区域的高度(游戏屏幕高度)。
     */
    public abstract void blitToScreen(int width, int height);

    /**
     * 销毁算法，释放资源。
     */
    @Override
    public void destroy() {
        this.output.destroyBuffers();
    }

    /**
     * 调整帧缓冲区的大小。
     *
     * @param width  新的宽度(游戏屏幕宽度)。
     * @param height 新的高度(游戏屏幕高度)。
     */
    @Override
    public void resize(int width, int height) {
        this.input.resize(width, height, Minecraft.ON_OSX);
        this.output.resize(width, height, Minecraft.ON_OSX);
    }

    /**
     * 获取输入帧缓冲区。
     *
     * @return 输入帧缓冲区。
     */
    public GlFrameBuffer getInputFrameBuffer() {
        return input;
    }

    /**
     * 设置输入帧缓冲区。
     *
     * @param input 输入帧缓冲区。
     */
    public void setInputFrameBuffer(GlFrameBuffer input) {
        this.input = input;
    }

    /**
     * 获取输出帧缓冲区。
     *
     * @return 输出帧缓冲区。
     */
    public GlFrameBuffer getOutputFrameBuffer() {
        return output;
    }

    /**
     * 设置输出帧缓冲区。
     *
     * @param output 输出帧缓冲区。
     */
    public void setOutputFrameBuffer(GlFrameBuffer output) {
        this.output = output;
    }

    /**
     * 获取输入帧缓冲区的颜色纹理ID。
     *
     * @return 输入帧缓冲区的颜色纹理ID。
     */
    public int getInputTextureId() {
        return input.getColorTextureId();
    }

    /**
     * 获取输出帧缓冲区的颜色纹理ID。
     *
     * @return 输出帧缓冲区的颜色纹理ID。
     */
    public int getOutputTextureId() {
        return output.getColorTextureId();
    }
}