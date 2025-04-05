#version 450
#extension GL_ARB_shading_language_420pack : enable
#extension GL_GOOGLE_include_directive : enable

layout(std140, binding = 0) uniform const_buffer
{
    float kDetectRatio;
    float kDetectThres;
    float kMinContrastRatio;
    float kRatioNorm;

    float kContrastBoost;
    float kEps;
    float kSharpStartY;
    float kSharpScaleY;

    float kSharpStrengthMin;
    float kSharpStrengthScale;
    float kSharpLimitMin;
    float kSharpLimitScale;

    float kScaleX;
    float kScaleY;

    float kDstNormX;
    float kDstNormY;
    float kSrcNormX;
    float kSrcNormY;

    uint kInputViewportOriginX;
    uint kInputViewportOriginY;
    uint kInputViewportWidth;
    uint kInputViewportHeight;

    uint kOutputViewportOriginX;
    uint kOutputViewportOriginY;
    uint kOutputViewportWidth;
    uint kOutputViewportHeight;

    float reserved0;
    float reserved1;
};

layout(binding = 2) uniform sampler2D in_texture;
layout(binding = 3) uniform writeonly image2D out_texture;

#line 1 "NIS/NIS_Scaler.h"

float getY(vec3 rgba)
{
    return float(0.2126f) * rgba.x + float(0.7152f) * rgba.y + float(0.0722f) * rgba.z;
}

float getYLinear(vec3 rgba)
{
    return float(0.2126f) * rgba.x + float(0.7152f) * rgba.y + float(0.0722f) * rgba.z;
}

vec3 YUVtoRGB(vec3 yuv)
{
    float y = yuv.x - 16.0f / 255.0f;
    float u = yuv.y - 128.0f / 255.0f;
    float v = yuv.z - 128.0f / 255.0f;
    vec3 rgb;
    rgb.x = clamp(1.164f * y + 1.596f * v, 0, 1);
    rgb.y = clamp(1.164f * y - 0.392f * u - 0.813f * v, 0, 1);
    rgb.z = clamp(1.164f * y + 2.017f * u, 0, 1);
    return rgb;
}

vec4 GetEdgeMap(float p[5][5], int i, int j)

{
    const float g_0 = abs(p[0 + i][0 + j] + p[0 + i][1 + j] + p[0 + i][2 + j] - p[2 + i][0 + j] - p[2 + i][1 + j] - p[2 + i][2 + j]);
    const float g_45 = abs(p[1 + i][0 + j] + p[0 + i][0 + j] + p[0 + i][1 + j] - p[2 + i][1 + j] - p[2 + i][2 + j] - p[1 + i][2 + j]);
    const float g_90 = abs(p[0 + i][0 + j] + p[1 + i][0 + j] + p[2 + i][0 + j] - p[0 + i][2 + j] - p[1 + i][2 + j] - p[2 + i][2 + j]);
    const float g_135 = abs(p[1 + i][0 + j] + p[2 + i][0 + j] + p[2 + i][1 + j] - p[0 + i][1 + j] - p[0 + i][2 + j] - p[1 + i][2 + j]);

    const float g_0_90_max = max(g_0, g_90);
    const float g_0_90_min = min(g_0, g_90);
    const float g_45_135_max = max(g_45, g_135);
    const float g_45_135_min = min(g_45, g_135);

    float e_0_90 = 0;
    float e_45_135 = 0;

    if (g_0_90_max + g_45_135_max == 0)
    {
        return vec4(0, 0, 0, 0);
    }

    e_0_90 = min(g_0_90_max / (g_0_90_max + g_45_135_max), 1.0f);
    e_45_135 = 1.0f - e_0_90;

    bool c_0_90 = (g_0_90_max > (g_0_90_min * kDetectRatio)) && (g_0_90_max > kDetectThres) && (g_0_90_max > g_45_135_min);
    bool c_45_135 = (g_45_135_max > (g_45_135_min * kDetectRatio)) && (g_45_135_max > kDetectThres) && (g_45_135_max > g_0_90_min);
    bool c_g_0_90 = g_0_90_max == g_0;
    bool c_g_45_135 = g_45_135_max == g_45;

    float f_e_0_90 = (c_0_90 && c_45_135) ? e_0_90 : 1.0f;
    float f_e_45_135 = (c_0_90 && c_45_135) ? e_45_135 : 1.0f;

    float weight_0 = (c_0_90 && c_g_0_90) ? f_e_0_90 : 0.0f;
    float weight_90 = (c_0_90 && !c_g_0_90) ? f_e_0_90 : 0.0f;
    float weight_45 = (c_45_135 && c_g_45_135) ? f_e_45_135 : 0.0f;
    float weight_135 = (c_45_135 && !c_g_45_135) ? f_e_45_135 : 0.0f;

    return vec4(weight_0, weight_90, weight_45, weight_135);
}

shared float shPixelsY[(32 + 5 + 1)][(32 + 5 + 1)];

float CalcLTIFast(const float y[5])
{
    const float a_min = min(min(y[0], y[1]), y[2]);
    const float a_max = max(max(y[0], y[1]), y[2]);

    const float b_min = min(min(y[2], y[3]), y[4]);
    const float b_max = max(max(y[2], y[3]), y[4]);

    const float a_cont = a_max - a_min;
    const float b_cont = b_max - b_min;

    const float cont_ratio = max(a_cont, b_cont) / (min(a_cont, b_cont) + kEps);
    return (1.0f - clamp((cont_ratio - kMinContrastRatio) * kRatioNorm, 0, 1)) * kContrastBoost;
}

float EvalUSM(const float pxl[5], const float sharpnessStrength, const float sharpnessLimit)
{
    float y_usm = -0.6001f * pxl[1] + 1.2002f * pxl[2] - 0.6001f * pxl[3];

    y_usm *= sharpnessStrength;

    y_usm = min(sharpnessLimit, max(-sharpnessLimit, y_usm));

    y_usm *= CalcLTIFast(pxl);

    return y_usm;
}

vec4 GetDirUSM(const float p[5][5])
{
    const float scaleY = 1.0f - clamp((p[2][2] - kSharpStartY) * kSharpScaleY, 0, 1);

    const float sharpnessStrength = scaleY * kSharpStrengthScale + kSharpStrengthMin;

    const float sharpnessLimit = (scaleY * kSharpLimitScale + kSharpLimitMin) * p[2][2];

    vec4 rval;

    float interp0Deg[5];
    {
        for (int i = 0; i < 5; ++i)
        {
            interp0Deg[i] = p[i][2];
        }
    }

    rval.x = EvalUSM(interp0Deg, sharpnessStrength, sharpnessLimit);

    float interp90Deg[5];
    {
        for (int i = 0; i < 5; ++i)
        {
            interp90Deg[i] = p[2][i];
        }
    }

    rval.y = EvalUSM(interp90Deg, sharpnessStrength, sharpnessLimit);

    float interp45Deg[5];
    interp45Deg[0] = p[1][1];
    interp45Deg[1] = mix(p[2][1], p[1][2], 0.5f);
    interp45Deg[2] = p[2][2];
    interp45Deg[3] = mix(p[3][2], p[2][3], 0.5f);
    interp45Deg[4] = p[3][3];

    rval.z = EvalUSM(interp45Deg, sharpnessStrength, sharpnessLimit);

    float interp135Deg[5];
    interp135Deg[0] = p[3][1];
    interp135Deg[1] = mix(p[3][2], p[2][1], 0.5f);
    interp135Deg[2] = p[2][2];
    interp135Deg[3] = mix(p[2][3], p[1][2], 0.5f);
    interp135Deg[4] = p[1][3];

    rval.w = EvalUSM(interp135Deg, sharpnessStrength, sharpnessLimit);
    return rval;
}

void NVSharpen(uvec2 blockIdx, uint threadIdx)
{
    const int dstBlockX = int(32 * blockIdx.x);
    const int dstBlockY = int(32 * blockIdx.y);

    const float kShift = 0.5f - 5 / 2;

    for (int i = int(threadIdx) * 2; i < (32 + 5 + 1) * (32 + 5 + 1) / 2; i += 256 * 2)
    {
        uvec2 pos = uvec2(uint(i) % uint((32 + 5 + 1)), uint(i) / uint((32 + 5 + 1)) * 2);

        for (int dy = 0; dy < 2; dy++)
        {
            for (int dx = 0; dx < 2; dx++)
            {
                const float tx = (dstBlockX + pos.x + dx + kShift) * kSrcNormX;
                const float ty = (dstBlockY + pos.y + dy + kShift) * kSrcNormY;

                const vec4 px = textureLod(in_texture, vec2(tx, ty), 0);
                shPixelsY[pos.y + dy][pos.x + dx] = getY(px.xyz);
            }
        }
    }

    groupMemoryBarrier();
    barrier();

    for (int k = int(threadIdx); k < 32 * 32; k += 256)
    {
        const ivec2 pos = ivec2(uint(k) % uint(32), uint(k) / uint(32));

        float p[5][5];

        for (int i = 0; i < 5; ++i)
        {
            for (int j = 0; j < 5; ++j)
            {
                p[i][j] = shPixelsY[pos.y + i][pos.x + j];
            }
        }

        vec4 dirUSM = GetDirUSM(p);

        vec4 w = GetEdgeMap(p, 5 / 2 - 1, 5 / 2 - 1);

        const float usmY = (dirUSM.x * w.x + dirUSM.y * w.y + dirUSM.z * w.z + dirUSM.w * w.w);

        const int dstX = dstBlockX + pos.x;
        const int dstY = dstBlockY + pos.y;

        vec2 coord = vec2((dstX + 0.5f) * kSrcNormX, (dstY + 0.5f) * kSrcNormY);
        vec2 dstCoord = vec2(dstX, dstY);

        {
            vec4 op = textureLod(in_texture, coord, 0);

            op.x += usmY;
            op.y += usmY;
            op.z += usmY;

            imageStore(out_texture, ivec2(dstCoord), (op));
        }
    }
}

#line 86 "NIS\NIS_Main.comp.glsl"

layout(local_size_x = 256) in;
void main()
{
    NVSharpen(gl_WorkGroupID.xy, gl_LocalInvocationID.x);
}
