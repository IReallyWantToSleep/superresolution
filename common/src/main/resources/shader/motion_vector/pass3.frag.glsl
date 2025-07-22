#version 410 core

#ifndef SR_GL41_COMPAT
#extension GL_ARB_shading_language_420pack : enable
#endif

#if SR_GL41_COMPAT
uniform sampler2D grad_current; // Ix, Iy
uniform sampler2D delta_time; // It
#else
layout(binding = 1) uniform sampler2D grad_current; // Ix, Iy
layout(binding = 2) uniform sampler2D delta_time; // It
#endif

layout (location = 0)in vec2 uv;
layout (location = 0)out vec4 out_motion; // RG

#if SR_GL41_COMPAT
layout(std140) uniform motion_vector_data {
    float exposure;
    int window_radius;
    float min_value;
    float scale;
} ;
#else
layout(std140, binding = 0) uniform motion_vector_data {
    float exposure;
    int window_radius;
    float min_value;
    float scale;
};
#endif


void main() {
    vec2 texelSize = 1.0 / textureSize(grad_current, 0);
    float sum_xx = 0.0, sum_xy = 0.0, sum_yy = 0.0;
    float sum_xt = 0.0, sum_yt = 0.0;

    for (int dx = -window_radius; dx <= window_radius; dx++) {
        for (int dy = -window_radius; dy <= window_radius; dy++) {
            vec2 offset = vec2(dx, dy) * texelSize;
            vec2 sampleUV = uv + offset;
            vec2 grad = texture(grad_current, sampleUV).xy;
            float Ix = grad.x;
            float Iy = grad.y;
            float It = texture(delta_time, sampleUV).x;

            sum_xx += Ix * Ix;
            sum_xy += Ix * Iy;
            sum_yy += Iy * Iy;
            sum_xt += Ix * (-It);
            sum_yt += Iy * (-It);
        }
    }

    float det = sum_xx * sum_yy - sum_xy * sum_xy;
    if (abs(det) > min_value) {
        float inv_det = 1.0 / det;
        float u = (sum_xy * sum_yt - sum_yy * sum_xt) * inv_det;
        float v = (sum_xy * sum_xt - sum_xx * sum_yt) * inv_det;
        out_motion = vec4(u * scale, v * scale, 0.0, 1.0);
    } else {
        out_motion = vec4(0.0, 0.0, 0.0, 1.0);
    }
}

