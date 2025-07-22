#version 410 core

#ifndef SR_GL41_COMPAT
#extension GL_ARB_shading_language_420pack : enable
#endif

#if SR_GL41_COMPAT
uniform sampler2D tex_current;
#else
layout(binding = 1) uniform sampler2D tex_current;
#endif

layout (location = 0)in vec2 uv;
layout (location = 0)out float out_result;

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
    vec3 color = texture(tex_current, uv).rgb;
    float luminance = 0.299 * color.r + 0.587 * color.g + 0.114 * color.b;
    out_result = luminance * exposure;
}
