#version 460 core

in vec3 in_position;
uniform vec2 pixelSize;
uniform float depth;
uniform mat4 projectionInverse;
uniform mat4 modelViewInverse;
uniform mat4 lastModelView;
uniform mat4 lastProjection;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
out vec2 FragColor;

vec2 calcMotion(vec3 Position) {
    vec4 cur_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    cur_Position /= cur_Position.w;
    vec4 last_Position = lastProjection * lastModelView * vec4(Position, 1.0);
    last_Position /= last_Position.w;
    vec2 motion = (last_Position.xy - cur_Position.xy);

    return motion;
}

void main() {
    // Call calcMotion with the input position to get the motion vector
    FragColor = calcMotion(in_position);
}