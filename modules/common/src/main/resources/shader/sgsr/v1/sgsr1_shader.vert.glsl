#version 410
precision mediump float;

layout(location = 0) in mediump vec2 aPosition;
layout(location = 1) in mediump vec2 aTexCoord;
layout(location = 0) out mediump vec2 in_TEXCOORD0;
void main() {
    in_TEXCOORD0 = aTexCoord;
    gl_Position = vec4(aPosition, 0.0, 1.0);
}
