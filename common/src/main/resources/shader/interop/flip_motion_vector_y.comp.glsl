#version 430 core

layout(local_size_x = 16, local_size_y = 16, local_size_z = 1) in;

layout(binding = 0) uniform sampler2D inputMotionVector;
layout(binding = 1, rg16f) uniform writeonly image2D outputMotionVector;

void main() {
    ivec2 texelCoord = ivec2(gl_GlobalInvocationID.xy);
    ivec2 texSize = imageSize(outputMotionVector);
    if (texelCoord.x >= texSize.x || texelCoord.y >= texSize.y) {
        return;
    }
    int flippedY = texSize.y - 1 - texelCoord.y;
    ivec2 flippedCoord = ivec2(texelCoord.x, flippedY);
    vec2 motionVector = texelFetch(inputMotionVector, flippedCoord, 0).rg;
    motionVector.y = -motionVector.y;
    imageStore(outputMotionVector, texelCoord, vec4(motionVector, 0.0, 0.0));
}
