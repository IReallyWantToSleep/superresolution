#version 430 core

layout(local_size_x = 16, local_size_y = 16, local_size_z = 1) in;

layout(binding = 0) uniform sampler2D inputMotionVectors;
layout(binding = 1, rg16f) uniform writeonly image2D outputMotionVectors;

// MOTION_VECTOR_PREPROCESSING_FUNCTION_PLACEHOLDER

void main() {
    ivec2 texelCoord = ivec2(gl_GlobalInvocationID.xy);
    ivec2 texSize = imageSize(outputMotionVectors);
    if (texelCoord.x < texSize.x && texelCoord.y < texSize.y) {
        vec2 mv = texelFetch(inputMotionVectors, texelCoord, 0).rg;
        #ifdef MOTION_VECTOR_PREPROCESSING_FUNCTION_INJECTED
        mv = motionVectorPreprocessing(mv);
        #endif
        imageStore(outputMotionVectors, texelCoord, vec4(mv, 0.0, 0.0));
    }
}
