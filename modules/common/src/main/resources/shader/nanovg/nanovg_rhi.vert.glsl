#version 460

layout(std140, binding = 0) uniform frame {
    vec2 viewSize;
};

layout(location = 0) in vec2 vertex;
layout(location = 1) in vec2 tcoord;
layout(location = 0) out vec2 ftcoord;
layout(location = 1) out vec2 fpos;

void main() {
    ftcoord = tcoord;
    fpos = vertex;
    gl_Position = vec4(
            2.0 * vertex.x / viewSize.x - 1.0,
            1.0 - 2.0 * vertex.y / viewSize.y,
            0.0,
            1.0
    );
}
