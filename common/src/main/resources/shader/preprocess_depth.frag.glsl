#version 410

#ifndef SR_GL41_COMPAT
#extension GL_ARB_shading_language_420pack : enable
#extension GL_ARB_explicit_uniform_location : enable
#endif

layout(location = 0) uniform sampler2D tex;
layout(location = 0) in vec2 vTexCoord;
layout(location = 0) out float outTex;

layout(binding = 0) uniform camera_config
{
    float near;
    float far;
};

float linearizeDepth(float depth) {
    float z = depth * 2.0 - 1.0;
    return (2.0 * far * near) / (far + near - z * (far - near));
}

void main() {
    float depth = texture(tex, vTexCoord).r;
    depth = linearizeDepth(depth);
    depth /= far - near;
    outTex = depth;
}
