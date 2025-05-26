#version 430
layout(location = 0) uniform sampler2D depthTex;
layout(location = 0) in vec2 vTexCoord;
layout(location = 0) out float outDepth;

void main() {
    float depth = texture(depthTex, vTexCoord).r;
    depth -= 0.995;
    depth = 0.005/depth;
    outDepth = depth;
}