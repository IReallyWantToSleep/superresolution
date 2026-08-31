#version 430

// This is the reversible AgX Log (Kraken) encoding, not AgX's final display
// contrast/look LUT. The latter belongs to the game's HDR presentation path and
// cannot be safely duplicated here without its exact look and exposure settings.
layout(local_size_x = 16, local_size_y = 16) in;
layout(binding = 0) uniform sampler2D sourceColor;
layout(binding = 0, OUTPUT_FORMAT) uniform writeonly image2D destinationColor;

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
#elif defined(EXPAND_HDR)
    const mat3 agxOutset = mat3(
        1.19687900512017, -0.0528968517574562, -0.0529716355144438,
        -0.0980208811401368, 1.15190312990417, -0.0980434501171241,
        -0.0990297440797205, -0.0989611768448433, 1.15107367264116
    );
    color.rgb = clamp(color.rgb, vec3(0.0), vec3(1.0));
    color.rgb = exp2(color.rgb * (4.026069 + 12.47393) - 12.47393);
    color.rgb = max(agxOutset * color.rgb, vec3(0.0));
#endif
    imageStore(destinationColor, pixel, color);
}
