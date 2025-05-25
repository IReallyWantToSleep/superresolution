#version 430
layout(location = 0) uniform sampler2D depthTex;
layout(location = 0) in vec2 vTexCoord;
layout(location = 0) out float outDepth;

void main() {
    float depth = texture(depthTex, vTexCoord).r;
    outDepth = depth;
}