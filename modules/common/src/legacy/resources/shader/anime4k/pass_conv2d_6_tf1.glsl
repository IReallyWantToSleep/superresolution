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


    V4 acc0 = M4(0.21900138, 0.24380322, 0.117478065, 0.101671755, -0.060501844, -0.04630456, -0.040603165, -0.016763113, -0.105872795, -0.059208833, -0.09758557, -0.06295865, -0.035265256, -0.03339182, -0.118740775, -0.12435564) * g_0;
    V4 acc1 = M4(-0.007547703, -0.20087689, 0.01301936, -0.12432312, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0) * g_1;
    V4 acc2 = M4(-0.16770789, -0.15201585, -0.13363576, -0.104183316, 0.11666257, 0.024335941, 0.017356642, 0.037058152, 0.12094904, 0.0761381, 0.07013408, 0.024340728, 0.090481654, 0.07857973, 0.11299244, 0.09266121) * g_2;
    V4 acc3 = M4(0.06486398, 0.18166889, 0.016482648, 0.09517302, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0) * g_3;
    acc0 += M4(-0.07940974, -0.015683927, -0.059816774, 0.026629109, 0.089630015, 0.023656415, 0.20412658, -0.07592602, 0.037736565, 0.064665176, -0.080728464, -0.08178537, 0.043535594, -0.009558873, -0.253799, -0.30782607) * g_4;
    acc1 += M4(0.15395758, -0.02467431, -0.059591502, -0.25927386, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0) * g_5;
    acc2 += M4(0.0863473, -0.010099851, 0.075266354, 0.07663382, -0.10770763, -0.059116766, -0.30261582, -0.09168942, -0.09094262, -0.11796774, -0.1330586, -0.1336101, -0.00047575767, 0.067060046, 0.15029946, 0.031129075) * g_6;
    acc3 += M4(-0.23855959, -0.068503566, 0.13902071, 0.41113314, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0) * g_7;
    acc0 += M4(-0.017873855, 0.037408214, 0.1865393, 0.19083366, -0.023038628, -0.024894793, -0.060310498, -0.10562787, 0.031628586, 0.05401001, 0.086884916, -0.17305085, 0.09977054, 0.22467463, -0.14997259, -0.07469249) * g_8;
    acc1 += M4(-0.18565421, -0.14398168, -0.12532495, 0.13335069, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0) * g_9;
    acc2 += M4(0.026004288, 0.0066698506, -0.13744862, -0.16312964, 0.034597933, -0.022455812, 0.06359688, 0.10521412, -0.021370241, -0.06744742, -0.024549061, 0.16830431, -0.03621629, -0.10720443, 0.07993672, 0.054700356) * g_10;
    acc3 += M4(0.15268096, 0.15088826, -0.014499998, -0.13102947, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0) * g_11;
    acc0 += M4(-0.03396043, -0.0127038155, -0.006599852, -0.00099901, -0.018076072, 0.009726799, 0.0065887906, -0.024110401, -0.01694063, 0.05823813, -0.052997246, -0.0023449156, 0.018371914, -0.002692457, -0.016739188, 0.009781788) * g_12;
    acc1 += M4(-0.057000678, 0.018157095, -0.046344277, 0.0064808726, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0) * g_13;
    acc2 += M4(0.03098514, 0.011056558, 0.011761984, 0.001963921, 0.10079744, -0.0016310114, 0.12952676, 0.004738247, 0.012071601, -0.029886661, 0.023969803, 0.018483462, -0.03328136, -0.00022892849, -0.0032173085, -0.0129970275) * g_14;
    acc3 += M4(0.077537596, 0.021612117, 0.058824588, 0.026143894, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0) * g_15;
    acc0 += M4(-0.064282164, 0.0005047178, -0.0364088, -0.005167397, -0.0032511558, -0.014292792, -0.00658204, 0.0016063198, 0.0035826736, 0.0038516312, -0.0077213943, -0.017344067, 0.001229853, -0.026396042, -0.008012593, -0.00022650551) * g_16;
    acc1 += M4(0.033679098, 0.020465137, 0.049416583, -0.0040426324, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0) * g_17;
    acc2 += M4(-0.0034123927, -0.016681872, 0.054839674, 0.003338093, 0.116827205, 0.11873996, -0.009644162, 5.7753903e-05, -0.0029349755, -0.007573481, -0.0034917742, -0.0027416318, 0.0099163195, 0.001524718, 0.008597776, -0.011323726) * g_18;
    acc3 += M4(-0.065955766, -0.04183413, -0.04493264, 0.00043845363, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0) * g_19;
    acc0 += M4(0.15685593, 0.009943394, 0.11915081, 0.0019218096, -0.02059704, 0.042321477, -0.011797986, -0.0032300933, -0.056465607, -0.015083969, 0.05043184, 0.0025028402, 0.041467074, -0.011542027, 0.013836909, 0.002807789) * g_20;
    acc1 += M4(0.059174202, 0.0018715418, -0.054615945, -0.0031367561, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0) * g_21;
    acc2 += M4(-0.056335747, -0.01838766, -0.030570565, -0.0020478335, 0.0032879566, -0.18220353, 0.0009251796, 0.005015692, 0.010041121, 0.0112544, -0.14510624, -0.0006237432, -0.15096238, -0.00092116185, -0.0038200049, -0.0059794555) * g_22;
    acc3 += M4(-0.16287957, -0.009402018, 0.030001085, 0.010191199, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0) * g_23;
    vec4 result = vec4(acc0 + acc1 + acc2 + acc3);
    result += vec4(0.007887967, 0.0013482213, 0.0024761131, -0.005471429);
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
