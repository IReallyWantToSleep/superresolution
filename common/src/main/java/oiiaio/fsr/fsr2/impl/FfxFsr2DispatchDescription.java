package oiiaio.fsr.fsr2.impl;

public class FfxFsr2DispatchDescription {
    public FfxResource                 color;                              ///< A FfxResource containing the color buffer for the current frame (at render resolution).
    public FfxResource                 depth;                              ///< A FfxResource containing 32bit depth values for the current frame (at render resolution).
    public FfxResource                 motionVectors;                      ///< A FfxResource containing 2-dimensional motion vectors (at render resolution if FFX_FSR2_ENABLE_DISPLAY_RESOLUTION_MOTION_VECTORS is not set).
    public FfxResource                 exposure;                           ///< A optional FfxResource containing a 1x1 exposure value.
    public FfxResource                 reactive;                           ///< A optional FfxResource containing alpha value of reactive objects in the scene.
    public FfxResource                 transparencyAndComposition;         ///< A optional FfxResource containing alpha value of special objects in the scene.
    public FfxResource                 output;                             ///< A FfxResource containing the output color buffer for the current frame (at presentation resolution).
    public FfxFloatCoords2D            jitterOffset;                       ///< The subpixel jitter offset applied to the camera.
    public FfxFloatCoords2D            motionVectorScale;                  ///< The scale factor to apply to motion vectors.
    public FfxDimensions2D             renderSize;                         ///< The resolution that was used for rendering the input resources.
    public boolean                     enableSharpening;                   ///< Enable an additional sharpening pass.
    public float                       sharpness;                          ///< The sharpness value between 0 and 1, where 0 is no additional sharpness and 1 is maximum additional sharpness.
    public float                       frameTimeDelta;                     ///< The time elapsed since the last frame (expressed in milliseconds).
    public float                       preExposure;                        ///< The pre exposure value (must be > 0.0f)
    public boolean                     reset;                              ///< A boolean value which when set to true, indicates the camera has moved discontinuously.
    public float                       cameraNear;                         ///< The distance to the near plane of the camera.
    public float                       cameraFar;                          ///< The distance to the far plane of the camera.
    public float                       cameraFovAngleVertical;             ///< The camera angle field of view in the vertical direction (expressed in radians).
    public float                       viewSpaceToMetersFactor;            ///< The scale factor to convert view space units to meters
    public boolean                     deviceDepthNegativeOneToOne;        ///< Use OpenGL's default device Z range of [-1, 1].
    public static FfxFsr2DispatchDescription create(FfxResource color, FfxResource depth, FfxResource motionVectors, FfxResource exposure, FfxResource reactive, FfxResource transparencyAndComposition, FfxResource output, FfxFloatCoords2D jitterOffset, FfxFloatCoords2D motionVectorScale, FfxDimensions2D renderSize, boolean enableSharpening, float sharpness, float frameTimeDelta, float preExposure, boolean reset, float cameraNear, float cameraFar, float cameraFovAngleVertical, float viewSpaceToMetersFactor, boolean deviceDepthNegativeOneToOne) {
        FfxFsr2DispatchDescription i = new FfxFsr2DispatchDescription();
        i.color = color;
        i.depth = depth;
        i.motionVectors = motionVectors;
        i.exposure = exposure;
        i.reactive = reactive;
        i.transparencyAndComposition = transparencyAndComposition;
        i.output = output;
        i.jitterOffset = jitterOffset;
        i.motionVectorScale = motionVectorScale;
        i.renderSize = renderSize;
        i.enableSharpening = enableSharpening;
        i.sharpness = sharpness;
        i.frameTimeDelta = frameTimeDelta;
        i.preExposure = preExposure;
        i.reset = reset;
        i.cameraNear = cameraNear;
        i.cameraFar = cameraFar;
        i.cameraFovAngleVertical = cameraFovAngleVertical;
        i.viewSpaceToMetersFactor = viewSpaceToMetersFactor;
        i.deviceDepthNegativeOneToOne = deviceDepthNegativeOneToOne;
        return i;
    }
    public static FfxFsr2DispatchDescription create(){
        return new FfxFsr2DispatchDescription();
    }
}
