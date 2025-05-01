#version 430 core
layout(binding = 1) uniform sampler2D tex_current;
in vec2 uv;
out vec2 out_grad; // R: Ix, G: Iy

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
