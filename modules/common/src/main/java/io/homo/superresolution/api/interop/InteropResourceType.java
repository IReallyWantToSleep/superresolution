package io.homo.superresolution.api.interop;

import io.homo.superresolution.api.InputResourceType;

/** Semantic resource names; allocation policy belongs to the algorithm's requirements. */
public enum InteropResourceType {
    Color(InputResourceType.Color),
    Depth(InputResourceType.Depth),
    MotionVectors(InputResourceType.MotionVectors),
    Exposure(InputResourceType.Exposure),
    DiffuseAlbedo(InputResourceType.DiffuseAlbedo),
    SpecularAlbedo(InputResourceType.SpecularAlbedo),
    Normals(InputResourceType.Normals),
    Roughness(InputResourceType.Roughness),
    NormalRoughness(InputResourceType.NormalRoughness),
    SpecularMotionVectors(InputResourceType.SpecularMotionVectors),
    SpecularHitDistance(InputResourceType.SpecularHitDistance),
    TransparencyLayer(InputResourceType.TransparencyLayer),
    TransparencyLayerOpacity(InputResourceType.TransparencyLayerOpacity),
    ColorBeforeTransparency(InputResourceType.ColorBeforeTransparency),
    ScreenSpaceSubsurfaceScatteringGuide(InputResourceType.ScreenSpaceSubsurfaceScatteringGuide),
    DepthOfFieldGuide(InputResourceType.DepthOfFieldGuide),
    OutputColor(null);

    private final InputResourceType inputType;

    InteropResourceType(InputResourceType inputType) {
        this.inputType = inputType;
    }

    public boolean isInput() {
        return inputType != null;
    }

    /** Null only for outputs. */
    public InputResourceType inputType() {
        return inputType;
    }

    public static InteropResourceType fromInput(InputResourceType type) {
        for (InteropResourceType resource : values()) {
            if (resource.inputType == type && type != null) {
                return resource;
            }
        }
        throw new IllegalArgumentException("Unknown input resource: " + type);
    }
}
