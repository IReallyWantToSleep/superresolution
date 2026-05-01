#version 460

layout(binding = 2) uniform sampler2D tex;

layout(std140, binding = 1) uniform frag {
    mat3 scissorMat;
    mat3 paintMat;
    vec4 innerCol;
    vec4 outerCol;
    vec2 scissorExt;
    vec2 scissorScale;
    vec2 extent;
    float radius;
    float feather;
    float strokeMult;
    float strokeThr;
    int texType;
    int type;
};

layout(location = 0) in vec2 ftcoord;
layout(location = 1) in vec2 fpos;
layout(location = 0) out vec4 outColor;

float sdroundrect(vec2 pt, vec2 ext, float rad) {
    vec2 ext2 = ext - vec2(rad, rad);
    vec2 d = abs(pt) - ext2;
    return min(max(d.x, d.y), 0.0) + length(max(d, 0.0)) - rad;
}

float scissorMask(vec2 p) {
    vec2 sc = (abs((scissorMat * vec3(p, 1.0)).xy) - scissorExt);
    sc = vec2(0.5, 0.5) - sc * scissorScale;
    return clamp(sc.x, 0.0, 1.0) * clamp(sc.y, 0.0, 1.0);
}

float strokeMask() {
    return min(1.0, (1.0 - abs(ftcoord.x * 2.0 - 1.0)) * strokeMult) * min(1.0, ftcoord.y);
}

void main() {
    vec4 result;
    float scissor = scissorMask(fpos);
    float strokeAlpha = strokeMask();
    if (strokeAlpha < strokeThr) {
        discard;
    }

    if (type == 0) {
        vec2 pt = (paintMat * vec3(fpos, 1.0)).xy;
        float d = clamp((sdroundrect(pt, extent, radius) + feather * 0.5) / feather, 0.0, 1.0);
        vec4 color = mix(innerCol, outerCol, d);
        color *= strokeAlpha * scissor;
        result = color;
    } else if (type == 1) {
        vec2 pt = (paintMat * vec3(fpos, 1.0)).xy / extent;
        vec4 color = texture(tex, pt);
        if (texType == 1) color = vec4(color.xyz * color.w, color.w);
        if (texType == 2) color = vec4(color.x);
        color *= innerCol;
        color *= strokeAlpha * scissor;
        result = color;
    } else if (type == 2) {
        result = vec4(1.0, 1.0, 1.0, 1.0);
    } else {
        vec4 color = texture(tex, ftcoord);
        if (texType == 1) color = vec4(color.xyz * color.w, color.w);
        if (texType == 2) color = vec4(color.x);
        color *= scissor;
        result = color * innerCol;
    }
    outColor = result;
}
