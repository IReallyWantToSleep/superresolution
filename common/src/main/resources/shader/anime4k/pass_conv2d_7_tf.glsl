// Auto-generated: pass_conv2d_7_tf.glsl
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
layout(set=0, binding=7, rgba32f) uniform writeonly image2D out_conv2d_7_tf;
#else
layout(binding=0) uniform sampler2D conv2d_tf;
layout(binding=1) uniform sampler2D conv2d_1_tf;
layout(binding=2) uniform sampler2D conv2d_2_tf;
layout(binding=3) uniform sampler2D conv2d_3_tf;
layout(binding=4) uniform sampler2D conv2d_4_tf;
layout(binding=5) uniform sampler2D conv2d_5_tf;
layout(binding=6) uniform sampler2D conv2d_6_tf;
layout(binding=7, rgba32f) uniform writeonly image2D out_conv2d_7_tf;
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
    ivec2 out_size = imageSize(out_conv2d_7_tf);
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

    vec4 result = mat4(0.49840534, -0.27439418, 0.23492397, -0.47983184, -0.025539074, 0.07513846, -0.07500975, 0.028990287, 0.12256049, 0.14243355, -0.12348785, -0.13570029, -0.015128587, 0.22891822, -0.21288195, 0.029125331) * g_0;
    result += mat4(-0.61711776, 0.65165913, -0.6276209, 0.60137707, 0.040811766, -0.022425218, 0.026560478, -0.04403384, -0.08499382, -0.08907417, 0.08237511, 0.08986123, 0.002114411, 0.3903695, -0.37956414, 0.0036628488) * g_1;
    result += mat4(0.42920297, 0.34968552, -0.34639058, -0.44392163, 0.27754238, 0.31185222, -0.32145932, -0.26781517, -0.10018203, 0.42185673, -0.41801262, 0.09102399, -0.9260872, -0.83047616, 0.8330451, 0.90285194) * g_2;
    result += mat4(-0.48964477, -0.30090564, 0.28795975, 0.49074662, -0.2898187, -0.30949312, 0.32423037, 0.28735653, 0.06979694, -0.37080827, 0.37262166, -0.08468711, 0.883281, 0.8313333, -0.8268277, -0.8688328) * g_3;
    result += mat4(-0.06258325, 0.062307704, -0.053695668, 0.051820014, 0.027173048, 0.032359015, -0.026604094, -0.025290731, 0.14759159, -0.08727416, 0.09184582, -0.15306252, -0.10878235, -0.06845573, 0.07877027, 0.095256425) * g_4;
    result += mat4(0.07183625, -0.06694184, 0.047080647, -0.047033392, -0.0578789, -0.00535714, 0.0061713103, 0.052350294, -0.15112464, 0.07721164, -0.08919564, 0.13706802, 0.14001346, 0.03183071, -0.03819614, -0.13127115) * g_5;
    result += mat4(-0.0142924385, 0.02050697, 0.009530746, -0.007626505, 0.07835004, -0.04558359, 0.042157937, -0.079061314, 0.092261456, -0.09581087, 0.076048635, -0.078059375, 0.051507134, -0.021826962, 0.02753379, -0.04380138) * g_6;
    result += mat4(0.01781111, -0.0015554995, -0.015265335, -0.0062380894, -0.15631297, -0.0133530125, 0.019194132, 0.14880383, -0.028465312, 0.008359392, 0.01303712, 0.0037185163, -0.033301484, 0.02395837, -0.026924727, 0.033141434) * g_7;
    result += mat4(0.002046132, -0.0067114127, 0.0042218952, -0.0047777356, 0.006151478, -0.02667589, 0.02685031, -0.0053875335, -0.021446217, 0.024302728, -0.026333824, 0.020515949, -0.048180956, 0.02286272, -0.015445096, 0.055391222) * g_8;
    result += mat4(-0.008362805, 0.013929478, -0.022133403, 0.009374011, -0.0075431014, 0.014837879, -0.010900632, -0.00035181196, 0.0085878065, -0.017398061, 0.010062801, -0.0038152467, 0.09283426, 0.03141528, -0.03532359, -0.08317591) * g_9;
    result += mat4(-0.016466662, -0.0040439493, 0.0029286034, 0.020054556, -0.02681229, 0.017074814, -0.015398153, 0.028605254, 0.08542703, 0.054289453, -0.05000763, -0.08847425, -0.07738893, 0.06564518, -0.0663739, 0.08258016) * g_10;
    result += mat4(0.00030723467, 0.0060439985, -0.0021319918, -0.004901246, -0.006261413, -0.008529834, 0.0016315739, 0.01323058, -0.010289119, -0.013662053, 0.011401511, 0.009048159, 0.019917885, -0.0075176735, 0.011048594, -0.024066536) * g_11;
    result += mat4(-0.0013854881, 0.010101165, 0.017365256, -0.025059987, -0.05032766, 0.06698587, 0.026516387, -0.050732236, 0.10618811, -0.085117176, 0.10117938, -0.12486345, 0.017643776, 0.025969386, -0.02716589, -0.020550942) * g_12;
    result += mat4(0.050578844, -0.063863434, -0.05964637, 0.06478122, 0.0069260565, -0.024911102, 0.006212355, 0.012302034, -0.03348743, 0.014547285, -0.02123717, 0.039564583, -0.0521126, -0.09169857, 0.09044547, 0.05881809) * g_13;
    result += vec4(0.013692567, -0.01411724, 0.014968168, -0.012323918);
    imageStore(out_conv2d_7_tf, id.xy, result);

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
