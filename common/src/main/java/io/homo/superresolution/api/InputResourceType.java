/*
 * Super Resolution
 * Copyright (c) 2025-2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.homo.superresolution.api;

public enum InputResourceType {
    Color("color"),
    Depth("depth"),
    MotionVectors("motion_vectors"),
    Exposure("exposure"),
    DiffuseAlbedo("diffuse_albedo"),
    SpecularAlbedo("specular_albedo"),
    Normals("normals"),
    Roughness("roughness"),
    NormalRoughness("normal_roughness"),
    SpecularMotionVectors("specular_motion_vectors"),
    SpecularHitDistance("specular_hit_distance"),
    TransparencyLayer("transparency_layer"),
    TransparencyLayerOpacity("transparency_layer_opacity"),
    ColorBeforeTransparency("color_before_transparency"),
    ScreenSpaceSubsurfaceScatteringGuide("screen_space_subsurface_scattering_guide"),
    DepthOfFieldGuide("depth_of_field_guide");

    private final String v3InputKey;

    InputResourceType(String v3InputKey) {
        this.v3InputKey = v3InputKey;
    }

    public String getV3InputKey() {
        return v3InputKey;
    }

    public static InputResourceType fromV3InputKey(String key) {
        if (key == null) {
            return null;
        }
        for (InputResourceType type : values()) {
            if (type.v3InputKey.equals(key)) {
                return type;
            }
        }
        return null;
    }
}
