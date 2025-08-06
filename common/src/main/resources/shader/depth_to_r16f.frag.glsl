#version 410
layout(location = 0) uniform sampler2D tex;
layout(location = 0) in vec2 vTexCoord;
layout(location = 0) out float outTex;

void main() {
    float depth = texture(tex, vTexCoord).r;
    outTex = depth;
}