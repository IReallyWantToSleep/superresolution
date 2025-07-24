package io.homo.superresolution.thirdparty.asr2;

public class Asr2PipelineResourceType {
    public static final Asr2PipelineResourceType INPUT_OPAQUE_ONLY = new Asr2PipelineResourceType(1)
            .setSrvShaderName("r_input_opaque_only");

    public static final Asr2PipelineResourceType INPUT_COLOR = new Asr2PipelineResourceType(2)
            .setSrvShaderName("r_input_color_jittered");

    public static final Asr2PipelineResourceType INPUT_MOTION_VECTORS = new Asr2PipelineResourceType(3)
            .setSrvShaderName("r_input_motion_vectors");

    public static final Asr2PipelineResourceType INPUT_DEPTH = new Asr2PipelineResourceType(4)
            .setSrvShaderName("r_input_depth");

    public static final Asr2PipelineResourceType INPUT_EXPOSURE = new Asr2PipelineResourceType(5)
            .setSrvShaderName("r_input_exposure");

    public static final Asr2PipelineResourceType INPUT_REACTIVE_MASK = new Asr2PipelineResourceType(6)
            .setSrvShaderName("r_reactive_mask");

    public static final Asr2PipelineResourceType INPUT_TRANSPARENCY_AND_COMPOSITION_MASK = new Asr2PipelineResourceType(7)
            .setSrvShaderName("r_transparency_and_composition_mask");

    public static final Asr2PipelineResourceType RECONSTRUCTED_PREVIOUS_NEAREST_DEPTH = new Asr2PipelineResourceType(8)
            .setSrvShaderName("r_reconstructed_previous_nearest_depth")
            .setUavShaderName("rw_reconstructed_previous_nearest_depth");

    public static final Asr2PipelineResourceType DILATED_MOTION_VECTORS = new Asr2PipelineResourceType(9)
            .setSrvShaderName("r_dilated_motion_vectors")
            .setUavShaderName("rw_dilated_motion_vectors");

    public static final Asr2PipelineResourceType DILATED_DEPTH = new Asr2PipelineResourceType(10)
            .setSrvShaderName("r_dilatedDepth")
            .setUavShaderName("rw_dilatedDepth");

    public static final Asr2PipelineResourceType INTERNAL_UPSCALED_COLOR = new Asr2PipelineResourceType(11)
            .setSrvShaderName("r_internal_upscaled_color")
            .setUavShaderName("rw_internal_upscaled_color");

    public static final Asr2PipelineResourceType LOCK_STATUS = new Asr2PipelineResourceType(12)
            .setSrvShaderName("r_lock_status")
            .setUavShaderName("rw_lock_status");

    public static final Asr2PipelineResourceType NEW_LOCKS = new Asr2PipelineResourceType(13)
            .setSrvShaderName("r_new_locks")
            .setUavShaderName("rw_new_locks");

    public static final Asr2PipelineResourceType PREPARED_INPUT_COLOR = new Asr2PipelineResourceType(14)
            .setSrvShaderName("r_prepared_input_color")
            .setUavShaderName("rw_prepared_input_color");

    public static final Asr2PipelineResourceType LUMA_HISTORY = new Asr2PipelineResourceType(15)
            .setSrvShaderName("r_luma_history")
            .setUavShaderName("rw_luma_history");

    public static final Asr2PipelineResourceType DEBUG_OUTPUT = new Asr2PipelineResourceType(16)
            .setUavShaderName("rw_debug_out");

    public static final Asr2PipelineResourceType LANCZOS_LUT = new Asr2PipelineResourceType(17)
            .setSrvShaderName("r_lanczos_lut");

    public static final Asr2PipelineResourceType SPD_ATOMIC_COUNT = new Asr2PipelineResourceType(18)
            .setUavShaderName("rw_spd_global_atomic");

    public static final Asr2PipelineResourceType UPSCALED_OUTPUT = new Asr2PipelineResourceType(19)
            .setUavShaderName("rw_upscaled_output");

    public static final Asr2PipelineResourceType RCAS_INPUT = new Asr2PipelineResourceType(20)
            .setSrvShaderName("r_rcas_input");

    public static final Asr2PipelineResourceType LOCK_STATUS_1 = new Asr2PipelineResourceType(21);
    public static final Asr2PipelineResourceType LOCK_STATUS_2 = new Asr2PipelineResourceType(22);
    public static final Asr2PipelineResourceType INTERNAL_UPSCALED_COLOR_1 = new Asr2PipelineResourceType(23);
    public static final Asr2PipelineResourceType INTERNAL_UPSCALED_COLOR_2 = new Asr2PipelineResourceType(24);
    public static final Asr2PipelineResourceType INTERNAL_DEFAULT_REACTIVITY = new Asr2PipelineResourceType(25);
    public static final Asr2PipelineResourceType INTERNAL_DEFAULT_TRANSPARENCY_AND_COMPOSITION = new Asr2PipelineResourceType(26);

    public static final Asr2PipelineResourceType UPSAMPLE_MAXIMUM_BIAS_LUT = new Asr2PipelineResourceType(27)
            .setSrvShaderName("r_upsample_maximum_bias_lut");

    public static final Asr2PipelineResourceType DILATED_REACTIVE_MASKS = new Asr2PipelineResourceType(28)
            .setSrvShaderName("r_dilated_reactive_masks")
            .setUavShaderName("rw_dilated_reactive_masks");

    public static final Asr2PipelineResourceType SCENE_LUMINANCE = new Asr2PipelineResourceType(29)
            .setSrvShaderName("r_imgMips");

    public static final Asr2PipelineResourceType SCENE_LUMINANCE_MIPMAP_0 = new Asr2PipelineResourceType(SCENE_LUMINANCE.id());
    public static final Asr2PipelineResourceType SCENE_LUMINANCE_MIPMAP_1 = new Asr2PipelineResourceType(30);
    public static final Asr2PipelineResourceType SCENE_LUMINANCE_MIPMAP_2 = new Asr2PipelineResourceType(31);
    public static final Asr2PipelineResourceType SCENE_LUMINANCE_MIPMAP_3 = new Asr2PipelineResourceType(32);
    public static final Asr2PipelineResourceType SCENE_LUMINANCE_MIPMAP_4 = new Asr2PipelineResourceType(33);
    public static final Asr2PipelineResourceType SCENE_LUMINANCE_MIPMAP_5 = new Asr2PipelineResourceType(34)
            .setSrvShaderName("r_img_mip_5")
            .setUavShaderName("rw_img_mip_5");
    public static final Asr2PipelineResourceType SCENE_LUMINANCE_MIPMAP_6 = new Asr2PipelineResourceType(35);
    public static final Asr2PipelineResourceType SCENE_LUMINANCE_MIPMAP_7 = new Asr2PipelineResourceType(36);
    public static final Asr2PipelineResourceType SCENE_LUMINANCE_MIPMAP_8 = new Asr2PipelineResourceType(37);
    public static final Asr2PipelineResourceType SCENE_LUMINANCE_MIPMAP_9 = new Asr2PipelineResourceType(38);
    public static final Asr2PipelineResourceType SCENE_LUMINANCE_MIPMAP_10 = new Asr2PipelineResourceType(39);
    public static final Asr2PipelineResourceType SCENE_LUMINANCE_MIPMAP_11 = new Asr2PipelineResourceType(40);
    public static final Asr2PipelineResourceType SCENE_LUMINANCE_MIPMAP_12 = new Asr2PipelineResourceType(41);

    public static final Asr2PipelineResourceType INTERNAL_DEFAULT_EXPOSURE = new Asr2PipelineResourceType(42);
    public static final Asr2PipelineResourceType AUTO_EXPOSURE = new Asr2PipelineResourceType(43)
            .setSrvShaderName("r_auto_exposure")
            .setUavShaderName("rw_auto_exposure");

    public static final Asr2PipelineResourceType AUTOREACTIVE = new Asr2PipelineResourceType(44)
            .setUavShaderName("rw_output_autoreactive");

    public static final Asr2PipelineResourceType AUTOCOMPOSITION = new Asr2PipelineResourceType(45)
            .setUavShaderName("rw_output_autocomposition");

    public static final Asr2PipelineResourceType PREV_PRE_ALPHA_COLOR = new Asr2PipelineResourceType(46)
            .setSrvShaderName("r_input_prev_color_pre_alpha")
            .setUavShaderName("rw_output_prev_color_pre_alpha");

    public static final Asr2PipelineResourceType PREV_POST_ALPHA_COLOR = new Asr2PipelineResourceType(47)
            .setSrvShaderName("r_input_prev_color_post_alpha")
            .setUavShaderName("rw_output_prev_color_post_alpha");

    public static final Asr2PipelineResourceType PREV_PRE_ALPHA_COLOR_1 = new Asr2PipelineResourceType(48);
    public static final Asr2PipelineResourceType PREV_POST_ALPHA_COLOR_1 = new Asr2PipelineResourceType(49);
    public static final Asr2PipelineResourceType PREV_PRE_ALPHA_COLOR_2 = new Asr2PipelineResourceType(50);
    public static final Asr2PipelineResourceType PREV_POST_ALPHA_COLOR_2 = new Asr2PipelineResourceType(51);

    public static final Asr2PipelineResourceType PREVIOUS_DILATED_MOTION_VECTORS = new Asr2PipelineResourceType(52)
            .setSrvShaderName("r_previous_dilated_motion_vectors");

    public static final Asr2PipelineResourceType INTERNAL_DILATED_MOTION_VECTORS_1 = new Asr2PipelineResourceType(53);
    public static final Asr2PipelineResourceType INTERNAL_DILATED_MOTION_VECTORS_2 = new Asr2PipelineResourceType(54);
    public static final Asr2PipelineResourceType LUMA_HISTORY_1 = new Asr2PipelineResourceType(55);
    public static final Asr2PipelineResourceType LUMA_HISTORY_2 = new Asr2PipelineResourceType(56);

    public static final Asr2PipelineResourceType LOCK_INPUT_LUMA = new Asr2PipelineResourceType(57)
            .setSrvShaderName("r_lock_input_luma")
            .setUavShaderName("rw_lock_input_luma");

    public static final Asr2PipelineResourceType SCENE_LUMINANCE_MIPMAP_SHADING_CHANGE = new Asr2PipelineResourceType(33) // SCENE_LUMINANCE_MIPMAP_4.id() = 33
            .setSrvShaderName("r_img_mip_shading_change")
            .setUavShaderName("rw_img_mip_shading_change");

    private final int id;
    private String uavShaderName;
    private String srvShaderName;

    Asr2PipelineResourceType(int id) {
        this.id = id;
    }

    public String uavShaderName() {
        return uavShaderName;
    }

    public String srvShaderName() {
        return srvShaderName;
    }

    public Asr2PipelineResourceType setUavShaderName(String uavShaderName) {
        this.uavShaderName = uavShaderName;
        return this;
    }

    public Asr2PipelineResourceType setSrvShaderName(String srvShaderName) {
        this.srvShaderName = srvShaderName;
        return this;
    }

    public int id() {
        return id;
    }

    public boolean equals(Object other) {
        if (other instanceof Asr2PipelineResourceType) return this.id == ((Asr2PipelineResourceType) other).id();
        return false;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}
