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


    V4 acc0 = M4(0.06195966, 0.041533474, 0.05534797, 0.032814845, -0.0040759044, 0.006847613, -0.010505143, -0.003235232, -0.0007882254, -0.007288701, -0.009972922, -0.004747213, 0.11268799, 0.09277099, 0.18925676, 0.13188636) * g_0;
    V4 acc1 = M4(0.11100991, 0.1228202, 0.08759583, 0.08144368, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0) * g_1;
    V4 acc2 = M4(-0.03252145, -0.010879425, -0.04323905, -0.019969447, 0.0124584325, -0.008094231, 0.014747572, 0.0041567446, 0.008857425, 0.0088460175, 0.010051646, 0.008954208, -0.0932778, -0.06654881, -0.115118034, -0.05532606) * g_2;
    V4 acc3 = M4(-0.089173585, -0.12174616, -0.05997865, -0.061645072, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0) * g_3;
    acc0 += M4(-0.0018995035, 0.05040881, -0.03226814, -0.0068750638, 0.0496936, 0.00097263523, 0.086142115, 0.010950587, -0.1922566, -0.10810325, -0.074392736, 0.10983854, 0.020001367, 0.013857823, -0.040136732, -0.065745324) * g_4;
    acc1 += M4(-0.06676309, -0.22884968, -0.016965179, -0.21633184, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0) * g_5;
    acc2 += M4(-0.017278094, -0.11232874, 0.0465679, -0.049438924, -0.030161293, 0.10906998, -0.09052521, 0.10863254, 0.17337105, 0.20023414, 0.028258668, 0.022757756, -0.002922295, 0.039041147, 0.062337574, 0.1529435) * g_6;
    acc3 += M4(0.08382841, 0.19592966, 0.07303599, 0.1720028, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0) * g_7;
    acc0 += M4(-0.03080013, -0.1730132, 0.0005536765, -0.19633447, 0.026837138, 0.048119962, -0.00042735864, 0.068101265, 0.031948328, 0.07943759, -0.0069323797, 0.055690814, 0.061132185, 0.10428707, 0.024976293, 0.025151895) * g_8;
    acc1 += M4(-0.026247857, 0.032743562, -0.049806755, 0.040676035, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0) * g_9;
    acc2 += M4(0.02218183, 0.16184524, -0.011757931, 0.20552161, 0.007849015, -0.08093901, 0.023407549, -0.06597672, -0.0061001, -0.073168084, 0.016283197, -0.053363383, -0.07003614, -0.09803488, -0.012091078, -0.038967162) * g_10;
    acc3 += M4(0.008845604, -0.0031054416, 0.0049737426, -0.032405715, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0) * g_11;
    acc0 += M4(-0.019187842, -0.03119596, -0.012722782, -0.016904848, -0.0005699967, 0.023239119, 0.010394643, -0.005401309, -0.009660837, -0.017971948, 0.0023106104, -0.009484206, 0.025547637, 0.015044243, 0.0402656, -0.0111312885) * g_12;
    acc1 += M4(-0.049746856, -0.018338734, -0.049453832, -0.0013227571, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0) * g_13;
    acc2 += M4(0.009532738, 0.026303211, 0.0052090404, 0.011427523, 0.039028667, 0.010359255, 0.056967948, 0.0075689442, 0.044696964, 0.082413465, -0.00041725027, 0.0047600465, -0.022573516, -0.006837194, -0.033555653, 0.005729658) * g_14;
    acc3 += M4(0.04186699, -0.010823237, 0.042962026, -0.003863623, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0) * g_15;
    acc0 += M4(-0.032541882, -0.013543262, -0.039106444, 0.000421735, -0.012176402, -0.009938113, -0.013782902, -0.003169788, 0.01334597, 0.01279018, -0.012351087, -0.0062262868, -0.0042494778, -0.0036080817, -0.0018103335, -0.0011680288) * g_16;
    acc1 += M4(0.022330342, 0.016340692, 0.037003696, 0.00039842215, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0) * g_17;
    acc2 += M4(0.0073578763, 0.00018266705, 0.035575993, 0.00076196797, 0.12603563, 0.13065642, 0.013463504, 0.0048739845, 0.0060395743, 0.01075345, -0.008887093, 0.0034178596, -0.0060950243, 0.0055201515, -0.007540923, -0.001457003) * g_18;
    acc3 += M4(-0.046173826, -0.027478294, -0.05375349, -0.0012074749, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0) * g_19;
    acc0 += M4(0.1166595, 0.014137965, 0.101687744, 0.00065719266, -0.026833449, 0.03727868, -0.0087721, 0.0033499338, -0.009230929, -0.015204777, 0.031615708, -0.0020063764, 0.030981783, -0.007144618, 0.0182652, -0.0036563987) * g_20;
    acc1 += M4(0.041982494, 0.00473427, -0.030853057, -0.0016349736, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0) * g_21;
    acc2 += M4(-0.026400864, -0.016585166, -0.013732271, -0.0006337465, 0.02547592, -0.11623509, 0.0025648684, -0.0016077366, -0.056311566, 0.020223971, -0.12761852, 0.00020625096, -0.12817322, -0.044699345, -0.010041291, 0.0038703096) * g_22;
    acc3 += M4(-0.116473116, -0.0049968953, 0.0519465, 0.0004983531, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0) * g_23;
    vec4 result = vec4(acc0 + acc1 + acc2 + acc3);
    result += vec4(-0.002995659, -0.004558525, -0.0023189238, -0.0036506923);
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
