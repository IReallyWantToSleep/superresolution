#version 460

layout(std140, binding = 1) uniform glass {
    vec4 copyRect;
    vec2 textureSize;
    vec2 shapeSize;
    vec4 radii;
    vec4 colorControls;
    vec4 params;
    vec4 optics;
    vec4 lighting;
    vec4 shapeAxes;
    vec4 surfaceTint;
    vec4 material;
};

layout(location = 0) in vec2 vertex;
layout(location = 1) in vec2 tcoord;
layout(location = 0) out vec2 position;

void main() {
    position = vertex * ceil(textureSize / 2.0);
    float clipY = 1.0 - 2.0 * vertex.y;
    if (params.w < 0.5) {
        clipY = -clipY;
    }
    gl_Position = vec4(2.0 * vertex.x - 1.0, clipY, 0.0, 1.0);
}
