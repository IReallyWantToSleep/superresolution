#version 410 core

#ifndef SR_GL41_COMPAT
#extension GL_ARB_shading_language_420pack : enable
#endif

#if SR_GL41_COMPAT
uniform sampler2D tex_current;
uniform sampler2D tex_previous;
#else
layout(binding = 1) uniform sampler2D tex_current;
layout(binding = 2) uniform sampler2D tex_previous;
#endif

layout (location = 0) in vec2 uv;
layout (location = 0)out float out_it;


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
    float current = texture(tex_current, uv).r;
    float previous = texture(tex_previous, uv).r;
    out_it = previous - current;
}
