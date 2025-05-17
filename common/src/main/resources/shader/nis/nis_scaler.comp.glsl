#version 450
#extension GL_ARB_shading_language_420pack: enable
#extension GL_GOOGLE_include_directive: enable

#define NIS_THREAD_GROUP_SIZE 256
#define NIS_BLOCK_WIDTH 32
#define NIS_BLOCK_HEIGHT 24
#define kPhaseCount  64
#define kFilterSize  6
#define kSupportSize 6
#define kPadSize     kSupportSize
#define kTilePitch (NIS_BLOCK_WIDTH + kPadSize)
#define kTileSize (kTilePitch * (NIS_BLOCK_HEIGHT + kPadSize))
#define kEdgeMapPitch (NIS_BLOCK_WIDTH + 2)
#define kEdgeMapSize (kEdgeMapPitch * (NIS_BLOCK_HEIGHT + 2))

layout (std140, binding = 0) uniform const_buffer
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

layout (binding = 2) uniform sampler2D in_texture;
layout (binding = 3) uniform writeonly image2D out_texture;

layout (binding = 4) uniform sampler2D coef_scaler;
layout (binding = 5) uniform sampler2D coef_usm;

shared float shPixelsY[kTileSize];
shared float shCoefScaler[kPhaseCount][kFilterSize];
shared float shCoefUSM[kPhaseCount][kFilterSize];
shared vec4 shEdgeMap[kEdgeMapSize];

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

vec4 GetEdgeMap(float p[4][4], int i, int j)

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

void LoadFilterBanksSh(int i0, int di) {
    for (int i = i0; i < kPhaseCount * 2; i += di)
    {
        int phase = i >> 1;
        int vIdx = i & 1;

        vec4 v = vec4(texelFetch(coef_scaler, ivec2(vIdx, phase), 0));
        int filterOffset = vIdx * 4;
        shCoefScaler[phase][filterOffset + 0] = v.x;
        shCoefScaler[phase][filterOffset + 1] = v.y;
        if (vIdx == 0)
        {
            shCoefScaler[phase][2] = v.z;
            shCoefScaler[phase][3] = v.w;
        }

        v = vec4(texelFetch(coef_usm, ivec2(vIdx, phase), 0));
        shCoefUSM[phase][filterOffset + 0] = v.x;
        shCoefUSM[phase][filterOffset + 1] = v.y;
        if (vIdx == 0)
        {
            shCoefUSM[phase][2] = v.z;
            shCoefUSM[phase][3] = v.w;
        }
    }
}

float CalcLTI(float p0, float p1, float p2, float p3, float p4, float p5, int phase_index)
{
    const bool selector = (phase_index <= 64 / 2);
    float sel = selector ? p0 : p3;
    const float a_min = min(min(p1, p2), sel);
    const float a_max = max(max(p1, p2), sel);
    sel = selector ? p2 : p5;
    const float b_min = min(min(p3, p4), sel);
    const float b_max = max(max(p3, p4), sel);

    const float a_cont = a_max - a_min;
    const float b_cont = b_max - b_min;

    const float cont_ratio = max(a_cont, b_cont) / (min(a_cont, b_cont) + kEps);
    return (1.0f - clamp((cont_ratio - kMinContrastRatio) * kRatioNorm, 0, 1)) * kContrastBoost;
}

vec4 GetInterpEdgeMap(const vec4 edge[2][2], float phase_frac_x, float phase_frac_y)
{
    vec4 h0 = mix(edge[0][0], edge[0][1], phase_frac_x);
    vec4 h1 = mix(edge[1][0], edge[1][1], phase_frac_x);
    return mix(h0, h1, phase_frac_y);
}

float EvalPoly6(const float pxl[6], int phase_int)
{
    float y = 0.f;
    {
        for (int i = 0; i < 6; ++i)
        {
            y += shCoefScaler[phase_int][i] * pxl[i];
        }
    }
    float y_usm = 0.f;
    {
        for (int i = 0; i < 6; ++i)
        {
            y_usm += shCoefUSM[phase_int][i] * pxl[i];
        }
    }

    const float y_scale = 1.0f - clamp((y * (1.0f / float(1.f)) - kSharpStartY) * kSharpScaleY, 0, 1);

    const float y_sharpness = y_scale * kSharpStrengthScale + kSharpStrengthMin;

    y_usm *= y_sharpness;

    const float y_sharpness_limit = (y_scale * kSharpLimitScale + kSharpLimitMin) * y;

    y_usm = min(y_sharpness_limit, max(-y_sharpness_limit, y_usm));

    y_usm *= CalcLTI(pxl[0], pxl[1], pxl[2], pxl[3], pxl[4], pxl[5], phase_int);

    return y + y_usm;
}

float FilterNormal(const float p[6][6], int phase_x_frac_int, int phase_y_frac_int)
{
    float h_acc = 0.0f;

    for (int j = 0; j < 6; ++j)
    {
        float v_acc = 0.0f;

        for (int i = 0; i < 6; ++i)
        {
            v_acc += p[i][j] * shCoefScaler[phase_y_frac_int][i];
        }
        h_acc += v_acc * shCoefScaler[phase_x_frac_int][j];
    }

    return h_acc;
}

float AddDirFilters(float p[6][6], float phase_x_frac, float phase_y_frac, int phase_x_frac_int, int phase_y_frac_int, vec4 w)
{
    float f = 0;
    if (w.x > 0.0f)
    {
        float interp0Deg[6];
        {
            for (int i = 0; i < 6; ++i)
            {
                interp0Deg[i] = mix(p[i][2], p[i][3], phase_x_frac);
            }
        }
        f += EvalPoly6(interp0Deg, phase_y_frac_int) * w.x;
    }
    if (w.y > 0.0f)
    {
        float interp90Deg[6];
        {
            for (int i = 0; i < 6; ++i)
            {
                interp90Deg[i] = mix(p[2][i], p[3][i], phase_y_frac);
            }
        }

        f += EvalPoly6(interp90Deg, phase_x_frac_int) * w.y;
    }
    if (w.z > 0.0f)
    {
        float pphase_b45 = 0.5f + 0.5f * (phase_x_frac - phase_y_frac);

        float temp_interp45Deg[7];
        temp_interp45Deg[1] = mix(p[2][1], p[1][2], pphase_b45);
        temp_interp45Deg[3] = mix(p[3][2], p[2][3], pphase_b45);
        temp_interp45Deg[5] = mix(p[4][3], p[3][4], pphase_b45);
        {
            pphase_b45 = pphase_b45 - 0.5f;
            float a = (pphase_b45 >= 0.f) ? p[0][2] : p[2][0];
            float b = (pphase_b45 >= 0.f) ? p[1][3] : p[3][1];
            float c = (pphase_b45 >= 0.f) ? p[2][4] : p[4][2];
            float d = (pphase_b45 >= 0.f) ? p[3][5] : p[5][3];
            temp_interp45Deg[0] = mix(p[1][1], a, abs(pphase_b45));
            temp_interp45Deg[2] = mix(p[2][2], b, abs(pphase_b45));
            temp_interp45Deg[4] = mix(p[3][3], c, abs(pphase_b45));
            temp_interp45Deg[6] = mix(p[4][4], d, abs(pphase_b45));
        }

        float interp45Deg[6];
        float pphase_p45 = phase_x_frac + phase_y_frac;
        if (pphase_p45 >= 1)
        {
            for (int i = 0; i < 6; i++)
            {
                interp45Deg[i] = temp_interp45Deg[i + 1];
            }
            pphase_p45 = pphase_p45 - 1;
        }
        else
        {
            for (int i = 0; i < 6; i++)
            {
                interp45Deg[i] = temp_interp45Deg[i];
            }
        }

        f += EvalPoly6(interp45Deg, int(pphase_p45 * 64)) * w.z;
    }
    if (w.w > 0.0f)
    {
        float pphase_b135 = 0.5f * (phase_x_frac + phase_y_frac);

        float temp_interp135Deg[7];
        temp_interp135Deg[1] = mix(p[3][1], p[4][2], pphase_b135);
        temp_interp135Deg[3] = mix(p[2][2], p[3][3], pphase_b135);
        temp_interp135Deg[5] = mix(p[1][3], p[2][4], pphase_b135);
        {
            pphase_b135 = pphase_b135 - 0.5f;
            float a = (pphase_b135 >= 0.f) ? p[5][2] : p[3][0];
            float b = (pphase_b135 >= 0.f) ? p[4][3] : p[2][1];
            float c = (pphase_b135 >= 0.f) ? p[3][4] : p[1][2];
            float d = (pphase_b135 >= 0.f) ? p[2][5] : p[0][3];
            temp_interp135Deg[0] = mix(p[4][1], a, abs(pphase_b135));
            temp_interp135Deg[2] = mix(p[3][2], b, abs(pphase_b135));
            temp_interp135Deg[4] = mix(p[2][3], c, abs(pphase_b135));
            temp_interp135Deg[6] = mix(p[1][4], d, abs(pphase_b135));
        }

        float interp135Deg[6];
        float pphase_p135 = 1 + (phase_x_frac - phase_y_frac);
        if (pphase_p135 >= 1)
        {
            for (int i = 0; i < 6; ++i)
            {
                interp135Deg[i] = temp_interp135Deg[i + 1];
            }
            pphase_p135 = pphase_p135 - 1;
        }
        else
        {
            for (int i = 0; i < 6; ++i)
            {
                interp135Deg[i] = temp_interp135Deg[i];
            }
        }

        f += EvalPoly6(interp135Deg, int(pphase_p135 * 64)) * w.w;
    }
    return f;
}

void NVScaler(uvec2 blockIdx, uint threadIdx)
{
    int dstBlockX = int(NIS_BLOCK_WIDTH * blockIdx.x);
    int dstBlockY = int(NIS_BLOCK_HEIGHT * blockIdx.y);

    const int srcBlockStartX = int(floor((dstBlockX + 0.5f) * kScaleX - 0.5f));
    const int srcBlockStartY = int(floor((dstBlockY + 0.5f) * kScaleY - 0.5f));
    const int srcBlockEndX = int(ceil((dstBlockX + NIS_BLOCK_WIDTH + 0.5f) * kScaleX - 0.5f));
    const int srcBlockEndY = int(ceil((dstBlockY + NIS_BLOCK_HEIGHT + 0.5f) * kScaleY - 0.5f));

    int numTilePixelsX = srcBlockEndX - srcBlockStartX + kSupportSize - 1;
    int numTilePixelsY = srcBlockEndY - srcBlockStartY + kSupportSize - 1;

    numTilePixelsX += numTilePixelsX & 0x1;
    numTilePixelsY += numTilePixelsY & 0x1;
    const int numTilePixels = numTilePixelsX * numTilePixelsY;

    const int numEdgeMapPixelsX = numTilePixelsX - kSupportSize + 2;
    const int numEdgeMapPixelsY = numTilePixelsY - kSupportSize + 2;
    const int numEdgeMapPixels = numEdgeMapPixelsX * numEdgeMapPixelsY;

    {
        for (uint i = threadIdx * 2; i < uint(numTilePixels) >> 1; i += NIS_THREAD_GROUP_SIZE * 2)
        {
            uint py = (i / numTilePixelsX) * 2;
            uint px = i % numTilePixelsX;

            float kShift = 0.5f - (kSupportSize - 1) / 2;

            const float tx = (srcBlockStartX + px + kShift) * kSrcNormX;
            const float ty = (srcBlockStartY + py + kShift) * kSrcNormY;

            float p[2][2];

            for (int j = 0; j < 2; j++)
            {
                for (int k = 0; k < 2; k++)
                {
                    const vec4 px = texture(in_texture, vec2(tx + k * kSrcNormX, ty + j * kSrcNormY));
                    p[j][k] = getY(px.xyz);
                }
            }

            const uint idx = py * kTilePitch + px;
            shPixelsY[idx] = float(p[0][0]);
            shPixelsY[idx + 1] = float(p[0][1]);
            shPixelsY[idx + kTilePitch] = float(p[1][0]);
            shPixelsY[idx + kTilePitch + 1] = float(p[1][1]);
        }
    }
    groupMemoryBarrier();
    barrier();
    {
        for (uint i = threadIdx * 2; i < uint(numEdgeMapPixels) >> 1; i += NIS_THREAD_GROUP_SIZE * 2)
        {
            uint py = (i / numEdgeMapPixelsX) * 2;
            uint px = i % numEdgeMapPixelsX;

            const uint edgeMapIdx = py * kEdgeMapPitch + px;

            uint tileCornerIdx = (py + 1) * kTilePitch + px + 1;
            float p[4][4];

            for (int j = 0; j < 4; j++)
            {
                for (int k = 0; k < 4; k++)
                {
                    p[j][k] = shPixelsY[tileCornerIdx + j * kTilePitch + k];
                }
            }

            shEdgeMap[edgeMapIdx] = vec4(GetEdgeMap(p, 0, 0));
            shEdgeMap[edgeMapIdx + 1] = vec4(GetEdgeMap(p, 0, 1));
            shEdgeMap[edgeMapIdx + kEdgeMapPitch] = vec4(GetEdgeMap(p, 1, 0));
            shEdgeMap[edgeMapIdx + kEdgeMapPitch + 1] = vec4(GetEdgeMap(p, 1, 1));
        }
    }
    LoadFilterBanksSh(int(threadIdx), NIS_THREAD_GROUP_SIZE);
    groupMemoryBarrier();
    barrier();

    const ivec2 pos = ivec2(uint(threadIdx) % uint(32), uint(threadIdx) / uint(32));

    const int dstX = dstBlockX + pos.x;

    const float srcX = (0.5f + dstX) * kScaleX - 0.5f;

    const int px = int(floor(srcX) - srcBlockStartX);

    const float fx = srcX - floor(srcX);

    const int fx_int = int(floor(fx * kPhaseCount + 0.5f));

    for (int k = 0; k < NIS_BLOCK_WIDTH * NIS_BLOCK_HEIGHT / NIS_THREAD_GROUP_SIZE; ++k)
    {
        const int dstY = dstBlockY + pos.y + k * (NIS_THREAD_GROUP_SIZE / NIS_BLOCK_WIDTH);

        const float srcY = (0.5f + dstY) * kScaleY - 0.5f;

        {
            const int py = int(floor(srcY) - srcBlockStartY);

            const float fy = srcY - floor(srcY);

            const int fy_int = int(floor(fy * kPhaseCount + 0.5f));
            const int startEdgeMapIdx = py * kEdgeMapPitch + px;
            vec4 edge[2][2];

            for (int i = 0; i < 2; i++)
            {
                for (int j = 0; j < 2; j++)
                {
                    edge[i][j] = shEdgeMap[startEdgeMapIdx + (i * kEdgeMapPitch) + j];
                }
            }
            const vec4 w = GetInterpEdgeMap(edge, fx, fy) * 1;

            const int startTileIdx = py * kTilePitch + px;
            float p[6][6];
            {
                for (int i = 0; i < 6; ++i)
                {
                    for (int j = 0; j < 6; ++j)
                    {
                        p[i][j] = shPixelsY[startTileIdx + i * kTilePitch + j];
                    }
                }
            }

            //const float baseWeight = float(1.f) - w.x - w.y - w.z - w.w;
            const float weightSum = w.x + w.y + w.z + w.w;
            const float baseWeight = (weightSum > 1e-5f) ? (1.0f - weightSum) : 1.0f;

            float opY = 0;

            opY += FilterNormal(p, fx_int, fy_int) * baseWeight;

            opY += AddDirFilters(p, fx, fy, fx_int, fy_int, w);

            vec2 coord = vec2((srcX + 0.5f) * kSrcNormX, (srcY + 0.5f) * kSrcNormY);
            vec2 dstCoord = vec2(dstX, dstY);

            vec4 op = texture(in_texture, coord);
            float y = getY(vec3(op.x, op.y, op.z));

            const float corr = opY * (1.0f / float(1.f)) - y;
            op.x += corr;
            op.y += corr;
            op.z += corr;

            imageStore(out_texture, ivec2(dstCoord), (op));
        }
    }
}

layout (local_size_x = NIS_THREAD_GROUP_SIZE) in;
void main()
{
    NVScaler(gl_WorkGroupID.xy, gl_LocalInvocationID.x);
}
