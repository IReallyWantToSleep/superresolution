// Auto-generated: pass_conv2d_7_tf2.glsl
#version 450
layout(local_size_x=16, local_size_y=16, local_size_z=1) in;

#ifdef VULKAN
layout(set=0, binding=0) uniform sampler2D conv2d_tf;
layout(set=0, binding=1) uniform sampler2D conv2d_1_tf;
layout(set=0, binding=2) uniform sampler2D conv2d_2_tf;
layout(set=0, binding=3) uniform sampler2D conv2d_3_tf;
layout(set=0, binding=4) uniform sampler2D conv2d_4_tf;
layout(set=0, binding=5) uniform sampler2D conv2d_5_tf;
layout(set=0, binding=6) uniform sampler2D conv2d_6_tf;
layout(set=0, binding=7, rgba32f) uniform writeonly image2D out_conv2d_7_tf2;
#else
layout(binding=0) uniform sampler2D conv2d_tf;
layout(binding=1) uniform sampler2D conv2d_1_tf;
layout(binding=2) uniform sampler2D conv2d_2_tf;
layout(binding=3) uniform sampler2D conv2d_3_tf;
layout(binding=4) uniform sampler2D conv2d_4_tf;
layout(binding=5) uniform sampler2D conv2d_5_tf;
layout(binding=6) uniform sampler2D conv2d_6_tf;
layout(binding=7, rgba32f) uniform writeonly image2D out_conv2d_7_tf2;
#endif

#define conv2d_tf_off(x_off, y_off) texture(conv2d_tf, coord + vec2(x_off, y_off) / vec2(textureSize(conv2d_tf, 0)))
#define conv2d_1_tf_off(x_off, y_off) texture(conv2d_1_tf, coord + vec2(x_off, y_off) / vec2(textureSize(conv2d_1_tf, 0)))
#define conv2d_2_tf_off(x_off, y_off) texture(conv2d_2_tf, coord + vec2(x_off, y_off) / vec2(textureSize(conv2d_2_tf, 0)))
#define conv2d_3_tf_off(x_off, y_off) texture(conv2d_3_tf, coord + vec2(x_off, y_off) / vec2(textureSize(conv2d_3_tf, 0)))
#define conv2d_4_tf_off(x_off, y_off) texture(conv2d_4_tf, coord + vec2(x_off, y_off) / vec2(textureSize(conv2d_4_tf, 0)))
#define conv2d_5_tf_off(x_off, y_off) texture(conv2d_5_tf, coord + vec2(x_off, y_off) / vec2(textureSize(conv2d_5_tf, 0)))
#define conv2d_6_tf_off(x_off, y_off) texture(conv2d_6_tf, coord + vec2(x_off, y_off) / vec2(textureSize(conv2d_6_tf, 0)))

void main() {
    ivec3 id = ivec3(gl_GlobalInvocationID);
    ivec2 out_size = imageSize(out_conv2d_7_tf2);
    if (id.x >= out_size.x || id.y >= out_size.y) return;
    vec2 coord = (vec2(id.xy) + 0.5) / vec2(out_size);

    #define g_0 (max((texelFetch(conv2d_tf, id.xy, 0)), 0.0))
    #define g_1 (max(-(texelFetch(conv2d_tf, id.xy, 0)), 0.0))
    #define g_2 (max((texelFetch(conv2d_1_tf, id.xy, 0)), 0.0))
    #define g_3 (max(-(texelFetch(conv2d_1_tf, id.xy, 0)), 0.0))
    #define g_4 (max((texelFetch(conv2d_2_tf, id.xy, 0)), 0.0))
    #define g_5 (max(-(texelFetch(conv2d_2_tf, id.xy, 0)), 0.0))
    #define g_6 (max((texelFetch(conv2d_3_tf, id.xy, 0)), 0.0))
    #define g_7 (max(-(texelFetch(conv2d_3_tf, id.xy, 0)), 0.0))
    #define g_8 (max((texelFetch(conv2d_4_tf, id.xy, 0)), 0.0))
    #define g_9 (max(-(texelFetch(conv2d_4_tf, id.xy, 0)), 0.0))
    #define g_10 (max((texelFetch(conv2d_5_tf, id.xy, 0)), 0.0))
    #define g_11 (max(-(texelFetch(conv2d_5_tf, id.xy, 0)), 0.0))
    #define g_12 (max((texelFetch(conv2d_6_tf, id.xy, 0)), 0.0))
    #define g_13 (max(-(texelFetch(conv2d_6_tf, id.xy, 0)), 0.0))

    vec4 result = mat4(0.26193246, -0.08897548, 0.061267443, -0.25502548, 0.34976056, -0.23517999, 0.22908741, -0.33819968, -0.4249045, -0.02897288, 0.053624775, 0.41125336, 0.3971896, -0.3364118, 0.34063658, -0.37734294) * g_0;
    result += mat4(-0.07136134, -0.31795704, 0.32975668, 0.06889778, -0.31952834, 0.14152569, -0.13179311, 0.31275147, 0.32171547, -0.03425337, 0.022895504, -0.31451315, 0.30312592, -0.4612237, 0.4638263, -0.29220816) * g_1;
    result += mat4(-0.9816334, -0.024063079, 0.037096597, 0.9639992, 0.078762755, -0.43782622, 0.42966, -0.06912469, 0.16942404, -0.39688957, 0.38650587, -0.16447635, 1.1599281, 0.5623576, -0.5874199, -1.1653886) * g_2;
    result += mat4(0.9495419, 0.05636359, -0.08134436, -0.9442565, -0.08434423, 0.43788254, -0.42345452, 0.07958486, -0.13317876, 0.415651, -0.4051089, 0.10869326, -1.1106733, -0.6096609, 0.6273834, 1.1237148) * g_3;
    result += mat4(0.031352982, 0.00622903, 0.0036376764, -0.044433158, -0.035468835, 0.07380877, -0.069309734, 0.039340217, -0.013770726, 0.035111934, -0.033351034, 0.010768963, -0.038448665, -0.1150351, 0.12009107, 0.02952756) * g_4;
    result += mat4(-0.036781523, -0.01044535, -0.006835061, 0.05920159, 0.022898888, -0.055695392, 0.056318935, -0.02758009, 0.036217816, -0.07555528, 0.07113846, -0.058904808, 0.03691926, 0.12880903, -0.13339037, -0.03178197) * g_5;
    result += mat4(-0.013955112, 0.03642308, -0.014084271, -0.0008459607, -0.008795388, 0.05006217, -0.04790876, 0.0035736726, 0.020451684, -0.056573924, 0.03668147, -0.0058956905, 0.019986568, 0.0068013207, 0.005485099, -0.018898444) * g_6;
    result += mat4(0.009726417, -0.024946058, 0.006819042, 0.002484902, -0.08513574, -0.06589352, 0.06967759, 0.0805339, 0.043680657, -0.016297236, 0.029656224, -0.06117106, 0.0056375926, -0.015530999, 0.0124292625, -0.0049519944) * g_7;
    result += mat4(0.002798859, -0.0038149385, 0.000955886, -0.005212997, 0.03420297, -0.046834014, 0.04461829, -0.031058373, -0.04256447, 0.044710428, -0.044202473, 0.039831605, 0.019566847, -0.069656156, 0.07339084, -0.008098615) * g_8;
    result += mat4(-0.011527557, 0.013887073, -0.017742066, 0.008177835, -0.027441531, 0.03506497, -0.031518754, 0.019462634, 0.033777516, -0.03414522, 0.02818709, -0.030482791, 0.010973496, 0.10447376, -0.10082309, -0.008164301) * g_9;
    result += mat4(-0.01885671, 0.0009849315, -0.0015023381, 0.022677526, -0.014264845, 0.005822911, -0.0096330745, 0.021688119, 0.06982261, 0.071110725, -0.068196446, -0.07124916, -0.06952884, 0.06564147, -0.06405388, 0.07185985) * g_10;
    result += mat4(0.015655048, 0.0007768749, 0.0018361825, -0.018139424, -0.014990514, -0.005449472, 0.00017803397, 0.020288127, -0.01508169, 0.0031788666, -0.0046446812, 0.0127716195, 0.0145331, -0.0021388829, 0.0037945886, -0.01677192) * g_11;
    result += mat4(-0.0015318524, 0.0076194108, 0.015957681, -0.020762932, -0.04897591, 0.051835787, 0.03068236, -0.041540552, 0.09874874, -0.07975051, 0.09337921, -0.11495881, 0.012116897, 0.0161503, -0.018404935, -0.014013282) * g_12;
    result += mat4(0.039060988, -0.05458328, -0.054484475, 0.06221836, 0.0033581867, -0.010600756, -0.0038673924, 0.011965157, -0.0350179, 0.0153771145, -0.02011272, 0.039087705, -0.05464301, -0.07609463, 0.078616396, 0.05697894) * g_13;
    result += vec4(-0.009726034, 0.0137573425, -0.012296629, 0.010552005);
    imageStore(out_conv2d_7_tf2, id.xy, result);

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
    #undef conv2d_tf_off
    #undef conv2d_1_tf_off
    #undef conv2d_2_tf_off
    #undef conv2d_3_tf_off
    #undef conv2d_4_tf_off
    #undef conv2d_5_tf_off
    #undef conv2d_6_tf_off
}
