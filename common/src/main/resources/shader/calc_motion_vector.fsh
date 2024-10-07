#version 460 core

in vec4 cur_Position;
in vec4 last_Position;

out vec4 FragColor;


void main() {
    vec2 motion = ((last_Position.xy / last_Position.w) - (cur_Position.xy / cur_Position.w)) * 0.5;
    FragColor = vec4(motion.xy,1.0,1.0);
}
