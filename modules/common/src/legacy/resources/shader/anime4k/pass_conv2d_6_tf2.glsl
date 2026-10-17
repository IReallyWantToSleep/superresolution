// Auto-generated: pass_conv2d_6_tf2.glsl
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
layout(set=0, binding=0, rgba16f) uniform writeonly image2D out_conv2d_6_tf2;
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
layout(binding=0, rgba16f) uniform writeonly image2D out_conv2d_6_tf2;
#endif

void main() {
    ivec3 id = ivec3(gl_GlobalInvocationID);
    ivec2 out_size = imageSize(out_conv2d_6_tf2);

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


    V4 acc0 = M4(-0.008019026, -0.028185422, 0.020470934, -0.04755843, 0.050335873, 0.044683646, 0.03821029, 0.007450188, 0.039109316, -0.002769654, 0.04600724, 0.014350844, 0.048450828, 0.03941582, 0.20045641, 0.19086362) * g_0;
    V4 acc1 = M4(0.081223115, 0.30994016, 0.013902807, 0.1932641, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0) * g_1;
    V4 acc2 = M4(0.04329196, 0.06767785, -0.008100487, 0.026395537, 0.01421103, -0.0580385, 0.0070027285, -0.04398463, -0.07278665, -0.054980356, -0.032499995, 0.0006663621, -0.1390264, -0.113030516, -0.20445663, -0.15234885) * g_2;
    V4 acc3 = M4(-0.13459964, -0.2576629, -0.046779398, -0.13900572, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0) * g_3;
    acc0 += M4(0.07483325, 0.013147594, 0.06134013, -0.0909146, -0.02794309, 0.049687527, -0.08155928, 0.12377137, -0.22077551, -0.15539326, -0.06942348, 0.18648465, 0.030812182, 0.17240414, -0.054036845, 0.1060944) * g_4;
    acc1 += M4(-0.23261072, -0.34949175, -0.0823904, -0.24681179, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0) * g_5;
    acc2 += M4(-0.11649956, -0.12081779, -0.030623032, -0.017810168, 0.057627242, 0.18217695, 0.11571123, 0.18852937, 0.27494383, 0.38333443, 0.052580897, 0.11069936, 0.08104833, 0.1576198, 0.058903005, 0.260108) * g_6;
    acc3 += M4(0.23566087, 0.22876601, 0.12191552, 0.13173717, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0) * g_7;
    acc0 += M4(-0.070260264, -0.25876266, -0.062819116, -0.3030565, 0.010920702, 0.07704275, -0.066010974, 0.11471069, 0.04592589, 0.1185449, -0.06423864, 0.1525578, 0.06277498, 0.2586822, -0.03197632, 0.093452714) * g_8;
    acc1 += M4(0.020673908, -0.0015805386, 0.015569225, -0.023704745, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0) * g_9;
    acc2 += M4(0.04059213, 0.27704826, 0.05259083, 0.3500655, -0.017008822, -0.105656594, 0.037453007, -0.11885026, -0.050436694, -0.13550362, 0.060046703, -0.15775552, -0.04172459, -0.16310458, 0.041797046, -0.08576887) * g_10;
    acc3 += M4(-0.025255289, 0.01313391, -0.042582404, 0.02361832, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0) * g_11;
    acc0 += M4(-0.013765708, -0.0027898327, -0.013371897, -0.003710145, -5.4879813e-05, 0.0042903507, -0.012877853, -0.0017408337, 0.004755811, 0.054108877, -0.026274698, 0.007841983, 0.0014868818, -0.003914284, 0.0113113215, -0.005260449) * g_12;
    acc1 += M4(-0.027104279, 0.014368826, -0.05572139, 0.014602862, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0) * g_13;
    acc2 += M4(0.023836985, 0.0033451922, 0.012775952, 0.008536059, 0.034077328, 0.013900489, 0.075832136, -0.009509624, -0.014945718, -0.052063085, 0.025826486, -0.0031978085, -0.014414275, -0.0003447713, -0.019767027, -0.0030674005) * g_14;
    acc3 += M4(0.027796073, 0.01567754, 0.045474526, -0.004044273, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0) * g_15;
    acc0 += M4(-0.042939972, -0.0043603256, -0.015121886, -0.0017079333, 0.0007536355, -0.013574549, -0.0012273194, -0.0008965791, 0.0037725314, 0.0026105344, -0.009147138, -0.009473263, 0.0010922512, -0.021958312, 0.0040458837, -0.0026702224) * g_16;
    acc1 += M4(0.02027918, 0.018509377, 0.03873534, 0.0042219553, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0) * g_17;
    acc2 += M4(-0.0096045965, -0.0063137687, 0.037288256, -0.0024752996, 0.071006335, 0.08198581, -0.014355797, 0.0010181116, -0.0036240586, -0.012588513, 0.0011914563, 0.0021665455, 0.0047698845, 0.006612787, -0.007448365, -0.0030797957) * g_18;
    acc3 += M4(-0.052995946, -0.04383154, -0.04104956, -0.010784443, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0) * g_19;
    acc0 += M4(0.09667259, 0.0124960765, 0.07921741, 0.001460559, -0.018548792, 0.03514463, -0.0074166316, -0.0031648194, -0.038867973, -0.011847746, 0.032886025, 0.00084089226, 0.033721194, -0.011651657, 0.010530514, 0.0005548376) * g_20;
    acc1 += M4(0.038685203, 0.0035427182, -0.034726575, 0.00086147187, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0) * g_21;
    acc2 += M4(-0.03411127, -0.016175553, -0.019261658, -0.0030228856, 0.003041096, -0.095099926, 0.006600643, 0.005763467, 0.012340728, 0.011495008, -0.07747458, 0.0017962418, -0.084497295, 0.0034767366, -0.010849909, -0.0033823769) * g_22;
    acc3 += M4(-0.109412365, -0.006487256, 0.0015345642, -5.4929988e-05, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0) * g_23;
    vec4 result = vec4(acc0 + acc1 + acc2 + acc3);
    result += vec4(-0.0022428911, -0.00018487207, 0.00068119244, 0.0023686169);
    imageStore(out_conv2d_6_tf2, id.xy, result);

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
