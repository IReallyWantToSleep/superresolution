// Auto-generated: pass_add_tf.glsl
#version 450
layout(local_size_x=16, local_size_y=16, local_size_z=1) in;

#ifdef VULKAN
layout(set=0, binding=0) uniform sampler2D depth_to_space2_tf;
layout(set=0, binding=1) uniform sampler2D up_sampling2d_tf;
layout(set=0, binding=2, rgba32f) uniform writeonly image2D out_add_tf;
#else
layout(binding=0) uniform sampler2D depth_to_space2_tf;
layout(binding=1) uniform sampler2D up_sampling2d_tf;
layout(binding=2, rgba32f) uniform writeonly image2D out_add_tf;
#endif

void main() {
    ivec3 id = ivec3(gl_GlobalInvocationID);
    ivec2 out_size = imageSize(out_add_tf);
    if (id.x >= out_size.x || id.y >= out_size.y) return;
    vec2 coord = (vec2(id.xy) + 0.5) / vec2(out_size);

    imageStore(out_add_tf, id.xy, texture(depth_to_space2_tf, coord) + texture(up_sampling2d_tf, coord));
}
