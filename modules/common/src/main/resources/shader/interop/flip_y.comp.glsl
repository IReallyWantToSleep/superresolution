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
    #ifdef FLIP_Y
    int sourceY = texSize.y - 1 - texelCoord.y;
    #else
    int sourceY = texelCoord.y;
    #endif
    vec4 color = texelFetch(inputTexture, ivec2(texelCoord.x, sourceY), 0);
    imageStore(outputTexture, texelCoord, color);
}
