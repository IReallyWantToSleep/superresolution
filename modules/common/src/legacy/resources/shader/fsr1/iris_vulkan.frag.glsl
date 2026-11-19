#version 450

uniform sampler2D InSampler;
layout(std140) uniform FsrParams {
    ivec4 OutputRect;
    float Sharpness;
};
layout(location = 0) out vec4 fragColor;

#define A_GPU 1
#define A_GLSL 1
#define A_SKIP_EXT 1
#define FSR_STAGE 1
#include "ffx_a.h"
#ifdef FSR_EASU_F
vec4 FsrEasuRF(vec2 position) { return textureGather(InSampler, position, 0); }
vec4 FsrEasuGF(vec2 position) { return textureGather(InSampler, position, 1); }
vec4 FsrEasuBF(vec2 position) { return textureGather(InSampler, position, 2); }
#else
vec4 FsrRcasLoadF(ivec2 position) { return texelFetch(InSampler, clamp(position, ivec2(0), textureSize(InSampler, 0) - 1), 0); }
void FsrRcasInputF(inout float red, inout float green, inout float blue) { }
#endif
#include "ffx_fsr1.h"

void main() {
    uvec2 pixel = uvec2(ivec2(gl_FragCoord.xy) - OutputRect.xy);
    vec3 color;
#ifdef FSR_EASU_F
    vec2 inputSize = vec2(textureSize(InSampler, 0));
    uvec4 con0, con1, con2, con3;
    FsrEasuCon(con0, con1, con2, con3, inputSize.x, inputSize.y, inputSize.x, inputSize.y, float(OutputRect.z), float(OutputRect.w));
    FsrEasuF(color, pixel, con0, con1, con2, con3);
#else
    uvec4 con0;
    FsrRcasCon(con0, Sharpness);
    FsrRcasF(color.r, color.g, color.b, pixel, con0);
#endif
    fragColor = vec4(color, 1.0);
}
