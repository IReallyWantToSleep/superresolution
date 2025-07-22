#version 410
precision mediump float;

#ifndef SR_GL41_COMPAT
#extension GL_ARB_shading_language_420pack : enable
#endif

layout(location = 0) in vec2 aPosition;
layout(location = 1) in vec2 aTexCoord;
layout (location = 0) out vec2 uv;


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
    uv = aTexCoord;
    gl_Position = vec4(aPosition, 0.0, 1.0);
}
