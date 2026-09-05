#version 430

// This is the reversible AgX Log (Kraken) encoding, not AgX's final display
// contrast/look LUT. The latter belongs to the game's HDR presentation path and
// cannot be safely duplicated here without its exact look and exposure settings.
layout(local_size_x = 16, local_size_y = 16) in;
layout(binding = 0) uniform sampler2D sourceColor;
#if defined(FINISH_HDR) || defined(FINISH_SDR)
layout(binding = 1) uniform sampler2D originalColor;
layout(location = 0) uniform float colorStrength;
layout(location = 1) uniform float highlightProtection;
layout(location = 2) uniform float highlightProtectionThreshold;
#endif
layout(binding = 0, OUTPUT_FORMAT) uniform writeonly image2D destinationColor;

vec3 linearToOklab(vec3 color) {
    float l = 0.4122214708 * color.r + 0.5363325363 * color.g + 0.0514459929 * color.b;
    float m = 0.2119034982 * color.r + 0.6806995451 * color.g + 0.1073969566 * color.b;
    float s = 0.0883024619 * color.r + 0.2817188376 * color.g + 0.6299787005 * color.b;
    vec3 lms = sign(vec3(l, m, s)) * pow(abs(vec3(l, m, s)), vec3(1.0 / 3.0));
    return vec3(
        0.2104542553 * lms.x + 0.7936177850 * lms.y - 0.0040720468 * lms.z,
        1.9779984951 * lms.x - 2.4285922050 * lms.y + 0.4505937099 * lms.z,
        0.0259040371 * lms.x + 0.7827717662 * lms.y - 0.8086757660 * lms.z
    );
}

vec3 oklabToLinear(vec3 lab) {
    float l = lab.x + 0.3963377774 * lab.y + 0.2158037573 * lab.z;
    float m = lab.x - 0.1055613458 * lab.y - 0.0638541728 * lab.z;
    float s = lab.x - 0.0894841775 * lab.y - 1.2914855480 * lab.z;
    l = l * l * l;
    m = m * m * m;
    s = s * s * s;
    return vec3(
        4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s,
       -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s,
       -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s
    );
}

vec3 applyColorTransfer(vec3 original, vec3 neural, float strength) {
    if (abs(strength - 1.0) < 0.0001) {
        return neural;
    }
    vec3 originalLab = linearToOklab(max(original, vec3(0.0)));
    vec3 neuralLab = linearToOklab(max(neural, vec3(0.0)));
    neuralLab.yz = mix(originalLab.yz, neuralLab.yz, strength);
    return max(oklabToLinear(neuralLab), vec3(0.0));
}

void main() {
    ivec2 pixel = ivec2(gl_GlobalInvocationID.xy);
    if (any(greaterThanEqual(pixel, imageSize(destinationColor)))) {
        return;
    }
    vec4 color = texelFetch(sourceColor, pixel, 0);
#if defined(COMPRESS_HDR)
    const mat3 agxInset = mat3(
        0.842479062253094, 0.0423282422610123, 0.0423756549057051,
        0.0784335999999992, 0.878468636469772, 0.0784336,
        0.0792237451477643, 0.0791661274605434, 0.879142973793104
    );
    color.rgb = max(agxInset * max(color.rgb, vec3(0.0)), vec3(1e-6));
    color.rgb = clamp(log2(color.rgb), vec3(-12.47393), vec3(4.026069));
    color.rgb = (color.rgb + 12.47393) / (4.026069 + 12.47393);
#elif defined(PREPARE_LINEAR)
    color = vec4(max(color.rgb, vec3(0.0)), 1.0);
#elif defined(FINISH_HDR)
    const mat3 agxOutset = mat3(
        1.19687900512017, -0.0528968517574562, -0.0529716355144438,
        -0.0980208811401368, 1.15190312990417, -0.0980434501171241,
        -0.0990297440797205, -0.0989611768448433, 1.15107367264116
    );
    color.rgb = clamp(color.rgb, vec3(0.0), vec3(1.0));
    color.rgb = exp2(color.rgb * (4.026069 + 12.47393) - 12.47393);
    color.rgb = max(agxOutset * color.rgb, vec3(0.0));
#endif
#if defined(FINISH_HDR) || defined(FINISH_SDR)
    vec4 original = texelFetch(originalColor, pixel, 0);
    color.rgb = applyColorTransfer(original.rgb, color.rgb, colorStrength);
    const vec3 luminanceWeights = vec3(0.2126, 0.7152, 0.0722);
    float originalLuminance = dot(max(original.rgb, vec3(0.0)), luminanceWeights);
    float highlightMask = smoothstep(
        highlightProtectionThreshold,
        highlightProtectionThreshold + 0.35,
        originalLuminance
    ) * highlightProtection;
    color.rgb = mix(color.rgb, original.rgb, highlightMask);
    color.a = original.a;
#endif
    imageStore(destinationColor, pixel, color);
}
