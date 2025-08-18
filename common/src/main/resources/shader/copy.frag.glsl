#version 410
#ifndef SR_GL41_COMPAT
#extension GL_ARB_shading_language_420pack : enable
#extension GL_ARB_explicit_uniform_location : enable
#endif

precision mediump float;

#if COPY_CHANCEL == 1
    #define COPY_DATA float
#elif COPY_CHANCEL == 2
    #define COPY_DATA vec2
#elif COPY_CHANCEL == 3
    #define COPY_DATA vec3
#elif COPY_CHANCEL == 4
    #define COPY_DATA vec4
#else
    #error "Invalid COPY_CHANCEL"
#endif

#define ZERO_COPY_DATA COPY_DATA(0)

layout(location = 0) uniform sampler2D tex;
layout(location = 0) in vec2 vTexCoord;
layout(location = 0) out COPY_DATA outTex;

float getComponent(COPY_DATA d, int idx) {
#if COPY_CHANCEL >= 1
    if (idx == 0) return d.r;
#endif
#if COPY_CHANCEL >= 2
    if (idx == 1) return d.g;
#endif
#if COPY_CHANCEL >= 3
    if (idx == 2) return d.b;
#endif
#if COPY_CHANCEL >= 4
    if (idx == 3) return d.a;
#endif
    return 0.0;
}

COPY_DATA setComponent(COPY_DATA d, int idx, float v) {
#if COPY_CHANCEL >= 1
    if (idx == 0) d.r = v;
#endif
#if COPY_CHANCEL >= 2
    if (idx == 1) d.g = v;
#endif
#if COPY_CHANCEL >= 3
    if (idx == 2) d.b = v;
#endif
#if COPY_CHANCEL >= 4
    if (idx == 3) d.a = v;
#endif
    return d;
}

void main() {
    COPY_DATA srcData;
    #if COPY_CHANCEL == 1
        srcData = texture(tex, vTexCoord).r;
    #elif COPY_CHANCEL == 2
        srcData = texture(tex, vTexCoord).rg;
    #elif COPY_CHANCEL == 3
        srcData = texture(tex, vTexCoord).rgb;
    #elif COPY_CHANCEL == 4
        srcData = texture(tex, vTexCoord).rgba;
    #endif

    COPY_DATA dstData = ZERO_COPY_DATA;

    #if defined(COPY_SRC_CHANCEL0) && defined(COPY_DST_CHANCEL0)
        dstData = setComponent(dstData, COPY_DST_CHANCEL0, getComponent(srcData, COPY_SRC_CHANCEL0));
    #endif

    #if defined(COPY_SRC_CHANCEL1) && defined(COPY_DST_CHANCEL1)
        dstData = setComponent(dstData, COPY_DST_CHANCEL1, getComponent(srcData, COPY_SRC_CHANCEL1));
    #endif

    #if defined(COPY_SRC_CHANCEL2) && defined(COPY_DST_CHANCEL2)
        dstData = setComponent(dstData, COPY_DST_CHANCEL2, getComponent(srcData, COPY_SRC_CHANCEL2));
    #endif

    #if defined(COPY_SRC_CHANCEL3) && defined(COPY_DST_CHANCEL3)
        dstData = setComponent(dstData, COPY_DST_CHANCEL3, getComponent(srcData, COPY_SRC_CHANCEL3));
    #endif

    outTex = dstData;
}
