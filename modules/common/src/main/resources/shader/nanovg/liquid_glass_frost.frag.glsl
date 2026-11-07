#version 460

layout(binding = 2) uniform sampler2D sourceTexture;

layout(std140, binding = 1) uniform glass {
    vec4 copyRect;
    vec2 textureSize;
    vec2 shapeSize;
    vec4 radii;
    vec4 colorControls;
    vec4 params;
    vec4 optics;
    vec4 lighting;
    vec4 shapeAxes;
    vec4 surfaceTint;
    vec4 material;
};

#if FROST_PASS != 0
layout(std140, binding = 3) uniform kernel {
    vec4 kernelMeta;
    vec4 kernelPairs[3];
};
#endif

layout(location = 0) in vec2 position;
layout(location = 0) out vec4 outColor;

vec4 sampleSource(vec2 p) {
    #if FROST_PASS == 0
    vec2 size = textureSize;
    vec2 minimum = copyRect.xy + vec2(0.5);
    vec2 maximum = copyRect.xy + copyRect.zw - vec2(0.5);
    #else
    vec2 size = ceil(textureSize / 2.0);
    vec2 minimum = floor(copyRect.xy / params.z) + vec2(0.5);
    vec2 maximum = ceil((copyRect.xy + copyRect.zw) / params.z) - vec2(0.5);
    #endif
    vec2 uv = clamp(p, minimum, maximum) / size;
    if (params.w > 0.5) {
        uv.y = 1.0 - uv.y;
    }
    return texture(sourceTexture, uv);
}

const int kMaxBox = 10;

vec4 frost(vec2 p) {
    #if FROST_PASS == 0
    float boxSize = params.z;
    vec2 origin = p * boxSize - vec2(boxSize * 0.5);
    vec4 sum = vec4(0.0);
    float count = 0.0;
    for (int y = 0; y < kMaxBox; y++) {
        if (float(y) * 2.0 >= boxSize) {
            break;
        }
        for (int x = 0; x < kMaxBox; x++) {
            if (float(x) * 2.0 >= boxSize) {
                break;
            }
            sum += sampleSource(origin + vec2(float(x), float(y)) * 2.0 + vec2(1.0));
            count += 1.0;
        }
    }
    return sum / float(max(count, 1.0));
    #else
    #if FROST_PASS == 1
    vec2 direction = vec2(1.0, 0.0);
    #else
    vec2 direction = vec2(0.0, 1.0);
    #endif
    vec4 sum = sampleSource(p) * kernelMeta.x;
    for (int i = 0; i < 6; i++) {
        if (float(i) >= kernelMeta.y) {
            break;
        }
        vec4 pairs = kernelPairs[i / 2];
        vec2 pair = i % 2 == 0 ? pairs.xy : pairs.zw;
        vec2 offset = direction * pair.y;
        sum += (sampleSource(p + offset) + sampleSource(p - offset)) * pair.x;
    }
    return sum;
    #endif
}

void main() {
    outColor = frost(position);
}
