#version 430 core

layout(binding = 1) uniform sampler2D tex_current;
layout(binding = 2) uniform sampler2D tex_previous;
in vec2 uv;
out float out_it;

void main() {
    float current = texture(tex_current, uv).r;
    float previous = texture(tex_previous, uv).r;
    out_it = previous - current;
}
