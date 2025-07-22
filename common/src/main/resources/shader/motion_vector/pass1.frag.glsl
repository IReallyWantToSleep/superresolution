#version 410 core

#ifndef SR_GL41_COMPAT
#extension GL_ARB_shading_language_420pack : enable
#endif

#if SR_GL41_COMPAT
uniform sampler2D tex_current;
#else
layout(binding = 1) uniform sampler2D tex_current;
#endif

layout (location = 0) in vec2 uv;
layout (location = 0)out vec2 out_grad; // R: Ix, G: Iy


#if SR_GL41_COMPAT
layout(std140) uniform motion_vector_data {
    float exposure;
    int window_radius;
    float min_value;
    float scale;
} ;
#else
layout(std140, binding = 0) uniform motion_vector_data {
    float exposure;
    int window_radius;
    float min_value;
    float scale;
};
#endif

void main() {
    vec2 texelSize = 1.0 / textureSize(tex_current, 0);
    float right = texture(tex_current, uv + vec2(texelSize.x, 0.0)).r;
    float left = texture(tex_current, uv - vec2(texelSize.x, 0.0)).r;
    float Ix = (right - left) / (2.0 * texelSize.x);

    float top = texture(tex_current, uv + vec2(0.0, texelSize.y)).r;
    float bottom = texture(tex_current, uv - vec2(0.0, texelSize.y)).r;
    float Iy = (top - bottom) / (2.0 * texelSize.y);

    out_grad = vec2(Ix, Iy);
}
