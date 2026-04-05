// Auto-generated: pass_up_sampling2d_tf.glsl
#version 450
layout(local_size_x=16, local_size_y=16, local_size_z=1) in;

#ifdef VULKAN
layout(set=0, binding=0) uniform sampler2D input_1_tf;
layout(set=0, binding=0, rgba16f) uniform writeonly image2D out_up_sampling2d_tf;
#else
layout(binding=0) uniform sampler2D input_1_tf;
layout(binding=0, rgba16f) uniform writeonly image2D out_up_sampling2d_tf;
#endif

void main() {
    ivec3 id = ivec3(gl_GlobalInvocationID);
    ivec2 out_size = imageSize(out_up_sampling2d_tf);
    if (id.x >= out_size.x || id.y >= out_size.y) return;
    ivec2 in_coord = ivec2(id.x / 2, id.y / 2);
    vec2 in_uv = (vec2(in_coord) + 0.5) / vec2(textureSize(input_1_tf, 0));
    imageStore(out_up_sampling2d_tf, id.xy, texture(input_1_tf, in_uv));
}
