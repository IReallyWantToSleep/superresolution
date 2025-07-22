package io.homo.superresolution.thirdparty.fsr2.common;

public class Fsr2PipelineResourceType {
    public static final Fsr2PipelineResourceType INPUT_OPAQUE_ONLY = new Fsr2PipelineResourceType(1)
            .setSrvShaderName("r_input_opaque_only");

    public static final Fsr2PipelineResourceType INPUT_COLOR = new Fsr2PipelineResourceType(2)
            .setSrvShaderName("r_input_color_jittered");

    public static final Fsr2PipelineResourceType INPUT_MOTION_VECTORS = new Fsr2PipelineResourceType(3)
            .setSrvShaderName("r_input_motion_vectors");

    public static final Fsr2PipelineResourceType INPUT_DEPTH = new Fsr2PipelineResourceType(4)
            .setSrvShaderName("r_input_depth");

    public static final Fsr2PipelineResourceType INPUT_EXPOSURE = new Fsr2PipelineResourceType(5)
            .setSrvShaderName("r_input_exposure");

    public static final Fsr2PipelineResourceType INPUT_REACTIVE_MASK = new Fsr2PipelineResourceType(6)
            .setSrvShaderName("r_reactive_mask");

    public static final Fsr2PipelineResourceType INPUT_TRANSPARENCY_AND_COMPOSITION_MASK = new Fsr2PipelineResourceType(7)
            .setSrvShaderName("r_transparency_and_composition_mask");

    public static final Fsr2PipelineResourceType RECONSTRUCTED_PREVIOUS_NEAREST_DEPTH = new Fsr2PipelineResourceType(8)
            .setSrvShaderName("r_reconstructed_previous_nearest_depth")
            .setUavShaderName("rw_reconstructed_previous_nearest_depth");

    public static final Fsr2PipelineResourceType DILATED_MOTION_VECTORS = new Fsr2PipelineResourceType(9)
            .setSrvShaderName("r_dilated_motion_vectors")
            .setUavShaderName("rw_dilated_motion_vectors");

    public static final Fsr2PipelineResourceType DILATED_DEPTH = new Fsr2PipelineResourceType(10)
            .setSrvShaderName("r_dilatedDepth")
            .setUavShaderName("rw_dilatedDepth");

    public static final Fsr2PipelineResourceType INTERNAL_UPSCALED_COLOR = new Fsr2PipelineResourceType(11)
            .setSrvShaderName("r_internal_upscaled_color")
            .setUavShaderName("rw_internal_upscaled_color");

    public static final Fsr2PipelineResourceType LOCK_STATUS = new Fsr2PipelineResourceType(12)
            .setSrvShaderName("r_lock_status")
            .setUavShaderName("rw_lock_status");

    public static final Fsr2PipelineResourceType NEW_LOCKS = new Fsr2PipelineResourceType(13)
            .setSrvShaderName("r_new_locks")
            .setUavShaderName("rw_new_locks");

    public static final Fsr2PipelineResourceType PREPARED_INPUT_COLOR = new Fsr2PipelineResourceType(14)
            .setSrvShaderName("r_prepared_input_color")
            .setUavShaderName("rw_prepared_input_color");

    public static final Fsr2PipelineResourceType LUMA_HISTORY = new Fsr2PipelineResourceType(15)
            .setSrvShaderName("r_luma_history")
            .setUavShaderName("rw_luma_history");

    public static final Fsr2PipelineResourceType DEBUG_OUTPUT = new Fsr2PipelineResourceType(16)
            .setUavShaderName("rw_debug_out");

    public static final Fsr2PipelineResourceType LANCZOS_LUT = new Fsr2PipelineResourceType(17)
            .setSrvShaderName("r_lanczos_lut");

    public static final Fsr2PipelineResourceType SPD_ATOMIC_COUNT = new Fsr2PipelineResourceType(18)
            .setUavShaderName("rw_spd_global_atomic");

    public static final Fsr2PipelineResourceType UPSCALED_OUTPUT = new Fsr2PipelineResourceType(19)
            .setUavShaderName("rw_upscaled_output");

    public static final Fsr2PipelineResourceType RCAS_INPUT = new Fsr2PipelineResourceType(20)
            .setSrvShaderName("r_rcas_input");

    public static final Fsr2PipelineResourceType LOCK_STATUS_1 = new Fsr2PipelineResourceType(21);
    public static final Fsr2PipelineResourceType LOCK_STATUS_2 = new Fsr2PipelineResourceType(22);
    public static final Fsr2PipelineResourceType INTERNAL_UPSCALED_COLOR_1 = new Fsr2PipelineResourceType(23);
    public static final Fsr2PipelineResourceType INTERNAL_UPSCALED_COLOR_2 = new Fsr2PipelineResourceType(24);
    public static final Fsr2PipelineResourceType INTERNAL_DEFAULT_REACTIVITY = new Fsr2PipelineResourceType(25);
    public static final Fsr2PipelineResourceType INTERNAL_DEFAULT_TRANSPARENCY_AND_COMPOSITION = new Fsr2PipelineResourceType(26);

    public static final Fsr2PipelineResourceType UPSAMPLE_MAXIMUM_BIAS_LUT = new Fsr2PipelineResourceType(27)
            .setSrvShaderName("r_upsample_maximum_bias_lut");

    public static final Fsr2PipelineResourceType DILATED_REACTIVE_MASKS = new Fsr2PipelineResourceType(28)
            .setSrvShaderName("r_dilated_reactive_masks")
            .setUavShaderName("rw_dilated_reactive_masks");

    public static final Fsr2PipelineResourceType SCENE_LUMINANCE = new Fsr2PipelineResourceType(29)
            .setSrvShaderName("r_imgMips");

    public static final Fsr2PipelineResourceType SCENE_LUMINANCE_MIPMAP_0 = new Fsr2PipelineResourceType(SCENE_LUMINANCE.id());
    public static final Fsr2PipelineResourceType SCENE_LUMINANCE_MIPMAP_1 = new Fsr2PipelineResourceType(30);
    public static final Fsr2PipelineResourceType SCENE_LUMINANCE_MIPMAP_2 = new Fsr2PipelineResourceType(31);
    public static final Fsr2PipelineResourceType SCENE_LUMINANCE_MIPMAP_3 = new Fsr2PipelineResourceType(32);
    public static final Fsr2PipelineResourceType SCENE_LUMINANCE_MIPMAP_4 = new Fsr2PipelineResourceType(33);
    public static final Fsr2PipelineResourceType SCENE_LUMINANCE_MIPMAP_5 = new Fsr2PipelineResourceType(34)
            .setSrvShaderName("r_img_mip_5")
            .setUavShaderName("rw_img_mip_5");
    public static final Fsr2PipelineResourceType SCENE_LUMINANCE_MIPMAP_6 = new Fsr2PipelineResourceType(35);
    public static final Fsr2PipelineResourceType SCENE_LUMINANCE_MIPMAP_7 = new Fsr2PipelineResourceType(36);
    public static final Fsr2PipelineResourceType SCENE_LUMINANCE_MIPMAP_8 = new Fsr2PipelineResourceType(37);
    public static final Fsr2PipelineResourceType SCENE_LUMINANCE_MIPMAP_9 = new Fsr2PipelineResourceType(38);
    public static final Fsr2PipelineResourceType SCENE_LUMINANCE_MIPMAP_10 = new Fsr2PipelineResourceType(39);
    public static final Fsr2PipelineResourceType SCENE_LUMINANCE_MIPMAP_11 = new Fsr2PipelineResourceType(40);
    public static final Fsr2PipelineResourceType SCENE_LUMINANCE_MIPMAP_12 = new Fsr2PipelineResourceType(41);

    public static final Fsr2PipelineResourceType INTERNAL_DEFAULT_EXPOSURE = new Fsr2PipelineResourceType(42);
    public static final Fsr2PipelineResourceType AUTO_EXPOSURE = new Fsr2PipelineResourceType(43)
            .setSrvShaderName("r_auto_exposure")
            .setUavShaderName("rw_auto_exposure");

    public static final Fsr2PipelineResourceType AUTOREACTIVE = new Fsr2PipelineResourceType(44)
            .setUavShaderName("rw_output_autoreactive");

    public static final Fsr2PipelineResourceType AUTOCOMPOSITION = new Fsr2PipelineResourceType(45)
            .setUavShaderName("rw_output_autocomposition");

    public static final Fsr2PipelineResourceType PREV_PRE_ALPHA_COLOR = new Fsr2PipelineResourceType(46)
            .setSrvShaderName("r_input_prev_color_pre_alpha")
            .setUavShaderName("rw_output_prev_color_pre_alpha");

    public static final Fsr2PipelineResourceType PREV_POST_ALPHA_COLOR = new Fsr2PipelineResourceType(47)
            .setSrvShaderName("r_input_prev_color_post_alpha")
            .setUavShaderName("rw_output_prev_color_post_alpha");

    public static final Fsr2PipelineResourceType PREV_PRE_ALPHA_COLOR_1 = new Fsr2PipelineResourceType(48);
    public static final Fsr2PipelineResourceType PREV_POST_ALPHA_COLOR_1 = new Fsr2PipelineResourceType(49);
    public static final Fsr2PipelineResourceType PREV_PRE_ALPHA_COLOR_2 = new Fsr2PipelineResourceType(50);
    public static final Fsr2PipelineResourceType PREV_POST_ALPHA_COLOR_2 = new Fsr2PipelineResourceType(51);

    public static final Fsr2PipelineResourceType PREVIOUS_DILATED_MOTION_VECTORS = new Fsr2PipelineResourceType(52)
            .setSrvShaderName("r_previous_dilated_motion_vectors");

    public static final Fsr2PipelineResourceType INTERNAL_DILATED_MOTION_VECTORS_1 = new Fsr2PipelineResourceType(53);
    public static final Fsr2PipelineResourceType INTERNAL_DILATED_MOTION_VECTORS_2 = new Fsr2PipelineResourceType(54);
    public static final Fsr2PipelineResourceType LUMA_HISTORY_1 = new Fsr2PipelineResourceType(55);
    public static final Fsr2PipelineResourceType LUMA_HISTORY_2 = new Fsr2PipelineResourceType(56);

    public static final Fsr2PipelineResourceType LOCK_INPUT_LUMA = new Fsr2PipelineResourceType(57)
            .setSrvShaderName("r_lock_input_luma")
            .setUavShaderName("rw_lock_input_luma");

    public static final Fsr2PipelineResourceType SCENE_LUMINANCE_MIPMAP_SHADING_CHANGE = new Fsr2PipelineResourceType(33) // SCENE_LUMINANCE_MIPMAP_4.id() = 33
            .setSrvShaderName("r_img_mip_shading_change")
            .setUavShaderName("rw_img_mip_shading_change");

    private final int id;
    private String uavShaderName;
    private String srvShaderName;

    Fsr2PipelineResourceType(int id) {
        this.id = id;
    }

    public String uavShaderName() {
        return uavShaderName;
    }

    public String srvShaderName() {
        return srvShaderName;
    }

    public Fsr2PipelineResourceType setUavShaderName(String uavShaderName) {
        this.uavShaderName = uavShaderName;
        return this;
    }

    public Fsr2PipelineResourceType setSrvShaderName(String srvShaderName) {
        this.srvShaderName = srvShaderName;
        return this;
    }

    public int id() {
        return id;
    }

    public boolean equals(Object other) {
        if (other instanceof Fsr2PipelineResourceType) return this.id == ((Fsr2PipelineResourceType) other).id();
        return false;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}
