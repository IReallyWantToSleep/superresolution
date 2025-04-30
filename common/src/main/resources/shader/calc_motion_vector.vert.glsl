#version 430 core
precision mediump float;

layout (location = 0) in vec2 aPosition;
layout (location = 1) in vec2 aTexCoord;

uniform mat4 u_LookAt;
uniform mat4 u_LookAtLast;
out vec4 currentPosition;
out vec4 lastPosition;

void main() {
    lastPosition = u_LookAtLast * aPosition;
    currentPosition = u_LookAt * aPosition;
    gl_Position = currentPosition;
}