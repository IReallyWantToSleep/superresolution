#version 430 core

layout(binding = 1) uniform sampler2D tex_current;
layout(binding = 2) uniform sampler2D tex_previous;
layout (location = 0) in vec2 uv;
layout (location = 0)out float out_it;

layout(std140, binding = 0) uniform motion_vector_data_t {
    float exposure;
    int window_radius;
    float min_value;
    float scale;
} motion_vector_data;

void main() {
    float current = texture(tex_current, uv).r;
    float previous = texture(tex_previous, uv).r;
    out_it = previous - current;
}
