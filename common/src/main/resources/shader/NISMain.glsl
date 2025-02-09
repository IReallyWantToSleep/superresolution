#version 450
#extension GL_ARB_separate_shader_objects : enable
#extension GL_ARB_shading_language_420pack : enable

//--insert--define--//
#define NIS_GLSL 1
#define NIS_SCALER 1

layout(local_size_x=256) in;
layout(binding = 0) uniform sampler2D samplerLinearClamp;
layout(binding = 1) uniform sampler2D in_texture;
layout(binding = 2,rgba8) writeonly uniform image2D out_texture;
layout(binding = 3) uniform sampler2D coef_scaler;
layout(binding = 4) uniform sampler2D coef_usm;

uniform float kDetectRatio;
uniform float kDetectThres;
uniform float kMinContrastRatio;
uniform float kRatioNorm;
uniform float kContrastBoost;
uniform float kEps;
uniform float kSharpStartY;
uniform float kSharpScaleY;
uniform float kSharpStrengthMin;
uniform float kSharpStrengthScale;
uniform float kSharpLimitMin;
uniform float kSharpLimitScale;
uniform float kScaleX;
uniform float kScaleY;
uniform float kDstNormX;
uniform float kDstNormY;
uniform float kSrcNormX;
uniform float kSrcNormY;
uniform uint kInputViewportOriginX;
uniform uint kInputViewportOriginY;
uniform uint kInputViewportWidth;
uniform uint kInputViewportHeight;
uniform uint kOutputViewportOriginX;
uniform uint kOutputViewportOriginY;
uniform uint kOutputViewportWidth;
uniform uint kOutputViewportHeight;
uniform float reserved0;
uniform float reserved1;

//--include--NISScaler.h--//


void main()
{
    NVScaler(gl_WorkGroupID.xy, gl_LocalInvocationID.x);
}