#version 430 core

precision highp float;

layout(location = 0) in vec2 vTexCoord;
layout(location = 0) uniform sampler2D inputDepth;
layout(location = 0) out float fragColor;

void main()
{
    float depth = texture(
            inputDepth,
            vec2(vTexCoord.x, 1.0 - vTexCoord.y) // We need flip y axis for vulkan
        ).r;
    gl_FragDepth = depth;
    fragColor = depth;
}
