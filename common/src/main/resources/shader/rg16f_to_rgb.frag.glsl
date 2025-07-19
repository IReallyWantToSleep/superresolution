#version 410

precision mediump float;

layout(location = 0) uniform sampler2D tex;
layout(location = 0) in vec2 vTexCoord;
layout(location = 0) out vec4 outTex;

void main() {
    vec2 co = texture(tex, vTexCoord).rg;
    outTex = vec4(co.r,co.g,0.0,1.0);
}