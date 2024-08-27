package io.homo.superresolution.fsr2;

import io.homo.superresolution.config.Config;
import io.homo.superresolution.impl.CanDestroy;
import io.homo.superresolution.impl.CanResize;
import io.homo.superresolution.resolutioncontrol.utils.FrameBuffer;
import net.minecraft.client.Minecraft;

public class FSR2Helper implements CanResize, CanDestroy {
    private final FrameBuffer motionVectorsBuffer;
    public int renderWidth;
    public int renderHeight;
    public FSR2Helper(){
        motionVectorsBuffer = new FrameBuffer(false);
    }
    public void updateMotionVectors(){

    }
    public int getMotionVectorsTex(){
        return motionVectorsBuffer.getColorTextureId();
    }
    public void resize(int width,int height){
        renderWidth = (int) (width* Config.getRenderScaleFactor());
        renderHeight = (int) (height* Config.getRenderScaleFactor());
        this.motionVectorsBuffer.resize(renderWidth,renderHeight,Minecraft.ON_OSX);
    }
    public void destroy(){
        this.motionVectorsBuffer.destroyBuffers();
    }
}
