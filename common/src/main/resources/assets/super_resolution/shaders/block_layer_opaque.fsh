#version 330 core

#import <sodium:include/fog.glsl>

in vec4 v_Color; // The interpolated vertex color
in vec2 v_TexCoord; // The interpolated block texture coordinates
in float v_FragDistance; // The fragment's distance from the camera

in float v_MaterialMipBias;
in float v_MaterialAlphaCutoff;
in vec4 v_lastPosition;
in vec4 v_currentPosition;


uniform sampler2D u_BlockTex; // The block texture

uniform vec4 u_FogColor; // The color of the shader fog
uniform float u_FogStart; // The starting position of the shader fog
uniform float u_FogEnd; // The ending position of the shader fog

layout(location = 0) out vec4 fragColor; // 原输出
layout(location = 1) out vec2 o_MotionVector; // 运动矢量


vec2 calculateMotionVector() {
    vec3 currentNDC = v_currentPosition.xyz / v_currentPosition.w;
    vec3 previousNDC = v_lastPosition.xyz / v_lastPosition.w;
    return (currentNDC.xy - previousNDC.xy) * 0.5;
}


void main() {
    vec4 diffuseColor = texture(u_BlockTex, v_TexCoord, v_MaterialMipBias);

    // Apply per-vertex color
    diffuseColor *= v_Color;

    #ifdef USE_FRAGMENT_DISCARD
    if (diffuseColor.a < v_MaterialAlphaCutoff) {
        discard;
    }
    #endif

    fragColor = _linearFog(diffuseColor, v_FragDistance, u_FogColor, u_FogStart, u_FogEnd);
    o_MotionVector = calculateMotionVector();
}
