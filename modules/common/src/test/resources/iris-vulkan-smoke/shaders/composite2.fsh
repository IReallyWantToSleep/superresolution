#version 330 compatibility
/* RENDERTARGETS: 0 */
uniform sampler2D colortex0;
in vec2 uv;
layout(location = 0) out vec4 color;
void main() {
    color = texture(colortex0, uv);
}
