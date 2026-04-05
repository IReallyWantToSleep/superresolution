// Auto-generated: pass_conv2d_7_tf1.glsl
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
layout(set=0, binding=7, rgba32f) uniform writeonly image2D out_conv2d_7_tf1;
#else
layout(binding=0) uniform sampler2D conv2d_tf;
layout(binding=1) uniform sampler2D conv2d_1_tf;
layout(binding=2) uniform sampler2D conv2d_2_tf;
layout(binding=3) uniform sampler2D conv2d_3_tf;
layout(binding=4) uniform sampler2D conv2d_4_tf;
layout(binding=5) uniform sampler2D conv2d_5_tf;
layout(binding=6) uniform sampler2D conv2d_6_tf;
layout(binding=7, rgba32f) uniform writeonly image2D out_conv2d_7_tf1;
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
    ivec2 out_size = imageSize(out_conv2d_7_tf1);
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

    vec4 result = mat4(-0.3827926, 0.1056839, -0.1289203, 0.38390785, -0.07724042, 0.02444138, -0.020446423, 0.08220423, -0.024638154, -0.12143138, 0.132206, 0.018706175, -0.045040097, 0.033647083, -0.021546327, 0.062334843) * g_0;
    result += mat4(0.32324916, -0.20762728, 0.21125656, -0.3162517, 0.024672946, -0.056018572, 0.06015832, -0.02943064, 0.007822833, 0.084045626, -0.08619084, -0.0050252257, -0.057595015, -0.027210819, 0.036963586, 0.06508716) * g_1;
    result += mat4(-0.05942627, -0.14783436, 0.15771991, 0.031499412, -0.15033798, -0.034240987, 0.030918758, 0.15237325, 0.104463406, -0.033642303, 0.030783184, -0.105458185, 0.19290254, 0.20379338, -0.21437173, -0.19155405) * g_2;
    result += mat4(0.015473907, 0.17550263, -0.19501981, -0.0013130337, 0.16203764, 0.038646486, -0.02897594, -0.15797648, -0.0821177, 0.042028215, -0.0341048, 0.05912176, -0.14742067, -0.28225517, 0.2982789, 0.14052054) * g_3;
    result += mat4(0.004493929, -0.0013769517, 0.011132162, -0.016023653, -0.06653748, 0.09170429, -0.08694396, 0.06876474, -0.016375715, 0.057579603, -0.055227786, 0.011393711, -0.035041302, -0.1154558, 0.11978173, 0.027483884) * g_4;
    result += mat4(-0.007911614, 0.019920643, -0.0399401, 0.032802556, 0.044665556, -0.073698655, 0.074479125, -0.04870769, 0.03429395, -0.09358368, 0.084313355, -0.050635695, 0.04906905, 0.10101571, -0.101483345, -0.048283197) * g_5;
    result += mat4(-0.0019719766, 0.013823704, 0.01670039, -0.020906566, -0.05427769, 0.018107574, -0.016823547, 0.048507586, 0.053844664, -0.05124477, 0.029477859, -0.038191713, 0.040334877, -0.011559442, 0.016308451, -0.031786084) * g_6;
    result += mat4(0.00062903645, -0.0022749042, -0.015295893, 0.011038592, -0.035221785, -0.06967963, 0.072812565, 0.031398028, 0.024308402, -0.05115432, 0.07441121, -0.050560996, -0.017839627, 0.0060721124, -0.008919023, 0.018216275) * g_7;
    result += mat4(-0.005193545, 0.0010102271, -0.0028523852, 0.0019597225, 0.015474986, -0.029404918, 0.028906407, -0.013784307, -0.027386874, 0.029147783, -0.029591048, 0.024827974, 0.0348289, -0.01639418, 0.020734334, -0.023485536) * g_8;
    result += mat4(-0.0050119217, 0.009296215, -0.016431872, 0.0052447766, -0.013346897, 0.019797867, -0.015190317, 0.0044413195, 0.0097761275, -0.018406617, 0.011642559, -0.0056654033, 0.015850553, 0.07141924, -0.07145242, -0.01063347) * g_9;
    result += mat4(-0.021503784, -0.0063448916, 0.0067279567, 0.023648351, -0.0173211, 0.020173592, -0.020280607, 0.021406414, 0.07843685, 0.06703231, -0.06199878, -0.082190335, -0.08597943, 0.061986525, -0.061703674, 0.089531325) * g_10;
    result += mat4(0.0026935616, 0.009677848, -0.0071270335, -0.005918894, -0.013004139, -0.012061882, 0.0063774474, 0.018483663, -0.011361797, -0.010031685, 0.007879762, 0.009868346, 0.02547185, -0.0011619271, 0.0026722867, -0.02742248) * g_11;
    result += mat4(0.0028551104, 0.008633299, 0.019463744, -0.029608142, -0.051027913, 0.06346373, 0.031523198, -0.052014265, 0.10916206, -0.08234259, 0.0989019, -0.128073, 0.019213166, 0.025646238, -0.028078414, -0.02086073) * g_12;
    result += mat4(0.045057014, -0.06702061, -0.0584342, 0.072332665, 0.0066350233, -0.022453366, 0.0046991715, 0.011824346, -0.032265235, 0.011746478, -0.018223295, 0.03805277, -0.054543532, -0.090831876, 0.090957575, 0.059684325) * g_13;
    result += vec4(-0.005613256, 0.0029789752, -0.0016761658, 0.0064466638);
    imageStore(out_conv2d_7_tf1, id.xy, result);

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
