#version 430 core

layout(local_size_x = 16, local_size_y = 16, local_size_z = 1) in;

layout(binding = 0) uniform sampler2D inputTexture;
layout(binding = 1, OUTPUT_FORMAT) uniform writeonly image2D outputTexture;

void main() {
    ivec2 texelCoord = ivec2(gl_GlobalInvocationID.xy);
    ivec2 texSize = imageSize(outputTexture);
    if (texelCoord.x >= texSize.x || texelCoord.y >= texSize.y) {
        return;
    }
    int flippedY = texSize.y - 1 - texelCoord.y;
    ivec2 flippedCoord = ivec2(texelCoord.x, flippedY);
    vec4 color = texelFetch(inputTexture, flippedCoord, 0);
    imageStore(outputTexture, texelCoord, color);
}
