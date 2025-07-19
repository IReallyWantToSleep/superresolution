#version 410
precision mediump float;
//--insert--define--//
#define MAX_LEVELS 8
uniform sampler2D uTexture;
layout(location = 0) uniform vec4 weightA;
layout(location = 1) uniform vec4 weightB;
layout(location = 0) out vec4 FragColor;
layout(location = 0) in vec2 vTexCoord;
void main()
{
    vec3 color = vec3(0.f);
    vec4 weight[2];
    weight[0] = weightA;
    weight[1] = weightB;
    for (int i = 0; i < MAX_LEVELS; ++i) {
        color += textureLod(uTexture, vTexCoord, float(i)).rgb * weight[i / 4][i % 4];
    }
    FragColor = vec4(
            color.rgb,
            textureLod(uTexture, vTexCoord, 0).a
        );
}
