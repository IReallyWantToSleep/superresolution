#version 330 compatibility
/* RENDERTARGETS: 0,1,2 */
#ifndef RG16F
const int RG16F = 33327;
#endif
#ifndef R32F
const int R32F = 33326;
#endif
const int colortex1Format = RG16F;
const int colortex2Format = R32F;
layout(location = 0) out vec4 color;
layout(location = 1) out vec2 motionVectors;
layout(location = 2) out float depth;
void main() {
    motionVectors = vec2(0.0);
    depth = 0.5;
#if defined(SR_IRIS_VULKAN) && SR_ENABLE
    if (SR_SHOULD_APPLY_JITTER != SR_ALGO_SUPPORTS_JITTER || (SR_USING_ALGO == SR_ALGO_FSR1 && SR_ALGO_SUPPORTS_JITTER != 0)
        || ((SR_USING_ALGO == SR_ALGO_DLSS || SR_USING_ALGO == SR_ALGO_FSR) && SR_ALGO_SUPPORTS_JITTER != 1)
        || abs(float(SR_SCALED_WIDTH) - SR_SCREEN_WIDTH * SR_RENDER_SCALE_FACTOR) > 1.0
        || abs(float(SR_SCALED_HEIGHT) - SR_SCREEN_HEIGHT * SR_RENDER_SCALE_FACTOR) > 1.0) {
        color = vec4(0.0, 1.0, 1.0, 1.0);
        return;
    }
#endif
    vec2 coord = (gl_FragCoord.xy - vec2(7, 11)) / vec2(376, 235);
    color = all(greaterThanEqual(coord, vec2(0))) && all(lessThan(coord, vec2(1)))
        ? vec4(coord, 0.25, 1.0) : vec4(1.0, 0.0, 1.0, 1.0);
}
