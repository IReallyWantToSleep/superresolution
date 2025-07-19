#version 410
precision mediump float;

layout(location = 0)uniform sampler2D uTexture;
layout(location = 0) in vec2 vTexCoord;
layout(location = 0) out vec4 FragColor;
void main() {
    FragColor = texture(uTexture, vTexCoord);
}
