#version 330
precision mediump float;

uniform sampler2D uTexture;
in vec2 vTexCoord;
out vec4 FragColor;
void main() {
    FragColor = texture(uTexture, vTexCoord);
}