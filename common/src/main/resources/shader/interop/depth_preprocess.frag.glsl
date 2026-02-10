#version 430 core
precision highp float;
layout(location = 0) in vec2 vTexCoord;
layout(location = 0) uniform sampler2D inputDepth;
layout(location = 0) out float fragColor;
void main()
{
    float depth = texture(
            inputDepth,
            vec2(vTexCoord.x, 1.0 - vTexCoord.y) // 对于Vulkan纹理坐标系，需要翻转Y轴
        ).r;
    gl_FragDepth = depth;
    fragColor = depth;
}
