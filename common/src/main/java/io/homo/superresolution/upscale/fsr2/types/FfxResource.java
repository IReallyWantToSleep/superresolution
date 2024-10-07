package io.homo.superresolution.upscale.fsr2.types;

public class FfxResource {
    public long resource;
    public boolean isDepth;
    public long descriptorData;

    public int type; //FfxResourceType
    public int format; //FfxSurfaceFormat
    public int width;
    public int height;
    public int depth;
    public int mipCount;
    public int flags; //FfxResourceFlags

    public int state;

    public FfxResource(int resource,boolean isDepth,long descriptorData,
                       int type,int format,int width,int height,int depth,
                       int mipCount,int flags,int state){
        this.resource = resource;
        this.isDepth = isDepth;
        this.descriptorData = descriptorData;
        this.type = type;
        this.format = format;
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.mipCount = mipCount;
        this.flags = flags;
        this.state = state;
    }
}