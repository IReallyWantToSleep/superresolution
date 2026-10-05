#version 410

#ifndef SR_GL41_COMPAT
#extension GL_ARB_shading_language_420pack : enable
#extension GL_ARB_explicit_uniform_location : enable
#endif

precision mediump float;

layout(location = 0) uniform sampler2D uTexture;
layout(location = 0) in vec2 vTexCoord;
layout(location = 0) out vec4 FragColor;
void main() {
    FragColor = texture(uTexture, vTexCoord);
}
