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

package io.homo.superresolution.core.graphics.opengl;

public class GlConst {
    public static final int GL_TEXTURE_MAG_FILTER = 10240;
    public static final int GL_TEXTURE_MIN_FILTER = 10241;
    public static final int GL_TEXTURE_WRAP_S = 10242;
    public static final int GL_TEXTURE_WRAP_T = 10243;
    public static final int GL_TEXTURE_WRAP_R = 32882;
    public static final int GL_LINEAR_MIPMAP_NEAREST = 9985;
    public static final int GL_REPEAT = 10497;
    public static final int GL_NEAREST_MIPMAP_NEAREST = 9984;
    public static final int GL_NEAREST = 9728;
    public static final int GL_DEPTH_STENCIL_ATTACHMENT = 33306;
    public static final int GL_LINEAR = 9729;
    public static final int GL_LINEAR_MIPMAP_LINEAR = 9987;
    public static final int GL_CLAMP_TO_EDGE = 33071;
    public static final int GL_TEXTURE_2D = 3553;
    public static final int GL_COLOR_ATTACHMENT0 = 36064;
    public static final int GL_DEPTH_ATTACHMENT = 36096;
    public static final int GL_DEPTH_COMPONENT = 0x1902;
    public static final int GL_RGB = 0x1907;
    public static final int GL_RGBA = 0x1908;
    public static final int GL_UNSIGNED_BYTE = 0x1401;
    public static final int GL_FLOAT = 0x1406;
    public static final int GL_UNPACK_ALIGNMENT = 0x0CF5;
    public static final int GL_TRUE = 1;
    public static final int GL_FALSE = 0;
    public static final int GL_ONE = 1;
    public static final int GL_ZERO = 0;
    public static final int GL_SRC_ALPHA = 0x0302;
    public static final int GL_ONE_MINUS_SRC_ALPHA = 0x0303;
    public static final int GL_COLOR_BUFFER_BIT = 0x00004000;
    public static final int GL_DEPTH_BUFFER_BIT = 0x00000100;
    public static final int GL_TEXTURE_BINDING_1D = 0x8068;
    public static final int GL_TEXTURE_BINDING_2D = 0x8069;
    public static final int GL_TEXTURE0 = 0x84C0;
    public static final int GL_CLAMP_TO_BORDER = 0x812D;
    public static final int GL_MIRRORED_REPEAT = 0x8370;
    public static final int GL_MAP_READ_BIT = 0x0001;
    public static final int GL_MAP_WRITE_BIT = 0x0002;
    public static final int GL_SHADER_IMAGE_ACCESS_BARRIER_BIT = 0x00000020;
    public static final int GL_COMPUTE_SHADER = 0x91B9;
    public static final int GL_RED = 0x1903;
    public static final int GL_SHORT = 0x1402;
    public static final int GL_TEXTURE_WIDTH = 0x1000;
    public static final int GL_TEXTURE_HEIGHT = 0x1001;
    public static final int GL_TEXTURE_INTERNAL_FORMAT = 0x1003;
    public static final int GL_ARRAY_BUFFER = 0x8892;

    public static final int
            GL_READ_ONLY = 0x88B8,
            GL_WRITE_ONLY = 0x88B9,
            GL_READ_WRITE = 0x88BA;

    public static final int
            GL_BUFFER_SIZE = 0x8764,
            GL_BUFFER_USAGE = 0x8765,
            GL_BUFFER_ACCESS = 0x88BB,
            GL_BUFFER_MAPPED = 0x88BC;

    public static final int
            GL_STREAM_DRAW = 0x88E0,
            GL_STREAM_READ = 0x88E1,
            GL_STREAM_COPY = 0x88E2,
            GL_STATIC_DRAW = 0x88E4,
            GL_STATIC_READ = 0x88E5,
            GL_STATIC_COPY = 0x88E6,
            GL_DYNAMIC_DRAW = 0x88E8,
            GL_DYNAMIC_READ = 0x88E9,
            GL_DYNAMIC_COPY = 0x88EA;
}
