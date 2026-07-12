package io.homo.superresolution.core.ngx;

public final class NgxVKDLSSEvalParams {
    public final NgxVKFeatureEvalParams feature = new NgxVKFeatureEvalParams();
    public NgxResourceVK depth;
    public NgxResourceVK motionVectors;
    public float jitterOffsetX;
    public float jitterOffsetY;
    public final NgxDimensions renderSubrectDimensions = new NgxDimensions();
    public int reset;
    public float motionVectorScaleX;
    public float motionVectorScaleY;
    public NgxResourceVK transparencyMask;
    public NgxResourceVK exposureTexture;
    public NgxResourceVK biasCurrentColorMask;
    public final NgxCoordinates colorSubrectBase = new NgxCoordinates();
    public final NgxCoordinates depthSubrectBase = new NgxCoordinates();
    public final NgxCoordinates motionVectorSubrectBase = new NgxCoordinates();
    public final NgxCoordinates translucencySubrectBase = new NgxCoordinates();
    public final NgxCoordinates biasCurrentColorSubrectBase = new NgxCoordinates();
    public final NgxCoordinates outputSubrectBase = new NgxCoordinates();
    public float preExposure;
    public float exposureScale;
    public int indicatorInvertXAxis;
    public int indicatorInvertYAxis;
    public final NgxVKGBuffer gBuffer = new NgxVKGBuffer();
    public int toneMapperType;
    public NgxResourceVK motionVectors3D;
    public NgxResourceVK particleMask;
    public NgxResourceVK animatedTextureMask;
    public NgxResourceVK depthHighRes;
    public NgxResourceVK positionViewSpace;
    public float frameTimeDeltaInMsec;
    public NgxResourceVK rayTracingHitDistance;
    public NgxResourceVK motionVectorsReflections;
}
