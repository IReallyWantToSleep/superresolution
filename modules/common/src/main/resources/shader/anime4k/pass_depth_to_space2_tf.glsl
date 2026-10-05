// Auto-generated: pass_depth_to_space2_tf.glsl
#version 450
layout(local_size_x=16, local_size_y=16, local_size_z=1) in;

#ifdef VULKAN
layout(set=0, binding=0) uniform sampler2D conv2d_6_tf;
layout(set=0, binding=1) uniform sampler2D conv2d_6_tf1;
layout(set=0, binding=2) uniform sampler2D conv2d_6_tf2;
layout(set=0, binding=0, rgba16f) uniform writeonly image2D out_depth_to_space2_tf;
#else
layout(binding=0) uniform sampler2D conv2d_6_tf;
layout(binding=1) uniform sampler2D conv2d_6_tf1;
layout(binding=2) uniform sampler2D conv2d_6_tf2;
layout(binding=0, rgba16f) uniform writeonly image2D out_depth_to_space2_tf;
#endif

void main() {
    ivec3 id = ivec3(gl_GlobalInvocationID);
    ivec2 out_size = imageSize(out_depth_to_space2_tf);
    if (id.x >= out_size.x || id.y >= out_size.y) return;

    ivec2 sub = id.xy % 2;
    ivec2 in_coord = id.xy / 2;

    vec2 in_uv_0 = (vec2(in_coord) + 0.5) / vec2(textureSize(conv2d_6_tf, 0));
    float c0 = texture(conv2d_6_tf, in_uv_0)[sub.y * 2 + sub.x];
    vec2 in_uv_1 = (vec2(in_coord) + 0.5) / vec2(textureSize(conv2d_6_tf1, 0));
    float c1 = texture(conv2d_6_tf1, in_uv_1)[sub.y * 2 + sub.x];
    vec2 in_uv_2 = (vec2(in_coord) + 0.5) / vec2(textureSize(conv2d_6_tf2, 0));
    float c2 = texture(conv2d_6_tf2, in_uv_2)[sub.y * 2 + sub.x];
    float c3 = 0.0;

    imageStore(out_depth_to_space2_tf, id.xy, vec4(c0, c1, c2, c3));
}
