package io.homo.superresolution.fsr2;

import io.homo.superresolution.config.Config;
import io.homo.superresolution.impl.CanDestroy;
import io.homo.superresolution.impl.CanResize;
import io.homo.superresolution.utils.FrameBuffer;
import net.minecraft.client.Minecraft;

public class FSR2Helper implements CanResize, CanDestroy {
    private final FrameBuffer motionVectorsBuffer;
    public int renderWidth;
    public int renderHeight;
    public int screenWidth;
    public int screenHeight;
    public FSR2Helper(){
        motionVectorsBuffer = new FrameBuffer(false);

    }
    public void updateMotionVectors(){
        motionVectorsBuffer.setClearColor(0,0,0,1.0f);
        motionVectorsBuffer.clear(Minecraft.ON_OSX);
    }
    public int getMotionVectorsTex(){
        return motionVectorsBuffer.getColorTextureId();
    }
    public FrameBuffer getMotionVectorsBuffer(){
        return motionVectorsBuffer;
    }
    public void resize(int width,int height){
        screenWidth = width;
        screenHeight = height;
        renderWidth = (int) (width* Config.getRenderScaleFactor());
        renderHeight = (int) (height* Config.getRenderScaleFactor());
        this.motionVectorsBuffer.resize(renderWidth,renderHeight,Minecraft.ON_OSX);
    }
    public void destroy(){
        this.motionVectorsBuffer.destroyBuffers();
    }
    public float getCameraNear() {
        Minecraft minecraft = Minecraft.getInstance();
        double e = Math.tan((double)((float)minecraft.options.fov().get() * 0.017453292F) / 2.0) * 0.15000000074505806;
        return (float)e;
    }

    public float getCameraFar() {
        Minecraft minecraft = Minecraft.getInstance();
        return 3.402823466e+38F;
    }

    public float getCameraFovAngleVertical() {
        Minecraft minecraft = Minecraft.getInstance();
        double fov = (double)minecraft.options.fov().get();
        double fovRadians = fov * 0.017453292F;
        return (float)(2 * Math.atan(Math.tan(fovRadians / 2)));
    }
}
