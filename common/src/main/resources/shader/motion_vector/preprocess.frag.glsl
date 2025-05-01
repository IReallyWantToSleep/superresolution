#version 430 core
layout(binding = 1) uniform sampler2D tex_current;
uniform float exposure = 1.0;
in vec2 uv;
out float out_result;

void main() {
    vec3 color = texture(tex_current, uv).rgb;
    float luminance = 0.299 * color.r + 0.587 * color.g + 0.114 * color.b;
    out_result = luminance * exposure;
}
