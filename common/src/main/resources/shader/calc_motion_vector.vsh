#version 460 core

in vec3 Position;
in vec2 UV;
in vec4 Color;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform mat4 lastProjMat;

out vec4 cur_Position;
out vec4 last_Position;

void main() {
    cur_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    last_Position = lastProjMat * ModelViewMat * vec4(Position, 1.0);
}
