#version 430 core

layout(local_size_x = 16, local_size_y = 16, local_size_z = 1) in;

layout(binding = 0) uniform sampler2D inputColor;
layout(binding = 1, COLOR_FORMAT) uniform writeonly image2D outputColor;

#ifdef HAS_DEPTH
layout(binding = 2) uniform sampler2D inputDepth;
layout(binding = 3, r32f) uniform writeonly image2D outputDepth;
#endif

#ifdef HAS_MOTION_VECTOR
layout(binding = 4) uniform sampler2D inputMotionVectors;
layout(binding = 5, rg16f) uniform writeonly image2D outputMotionVectors;
#endif

#ifdef HAS_EXPOSURE
layout(binding = 6) uniform sampler2D inputExposure;
layout(binding = 7, r16f) uniform writeonly image2D outputExposure;
#endif

void main() {
    ivec2 texelCoord = ivec2(gl_GlobalInvocationID.xy);

    ivec2 colorSize = imageSize(outputColor);
    if (texelCoord.x < colorSize.x && texelCoord.y < colorSize.y) {
        int flippedY = colorSize.y - 1 - texelCoord.y;

        vec4 color = texelFetch(inputColor, ivec2(texelCoord.x, flippedY), 0);
        imageStore(outputColor, texelCoord, color);
        
        #ifdef HAS_DEPTH
        vec4 depth = texelFetch(inputDepth, ivec2(texelCoord.x, flippedY), 0);
        imageStore(outputDepth, texelCoord, depth);
        #endif

        #ifdef HAS_MOTION_VECTOR
        vec2 mv = texelFetch(inputMotionVectors, ivec2(texelCoord.x, flippedY), 0).rg;
        mv.y = -mv.y;
        imageStore(outputMotionVectors, texelCoord, vec4(mv, 0.0, 0.0));
        #endif
    }

    #ifdef HAS_EXPOSURE
    if (texelCoord.x == 0 && texelCoord.y == 0) {
        float exposure = texelFetch(inputExposure, ivec2(0, 0), 0).r;
        imageStore(outputExposure, ivec2(0, 0), vec4(exposure, 0.0, 0.0, 0.0));
    }
    #endif
}
