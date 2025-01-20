#version 460 core

in vec3 Position;
in vec2 UV;
in vec4 Color;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
out vec3 in_position;
void main() {
    in_position = Position;
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}