package io.homo.superresolution.upscale;

import com.mojang.blaze3d.pipeline.MainTarget;
import io.homo.superresolution.impl.CanDestroy;
import io.homo.superresolution.impl.CanResize;
import io.homo.superresolution.render.gl.utils.FrameBuffer;
import net.minecraft.client.Minecraft;

public abstract class AbstractAlgorithm implements CanResize, CanDestroy {
    protected MainTarget input;
    protected FrameBuffer output;
    public boolean isSupport = true;
    public static AbstractAlgorithm create() {
        return null;
    }

    public boolean isSupport() {return true;}

    public abstract void init();
    public abstract boolean run(float frameTimeDelta);
    public abstract void blitToScreen(int width,int height);

    public void destroy() {
        this.output.destroyBuffers();
    }

    public void resize(int width, int height) {
        this.input.resize(width,height, Minecraft.ON_OSX);
        this.output.resize(width,height, Minecraft.ON_OSX);
    }

    public void setInput(MainTarget input) {
        this.input = input;
    }

    public void setOutput(FrameBuffer output) {
        this.output = output;
    }

    public MainTarget getInput() {
        return input;
    }

    public FrameBuffer getOutput() {
        return output;
    }

    public int getInputTexId() {
        return input.getColorTextureId();
    }

    public int getOutputTexId() {
        return output.getColorTextureId();
    }
}
