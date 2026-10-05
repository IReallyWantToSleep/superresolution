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


    V4 acc0 = M4(-0.28731483, -0.28169647, -0.26601523, -0.20379841, 0.043805175, 0.043708004, 0.034483507, 0.016797496, 0.17906244, 0.1421112, 0.18003947, 0.12905708, 0.0027881037, 0.013680578, 0.029185824, 0.062149458) * g_0;
    V4 acc1 = M4(-0.04496684, 0.0351581, -0.104422495, -0.039678875, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0) * g_1;
    V4 acc2 = M4(0.28526598, 0.2826007, 0.2632455, 0.19020885, 0.017290888, -0.042640634, -0.018812168, -0.021613158, -0.15308675, -0.1463841, -0.13770425, -0.0889454, 0.022968816, 0.004390897, 0.023246381, -0.0006041598) * g_2;
    V4 acc3 = M4(0.01830812, -0.020124227, 0.07409903, 0.06260258, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0) * g_3;
    acc0 += M4(0.1146815, 0.028763762, -0.015858276, -0.22741714, -0.123094, -0.061641257, -0.01165587, 0.17179602, 0.079209566, -0.055403415, 0.1742816, 0.033580918, -0.10710559, -0.24322936, 0.07923649, 0.0020310096) * g_4;
    acc1 += M4(-0.08152331, 0.1876312, 0.0452601, 0.32574433, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0) * g_5;
    acc2 += M4(-0.067084454, 0.07974862, -0.078263395, 0.17602646, -0.043463673, -0.3042032, 0.3347838, -0.0029415577, -0.23070753, -0.33625132, 0.13910769, 0.12265481, -0.03364489, -0.108518235, 0.30449784, 0.26187637) * g_6;
    acc3 += M4(0.09993933, -0.1707275, -0.1890321, -0.52992857, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0) * g_7;
    acc0 += M4(0.09415703, 0.40185246, -0.36297506, -0.11392933, -0.074985184, -0.15693808, 0.025643557, 0.06432217, -0.074275084, -0.1303661, 0.11813074, 0.12708026, 0.09587864, 0.16418858, 0.08375739, 0.036546703) * g_8;
    acc1 += M4(-0.0015154806, -0.15868801, -0.16093469, -0.31733873, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0) * g_9;
    acc2 += M4(-0.12209903, -0.35742065, 0.36612418, 0.15638746, 0.07944282, 0.111370504, -0.045297295, -0.074534655, 0.07760143, 0.10560609, -0.10006874, -0.1366334, -0.021637926, -0.030410666, -0.038979225, -0.029031545) * g_10;
    acc3 += M4(0.0007081034, 0.16215397, 0.103592895, 0.31834108, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0) * g_11;
    acc0 += M4(-0.018274482, 0.009613625, -0.012639771, 0.007172152, -0.007539705, 0.011842848, -0.008011464, -0.009407413, 0.01003011, 0.0683904, -0.019395752, 0.016741594, 0.011211238, -0.0075163473, 0.0047904756, -0.010505887) * g_12;
    acc1 += M4(-0.046080105, 0.020556407, -0.06601358, 0.022094546, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0) * g_13;
    acc2 += M4(0.022211462, -0.002159848, 0.019243062, 0.0029890833, 0.05726425, -0.003533362, 0.108623564, -0.011901096, 8.560688e-05, -0.043200273, 0.0394512, -0.011208162, -0.027750757, 0.0029649043, -0.019478222, 0.0016353371) * g_14;
    acc3 += M4(0.067286275, 0.027566373, 0.06643383, -0.0018268235, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0) * g_15;
    acc0 += M4(-0.05380615, 0.004247197, -0.026698917, -0.0073969425, -0.0011662057, -0.014842724, -0.0022988552, 0.0013432831, 0.011608433, 0.008445917, -0.010101205, -0.00563566, 0.0028953326, -0.023813438, -0.0011192588, -0.0024804787) * g_16;
    acc1 += M4(0.019996438, 0.014099036, 0.03860708, 0.0047151363, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0) * g_17;
    acc2 += M4(-0.015056218, -0.010782295, 0.03997383, -0.0023854189, 0.09982181, 0.11087299, -0.0132297985, 0.0011237978, -0.0034997186, -0.011487468, 0.005097492, -0.0032565834, 0.0054192827, 0.006910739, 0.0006883609, -0.004237314) * g_18;
    acc3 += M4(-0.058096588, -0.03534566, -0.034876667, -0.013044089, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0) * g_19;
    acc0 += M4(0.14296977, 0.009741475, 0.107693166, 0.00030461873, -0.019768376, 0.042854585, -0.009088426, -0.0031442628, -0.050679293, -0.01041722, 0.043780353, -0.0015516713, 0.040799435, -0.0109121585, 0.009643011, 0.002828792) * g_20;
    acc1 += M4(0.05262461, -0.00037404115, -0.041904166, 0.0049253628, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0) * g_21;
    acc2 += M4(-0.049604442, -0.015332969, -0.023177424, -0.0018659168, 0.0041921125, -0.14455552, 0.0080948975, 0.00516916, 0.010852033, 0.008573455, -0.11957098, 0.00087454374, -0.1268283, 0.00087787485, -0.0066636256, -0.006835732) * g_22;
    acc3 += M4(-0.15538642, -0.0066933855, 0.0046638907, -2.5198271e-05, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0) * g_23;
    vec4 result = vec4(acc0 + acc1 + acc2 + acc3);
    result += vec4(-0.0143151255, -0.011507612, -0.0032739025, 0.003943137);
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
