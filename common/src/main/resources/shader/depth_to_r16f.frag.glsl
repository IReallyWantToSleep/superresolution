#version 410
#extension GL_ARB_explicit_uniform_location : enable
layout(location = 0) uniform sampler2D tex;
layout(location = 0) in vec2 vTexCoord;
layout(location = 0) out float outTex;

void main() {
    float depth = texture(tex, vTexCoord).r;
    outTex = depth;
}
