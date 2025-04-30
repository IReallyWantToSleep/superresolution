#version 430 core
precision mediump float;

in vec4 currentPosition;
in vec4 lastPosition;

out vec2 fragColor;

void main() {
    vec2 newXY = currentPosition.xy / currentPosition.w;
    vec2 oldXY = lastPosition.xy / lastPosition.w;
    fragColor = vec4((newXY - oldXY) * 0.5, 0.0, 1.0);
}