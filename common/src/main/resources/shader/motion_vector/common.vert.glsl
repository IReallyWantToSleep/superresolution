#version 430
precision mediump float;

layout(location = 0) in vec2 aPosition;
layout(location = 1) in vec2 aTexCoord;
layout (location = 0) out vec2 uv;

layout(std140, binding = 0) uniform motion_vector_data_t {
    float exposure;
    int window_radius;
    float min_value;
    float scale;
} motion_vector_data;

void main() {
    uv = aTexCoord;
    gl_Position = vec4(aPosition, 0.0, 1.0);
}
