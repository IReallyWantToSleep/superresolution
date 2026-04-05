// Auto-generated: pass_conv2d_6_tf1.glsl
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
layout(set=0, binding=0, rgba16f) uniform writeonly image2D out_conv2d_6_tf1;
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
layout(binding=0, rgba16f) uniform writeonly image2D out_conv2d_6_tf1;
#endif

void main() {
    ivec3 id = ivec3(gl_GlobalInvocationID);
    ivec2 out_size = imageSize(out_conv2d_6_tf1);

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


    V4 acc0 = M4(0.13833599, 0.1510968, 0.11759147, 0.07917428, -0.021598516, -0.010830674, -0.017662032, 0.00013040435, -0.0077185524, -0.017163612, -0.023503732, -0.030710805, -0.014679641, -0.03294865, -0.044414926, -0.064137764) * g_0;
    V4 acc1 = M4(-0.09158606, -0.11999663, -0.08608922, -0.07208319, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0) * g_1;
    V4 acc2 = M4(-0.101412505, -0.11049353, -0.10577118, -0.08418702, 0.026197933, 0.018878283, 0.011381935, -0.006758761, 0.029558074, 0.03429811, 0.03366169, 0.024998328, 0.051455256, 0.06676351, 0.04841867, 0.049169395) * g_2;
    V4 acc3 = M4(0.08186813, 0.078294076, 0.0748663, 0.04272876, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0) * g_3;
    acc0 += M4(-0.023789646, -0.0065196953, -0.03296955, -0.046716344, 0.038900565, 0.027438866, 0.047384635, 0.0030110308, 0.035800878, 0.0038158628, -0.013771619, -0.071550526, 0.021919368, -0.0146391075, -0.07219394, -0.16968054) * g_4;
    acc1 += M4(0.054953024, -0.02576673, -0.011619401, -0.17568444, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0) * g_5;
    acc2 += M4(-0.00050889613, 0.005066713, 0.04702386, 0.09506398, 0.0006514243, -0.01641264, -0.07405263, -0.113081336, -0.015327367, -0.0374403, -0.049880877, -0.066627115, 0.01887231, 0.0051407875, 0.082068115, 0.09462416) * g_6;
    acc3 += M4(-0.11107563, -0.025928324, 0.07766232, 0.26844975, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0) * g_7;
    acc0 += M4(-0.05395025, -0.002871982, 0.048222832, 0.16542372, 0.066668294, -0.026515048, 0.00071685715, -0.054526504, 0.056437008, 0.026028411, 0.0077580963, -0.047788117, 0.053688508, 0.10267845, -0.010855451, -0.020061634) * g_8;
    acc1 += M4(-0.122868866, -0.09174706, -0.055293567, 0.07057342, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0) * g_9;
    acc2 += M4(0.06395515, -0.01115496, -0.043855757, -0.17008409, -0.023970399, -0.008306531, 0.019926487, 0.05210557, -0.026737934, -0.022378528, 0.01323459, 0.045358825, -0.0674798, -0.052949753, -0.0035545477, 0.027470807) * g_10;
    acc3 += M4(0.10432877, 0.12136149, 0.0016662778, -0.063579515, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0) * g_11;
    acc0 += M4(-0.020676322, -0.017988455, -0.013410114, 0.0011058156, 0.008996582, 0.028164206, 0.04799255, 0.0121447, -0.027267504, -0.00950592, -0.008361492, 0.009393668, 0.017941885, 0.021878568, 0.040214665, 0.01711583) * g_12;
    acc1 += M4(-0.0508187, -0.017610071, -0.053712547, 0.001253383, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0) * g_13;
    acc2 += M4(0.012127488, 0.0212642, 0.008170013, -0.0024069725, 0.028343009, -0.0052684117, 0.062867485, -0.0059288656, 0.06595123, 0.08722877, 0.0088819405, -0.008057745, -0.015576851, -0.014842752, -0.031123307, -0.01628728) * g_14;
    acc3 += M4(0.042052757, -0.005770826, 0.052308746, 0.0044931723, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0) * g_15;
    acc0 += M4(-0.031814516, -0.0126058, -0.03812562, 0.00073940074, -0.014912649, -0.014191315, -0.013656121, 7.711217e-05, 0.01380188, 0.007500692, -0.0040368475, 5.237243e-05, -0.010446347, -0.0037906913, -0.002848231, 0.0020671007) * g_16;
    acc1 += M4(0.023070367, 0.011798055, 0.04581355, 0.0030063705, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0) * g_17;
    acc2 += M4(-0.0030911514, -0.0057753944, 0.027413707, -0.0011661226, 0.13611823, 0.13934772, 0.01143973, -0.0017637258, 0.0081741065, 0.012764391, -0.017416194, -0.0027782691, -0.005060463, 0.004722867, -0.007297715, 4.7994035e-06) * g_18;
    acc3 += M4(-0.03799101, -0.021981817, -0.05661863, -0.008100254, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0) * g_19;
    acc0 += M4(0.12679234, 0.01708845, 0.10885553, 0.0025597299, -0.031829763, 0.03872515, -0.011161394, -0.002008881, -0.009381131, -0.017221281, 0.03677418, -0.0024946241, 0.035622828, -0.0055157435, 0.021508604, 0.0012442851) * g_20;
    acc1 += M4(0.05474838, 0.004408891, -0.03361215, 0.0023985296, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0) * g_21;
    acc2 += M4(-0.032176957, -0.021095013, -0.017762419, -0.0007824195, 0.028674832, -0.13501178, 0.0017282439, -0.00039017524, -0.064180896, 0.024000574, -0.14208168, 0.0024250667, -0.14630087, -0.055540387, -0.011693568, -0.0012458846) * g_22;
    acc3 += M4(-0.13070844, -0.007453941, 0.05583553, -0.0022392496, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0) * g_23;
    vec4 result = vec4(acc0 + acc1 + acc2 + acc3);
    result += vec4(0.0073291976, 0.006784828, 0.0055302526, 0.0012668582);
    imageStore(out_conv2d_6_tf1, id.xy, result);

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
