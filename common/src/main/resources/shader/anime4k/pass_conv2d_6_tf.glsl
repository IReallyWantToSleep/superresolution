// Auto-generated: pass_conv2d_6_tf.glsl
#version 450
#ifdef FP16
#extension GL_NV_gpu_shader5 : enable
#define V4 f16vec4
#define M4 f16mat4
#else
#define V4 vec4
#define M4 mat4
#endif
layout(local_size_x=16, local_size_y=16, local_size_z=1) in;

#ifdef VULKAN
layout(set=0, binding=0) uniform sampler2D conv2d_tf;
layout(set=0, binding=1) uniform sampler2D conv2d_tf1;
layout(set=0, binding=2) uniform sampler2D conv2d_1_tf;
layout(set=0, binding=3) uniform sampler2D conv2d_1_tf1;
layout(set=0, binding=4) uniform sampler2D conv2d_2_tf;
layout(set=0, binding=5) uniform sampler2D conv2d_2_tf1;
layout(set=0, binding=6) uniform sampler2D conv2d_3_tf;
layout(set=0, binding=7) uniform sampler2D conv2d_3_tf1;
layout(set=0, binding=8) uniform sampler2D conv2d_4_tf;
layout(set=0, binding=9) uniform sampler2D conv2d_4_tf1;
layout(set=0, binding=10) uniform sampler2D conv2d_5_tf;
layout(set=0, binding=11) uniform sampler2D conv2d_5_tf1;
layout(set=0, binding=0, rgba16f) uniform writeonly image2D out_conv2d_6_tf;
#else
layout(binding=0) uniform sampler2D conv2d_tf;
layout(binding=1) uniform sampler2D conv2d_tf1;
layout(binding=2) uniform sampler2D conv2d_1_tf;
layout(binding=3) uniform sampler2D conv2d_1_tf1;
layout(binding=4) uniform sampler2D conv2d_2_tf;
layout(binding=5) uniform sampler2D conv2d_2_tf1;
layout(binding=6) uniform sampler2D conv2d_3_tf;
layout(binding=7) uniform sampler2D conv2d_3_tf1;
layout(binding=8) uniform sampler2D conv2d_4_tf;
layout(binding=9) uniform sampler2D conv2d_4_tf1;
layout(binding=10) uniform sampler2D conv2d_5_tf;
layout(binding=11) uniform sampler2D conv2d_5_tf1;
layout(binding=0, rgba16f) uniform writeonly image2D out_conv2d_6_tf;
#endif

void main() {
    ivec3 id = ivec3(gl_GlobalInvocationID);
    ivec2 out_size = imageSize(out_conv2d_6_tf);

    if (id.x >= out_size.x || id.y >= out_size.y) return;

    #define g_0 (max((texelFetch(conv2d_tf, id.xy, 0)), 0.0))
    #define g_1 (max((texelFetch(conv2d_tf1, id.xy, 0)), 0.0))
    #define g_2 (max(-(texelFetch(conv2d_tf, id.xy, 0)), 0.0))
    #define g_3 (max(-(texelFetch(conv2d_tf1, id.xy, 0)), 0.0))
    #define g_4 (max((texelFetch(conv2d_1_tf, id.xy, 0)), 0.0))
    #define g_5 (max((texelFetch(conv2d_1_tf1, id.xy, 0)), 0.0))
    #define g_6 (max(-(texelFetch(conv2d_1_tf, id.xy, 0)), 0.0))
    #define g_7 (max(-(texelFetch(conv2d_1_tf1, id.xy, 0)), 0.0))
    #define g_8 (max((texelFetch(conv2d_2_tf, id.xy, 0)), 0.0))
    #define g_9 (max((texelFetch(conv2d_2_tf1, id.xy, 0)), 0.0))
    #define g_10 (max(-(texelFetch(conv2d_2_tf, id.xy, 0)), 0.0))
    #define g_11 (max(-(texelFetch(conv2d_2_tf1, id.xy, 0)), 0.0))
    #define g_12 (max((texelFetch(conv2d_3_tf, id.xy, 0)), 0.0))
    #define g_13 (max((texelFetch(conv2d_3_tf1, id.xy, 0)), 0.0))
    #define g_14 (max(-(texelFetch(conv2d_3_tf, id.xy, 0)), 0.0))
    #define g_15 (max(-(texelFetch(conv2d_3_tf1, id.xy, 0)), 0.0))
    #define g_16 (max((texelFetch(conv2d_4_tf, id.xy, 0)), 0.0))
    #define g_17 (max((texelFetch(conv2d_4_tf1, id.xy, 0)), 0.0))
    #define g_18 (max(-(texelFetch(conv2d_4_tf, id.xy, 0)), 0.0))
    #define g_19 (max(-(texelFetch(conv2d_4_tf1, id.xy, 0)), 0.0))
    #define g_20 (max((texelFetch(conv2d_5_tf, id.xy, 0)), 0.0))
    #define g_21 (max((texelFetch(conv2d_5_tf1, id.xy, 0)), 0.0))
    #define g_22 (max(-(texelFetch(conv2d_5_tf, id.xy, 0)), 0.0))
    #define g_23 (max(-(texelFetch(conv2d_5_tf1, id.xy, 0)), 0.0))


    V4 acc0 = M4(-0.21756424, -0.20681517, -0.2179329, -0.167185, 0.010440645, 0.0073316917, 0.0063002678, 0.0011797572, 0.11445765, 0.11474633, 0.10037015, 0.09006147, -0.012260069, 0.019565688, -0.03428165, 0.010248612) * g_0;
    V4 acc1 = M4(0.04342776, 0.06015732, 0.051889125, 0.060039498, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0) * g_1;
    V4 acc2 = M4(0.21147273, 0.20281456, 0.20409581, 0.15324087, -0.01227855, 7.2781324e-05, 0.0044902405, 0.016953066, -0.08882692, -0.086573765, -0.07687893, -0.064554274, 0.019587588, 0.0054162783, 0.034397107, 0.001115152) * g_2;
    V4 acc3 = M4(-0.08410015, -0.081861466, -0.0600889, -0.01487149, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0) * g_3;
    acc0 += M4(0.0260726, -0.021659926, 0.018740624, -0.08016891, -0.034672216, 0.012858317, -0.041836664, 0.00847369, 0.039998632, 0.009468795, 0.03276544, 0.0011095483, -0.08759747, -0.08791631, -0.108302906, -0.12828304) * g_4;
    acc1 += M4(0.00032998598, 0.1441819, 0.05441114, 0.19356604, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0) * g_5;
    acc2 += M4(-0.0063260566, 0.07555057, -0.06882009, 0.05510606, -0.0611658, -0.11192801, 0.10612225, 0.06543209, -0.16342175, -0.16640195, 0.011747759, 0.053757124, 0.019529555, -0.0077700675, 0.16181913, 0.17431107) * g_6;
    acc3 += M4(0.046671756, -0.1811399, -0.11440204, -0.39176598, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0) * g_7;
    acc0 += M4(0.1528676, 0.19299458, -0.07552212, -0.052753918, -0.01782599, -0.10101222, 0.02884889, 0.023560723, 0.008499066, -0.016687596, 0.060011435, 0.044715494, 0.04413199, 0.08109335, 0.049023956, 0.01188734) * g_8;
    acc1 += M4(-0.04360357, -0.20200044, -0.14808398, -0.2925892, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0) * g_9;
    acc2 += M4(-0.14149652, -0.21643499, 0.08664547, 0.045656525, 0.062213115, 0.074432455, -0.019045075, -0.027151313, 0.014945344, 0.020906053, -0.04830915, -0.04429242, -0.031074684, -0.008278694, -0.034240656, -0.0041569816) * g_10;
    acc3 += M4(0.03533642, 0.21441406, 0.11025771, 0.29021832, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0) * g_11;
    acc0 += M4(-0.024683615, -0.00643183, -0.031245839, -0.0021561938, 0.0058296775, 0.037569616, 0.029878886, 0.011086285, -0.012610353, -0.0015341352, 0.0023249676, 0.0024935184, 0.031982455, 0.034985486, 0.020336375, -0.012821327) * g_12;
    acc1 += M4(-0.046281278, -0.018715756, -0.043473657, 0.0039482294, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0) * g_13;
    acc2 += M4(0.011827118, 0.01547756, 0.016957602, 0.0030937353, 0.019633431, -0.013005773, 0.060447678, -0.008928967, 0.05369271, 0.08065334, 0.0008835115, 0.0002270095, -0.028802503, -0.022419881, -0.017667942, 0.011944444) * g_14;
    acc3 += M4(0.051962506, -0.0057723154, 0.06318386, -0.002263123, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0) * g_15;
    acc0 += M4(-0.027700974, -0.0124156615, -0.022744898, 0.009929454, -0.016557567, -0.012396817, -0.015395949, 0.0040404443, 0.01331711, 0.016185608, -0.0134496335, 0.0008538896, -0.0055549447, 0.0011827336, -0.0032730314, -0.0011581685) * g_16;
    acc1 += M4(0.02058762, 0.010159769, 0.047219213, 0.0034779152, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0) * g_17;
    acc2 += M4(-0.0060311914, -0.00726593, 0.020123255, -0.008067445, 0.13535093, 0.13154678, 0.018581968, -0.0049950317, 0.0041920403, 0.0059907753, -0.010588422, 0.0028443427, -0.005116031, 0.0064719566, -0.007616424, 0.0019991258) * g_18;
    acc3 += M4(-0.032414515, -0.01753005, -0.051176164, -0.005149293, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0) * g_19;
    acc0 += M4(0.12949836, 0.018909778, 0.10953147, -0.00032744682, -0.03144348, 0.041169584, -0.0127366, 0.0022945087, -0.010769109, -0.016837485, 0.037444696, -0.0005296925, 0.037110455, -0.004333503, 0.02425513, 0.0010398232) * g_20;
    acc1 += M4(0.053315636, 0.002200081, -0.03295669, 0.0018115324, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0) * g_21;
    acc2 += M4(-0.033929467, -0.021535784, -0.017507806, 0.0012613826, 0.03138212, -0.13199227, 0.0048884377, -0.00027967797, -0.061623327, 0.024620598, -0.14203992, -0.00022858215, -0.14506285, -0.054894477, -0.010950959, -0.0007437272) * g_22;
    acc3 += M4(-0.12581743, -0.0075021237, 0.061514385, -0.0005004876, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0) * g_23;
    vec4 result = vec4(acc0 + acc1 + acc2 + acc3);
    result += vec4(-0.009366705, -0.006265362, -0.0050032274, 0.00088919525);
    imageStore(out_conv2d_6_tf, id.xy, result);

    #undef g_0
    #undef g_1
    #undef g_2
    #undef g_3
    #undef g_4
    #undef g_5
    #undef g_6
    #undef g_7
    #undef g_8
    #undef g_9
    #undef g_10
    #undef g_11
    #undef g_12
    #undef g_13
    #undef g_14
    #undef g_15
    #undef g_16
    #undef g_17
    #undef g_18
    #undef g_19
    #undef g_20
    #undef g_21
    #undef g_22
    #undef g_23
}
