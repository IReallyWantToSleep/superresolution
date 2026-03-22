package io.homo.superresolution.common.minecraft.handler.shadercompat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.core.graphics.impl.texture.TextureFormat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SRCompatConfigParser {
    private static final Gson GSON = new GsonBuilder().create();

    public static SRShaderCompatData load(Path file) {
        try {
            if (!Files.exists(file)) return null;
            String jsonContent = Files.readString(file);
            JsonObject rootObj = GSON.fromJson(jsonContent, JsonObject.class);

            if (!rootObj.has("schema_version")) {
                SuperResolution.LOGGER.error("无效的光影接口配置：缺少 schema_version 字段。");
                return null;
            }

            int version = rootObj.get("schema_version").getAsInt();

            if (version == 1) {
                return parseV1(rootObj);
            } else {
                SuperResolution.LOGGER.error("不支持的光影接口配置版本: " + version);
                return null;
            }

        } catch (Exception e) {
            SuperResolution.LOGGER.error("解析光影接口配置文件失败", e);
            return null;
        }
    }

    private static SRShaderCompatData parseV1(JsonObject root) {
        RawSchemaV1 dto = GSON.fromJson(root, RawSchemaV1.class);

        Map<String, SRShaderCompatData.WorldProfile> profiles = new HashMap<>();
        SRShaderCompatData.WorldProfile defaultProfile = null;

        if (dto.profiles != null) {
            for (Map.Entry<String, RawSchemaV1.RawProfile> entry : dto.profiles.entrySet()) {
                String worldKey = entry.getKey();
                RawSchemaV1.RawProfile rawProfile = entry.getValue();
                if (rawProfile == null) {
                    SuperResolution.LOGGER.error("配置错误：profile '{}' 为 null", worldKey);
                    return null;
                }

                // --- 验证 upscale 部分 ---
                SRShaderCompatData.PipelineTrigger trigger = null;
                if (rawProfile.upscale != null && rawProfile.upscale.trigger != null) {
                    RawSchemaV1.RawTrigger rt = rawProfile.upscale.trigger;
                    if (rt.type == null || (!"before".equalsIgnoreCase(rt.type) && !"after".equalsIgnoreCase(rt.type))) {
                        SuperResolution.LOGGER.error("配置错误：profile '{}' 中 upscale.trigger.type 必须为 'before' 或 'after'，但得到: {}", worldKey, rt.type);
                        return null;
                    }
                    if (rt.pass == null || rt.pass.isBlank()) {
                        SuperResolution.LOGGER.error("配置错误：profile '{}' 中 upscale.trigger.pass 不能为空。", worldKey);
                        return null;
                    }

                    SRShaderCompatData.PipelineTrigger.Order order =
                            "before".equalsIgnoreCase(rt.type) ?
                                    SRShaderCompatData.PipelineTrigger.Order.BEFORE :
                                    SRShaderCompatData.PipelineTrigger.Order.AFTER;

                    trigger = new SRShaderCompatData.PipelineTrigger(order, rt.pass);
                }

                SRShaderCompatData.UpscaleConfig upscaleConfig;
                if (rawProfile.upscale != null) {
                    // 校验 internal_format（允许为空，后续使用默认）
                    String internalFormat = rawProfile.upscale.internal_format;
                    if (internalFormat != null && parseTextureFormat(internalFormat) == null) {
                        SuperResolution.LOGGER.error("配置错误：profile '{}' 中 upscale.internal_format 非法: {}", worldKey, internalFormat);
                        return null;
                    }

                    // 校验 inputs
                    if (rawProfile.upscale.inputs != null) {
                        for (Map.Entry<String, RawSchemaV1.RawInputTexture> inEntry : rawProfile.upscale.inputs.entrySet()) {
                            String inKey = inEntry.getKey();
                            RawSchemaV1.RawInputTexture rit = inEntry.getValue();
                            if (rit == null) {
                                SuperResolution.LOGGER.error("配置错误：profile '{}' upscale.inputs.{} 为 null", worldKey, inKey);
                                return null;
                            }
                            if (rit.enabled && (rit.src == null || rit.src.isBlank())) {
                                SuperResolution.LOGGER.error("配置错误：profile '{}' upscale.inputs.{} 启用但未指定 src。", worldKey, inKey);
                                return null;
                            }
                            if (!isValidRegionList(rit.region)) {
                                SuperResolution.LOGGER.error("配置错误：profile '{}' upscale.inputs.{} 的 region 必须为长度为 4 的整数数组。", worldKey, inKey);
                                return null;
                            }
                        }
                    }

                    // 校验 outputs
                    if (rawProfile.upscale.outputs != null) {
                        for (Map.Entry<String, RawSchemaV1.RawOutputTexture> outEntry : rawProfile.upscale.outputs.entrySet()) {
                            String outKey = outEntry.getKey();
                            RawSchemaV1.RawOutputTexture rot = outEntry.getValue();
                            if (rot == null) {
                                SuperResolution.LOGGER.error("配置错误：profile '{}' upscale.outputs.{} 为 null", worldKey, outKey);
                                return null;
                            }
                            if (rot.enabled && (rot.target == null || rot.target.isEmpty())) {
                                SuperResolution.LOGGER.error("配置错误：profile '{}' upscale.outputs.{} 启用但未指定 target。", worldKey, outKey);
                                return null;
                            }
                            if (!isValidRegionList(rot.region)) {
                                SuperResolution.LOGGER.error("配置错误：profile '{}' upscale.outputs.{} 的 region 必须为长度为 4 的整数数组。", worldKey, outKey);
                                return null;
                            }
                        }
                    }

                    // 解析 pre_exposure
                    SRShaderCompatData.SourceConfig preExposureConfig = null;
                    if (rawProfile.upscale.pre_exposure != null) {
                        if (!validateRawSourceConfig(rawProfile.upscale.pre_exposure, worldKey + " upscale.pre_exposure")) return null;
                        // pre_exposure 值类型必须为标量（FLOAT/INT/UINT）
                        SRShaderCompatData.SourceConfig.ValueType vType;
                        try {
                            vType = SRShaderCompatData.SourceConfig.ValueType.fromString(rawProfile.upscale.pre_exposure.type);
                        } catch (IllegalArgumentException ex) {
                            SuperResolution.LOGGER.error("配置错误：profile '{}' upscale.pre_exposure.type 非法: {}", worldKey, ex.getMessage());
                            return null;
                        }
                        if (vType != SRShaderCompatData.SourceConfig.ValueType.FLOAT &&
                                vType != SRShaderCompatData.SourceConfig.ValueType.INT &&
                                vType != SRShaderCompatData.SourceConfig.ValueType.UINT) {
                            SuperResolution.LOGGER.error("配置错误：profile '{}' upscale.pre_exposure.type 必须为标量类型 (float/int/uint)，但得到: {}", worldKey, rawProfile.upscale.pre_exposure.type);
                            return null;
                        }
                        try {
                            preExposureConfig = new SRShaderCompatData.SourceConfig(
                                    rawProfile.upscale.pre_exposure.source,
                                    rawProfile.upscale.pre_exposure.type,
                                    rawProfile.upscale.pre_exposure.value
                            );
                        } catch (IllegalArgumentException ex) {
                            SuperResolution.LOGGER.error("配置错误：profile '{}' upscale.pre_exposure 中存在无效值: {}", worldKey, ex.getMessage());
                            return null;
                        }
                    }
                    boolean autoExposureEffective = !(rawProfile.upscale != null &&
                            rawProfile.upscale.inputs != null &&
                            rawProfile.upscale.inputs.containsKey("exposure"));
                    if (
                            rawProfile.upscale != null &&
                                    rawProfile.upscale.inputs != null &&
                                    rawProfile.upscale.inputs.containsKey("exposure") &&
                                    rawProfile.upscale.auto_exposure
                    ){
                        SuperResolution.LOGGER.warn("配置警告：profile '{}' 中 upscale.auto_exposure 为 true ，但同时启用 exposure 输入纹理，已自动忽略auto_exposure设置，默认为false。", worldKey);
                    }
                    upscaleConfig = new SRShaderCompatData.UpscaleConfig(
                            rawProfile.upscale.enabled,
                            trigger,
                            parseTextureFormat(rawProfile.upscale.internal_format),
                            mapInputTextures(rawProfile.upscale.inputs, worldKey),
                            mapOutputTextures(rawProfile.upscale.outputs, worldKey),
                            preExposureConfig,
                            rawProfile.upscale.hdr,
                            rawProfile.upscale.auto_exposure && autoExposureEffective,
                            rawProfile.upscale.motion_jittered

                    );
                } else {
                    upscaleConfig = new SRShaderCompatData.UpscaleConfig(
                            false,
                            null,
                            TextureFormat.R11G11B10F,
                            new HashMap<>(),
                            new HashMap<>(),
                            null,
                            false,
                            true,
                            false
                    );
                }

                // --- 验证 jitter 部分 ---
                SRShaderCompatData.JitterConfig jitterConfig;
                if (rawProfile.jitter != null) {
                    RawSchemaV1.RawJitter rj = rawProfile.jitter;
                    SRShaderCompatData.JitterConfig.JitterSource jSource = SRShaderCompatData.JitterConfig.JitterSource.MOD;
                    if (rj.source != null) {
                        if ("shaderpack".equalsIgnoreCase(rj.source)) {
                            jSource = SRShaderCompatData.JitterConfig.JitterSource.SHADERPACK;
                        } else if ("mod".equalsIgnoreCase(rj.source)) {
                            jSource = SRShaderCompatData.JitterConfig.JitterSource.MOD;
                        } else {
                            SuperResolution.LOGGER.error("配置错误：profile '{}' jitter.source 必须为 'mod' 或 'shaderpack'，但得到: {}", worldKey, rj.source);
                            return null;
                        }
                    }

                    SRShaderCompatData.JitterSourceConfig sc = null;
                    if (rj.source_config != null) {
                        RawSchemaV1.RawJitterSourceConfig rc = rj.source_config;
                        if (rc.jitter_offset == null) {
                            SuperResolution.LOGGER.error("配置错误：profile '{}' jitter.source_config.jitter_offset 不能为空。", worldKey);
                            return null;
                        }
                        if (rc.jitter_sequence_length == null) {
                            SuperResolution.LOGGER.error("配置错误：profile '{}' jitter.source_config.jitter_sequence_length 不能为空。", worldKey);
                            return null;
                        }

                        // 验证两个 RawSourceConfig 的合法性（包括 source/type/value 的匹配）
                        if (!validateRawSourceConfig(rc.jitter_offset, worldKey + " jitter.source_config.jitter_offset")) return null;
                        if (!validateRawSourceConfig(rc.jitter_sequence_length, worldKey + " jitter.source_config.jitter_sequence_length")) return null;

                        // 创建 SourceConfig（构造函数本身会对 source/type 做一次严格检查）
                        SRShaderCompatData.SourceConfig offsetSC;
                        SRShaderCompatData.SourceConfig seqLenSC;
                        try {
                            offsetSC = new SRShaderCompatData.SourceConfig(
                                    rc.jitter_offset.source,
                                    rc.jitter_offset.type,
                                    rc.jitter_offset.value
                            );
                            seqLenSC = new SRShaderCompatData.SourceConfig(
                                    rc.jitter_sequence_length.source,
                                    rc.jitter_sequence_length.type,
                                    rc.jitter_sequence_length.value
                            );
                        } catch (IllegalArgumentException ex) {
                            SuperResolution.LOGGER.error("配置错误：profile '{}' 的 jitter.source_config 中存在无效的 source/type 值: {}", worldKey, ex.getMessage());
                            return null;
                        }

                        sc = new SRShaderCompatData.JitterSourceConfig(offsetSC, seqLenSC);
                    }

                    jitterConfig = new SRShaderCompatData.JitterConfig(rj.enabled, jSource, sc);
                } else {
                    jitterConfig = new SRShaderCompatData.JitterConfig(false);
                }

                SRShaderCompatData.WorldProfile profile = new SRShaderCompatData.WorldProfile(
                        true,
                        upscaleConfig,
                        jitterConfig
                );

                profiles.put(worldKey, profile);
                if ("*".equals(worldKey)) {
                    defaultProfile = profile;
                }
            }
        }

        return new SRShaderCompatData(1, profiles, defaultProfile);
    }

    private static TextureFormat parseTextureFormat(String formatStr) {
        if (formatStr == null) return null;
        return switch (formatStr.toLowerCase()) {
            case "rgba8" -> TextureFormat.RGBA8;
            case "rgba16f" -> TextureFormat.RGBA16F;
            case "r11g11b10" -> TextureFormat.R11G11B10F;
            default -> null;
        };
    }

    private static Map<String, SRShaderCompatData.InputTexture> mapInputTextures(Map<String, RawSchemaV1.RawInputTexture> source, String worldKey) {
        Map<String, SRShaderCompatData.InputTexture> result = new HashMap<>();
        if (source == null) return result;
        for (Map.Entry<String, RawSchemaV1.RawInputTexture> e : source.entrySet()) {
            String k = e.getKey();
            RawSchemaV1.RawInputTexture v = e.getValue();
            if (v == null) {
                SuperResolution.LOGGER.error("配置错误：profile '{}' upscale.inputs.{} 为 null", worldKey, k);
                return new HashMap<>(); // 已经记录错误，上层会因为之前的校验而提前返回
            }
            if (v.enabled && (v.src == null || v.src.isBlank())) {
                SuperResolution.LOGGER.error("配置错误：profile '{}' upscale.inputs.{} 启用但未指定 src。", worldKey, k);
                return new HashMap<>();
            }
            if (!isValidRegionList(v.region)) {
                SuperResolution.LOGGER.error("配置错误：profile '{}' upscale.inputs.{} 的 region 必须为长度为 4 的整数数组。", worldKey, k);
                return new HashMap<>();
            }
            result.put(k, new SRShaderCompatData.InputTexture(
                    v.enabled,
                    v.src,
                    TextureRegion.fromList(v.region)
            ));
        }
        return result;
    }

    private static Map<String, SRShaderCompatData.OutputTexture> mapOutputTextures(Map<String, RawSchemaV1.RawOutputTexture> source, String worldKey) {
        Map<String, SRShaderCompatData.OutputTexture> result = new HashMap<>();
        if (source == null) return result;
        for (Map.Entry<String, RawSchemaV1.RawOutputTexture> e : source.entrySet()) {
            String k = e.getKey();
            RawSchemaV1.RawOutputTexture v = e.getValue();
            if (v == null) {
                SuperResolution.LOGGER.error("配置错误：profile '{}' upscale.outputs.{} 为 null", worldKey, k);
                return new HashMap<>();
            }
            if (v.enabled && (v.target == null || v.target.isEmpty())) {
                SuperResolution.LOGGER.error("配置错误：profile '{}' upscale.outputs.{} 启用但未指定 target。", worldKey, k);
                return new HashMap<>();
            }
            if (!isValidRegionList(v.region)) {
                SuperResolution.LOGGER.error("配置错误：profile '{}' upscale.outputs.{} 的 region 必须为长度为 4 的整数数组。", worldKey, k);
                return new HashMap<>();
            }
            result.put(k, new SRShaderCompatData.OutputTexture(
                    v.enabled,
                    v.target,
                    TextureRegion.fromList(v.region)
            ));
        }
        return result;
    }

    private static boolean isValidRegionList(List<Integer> region) {
        if (region == null) return false;
        if (region.size() != 4) return false;
        return true;
    }

    private static boolean validateRawSourceConfig(RawSchemaV1.RawSourceConfig rsc, String context) {
        if (rsc == null) {
            SuperResolution.LOGGER.error("配置错误：{} 为 null", context);
            return false;
        }
        if (rsc.source == null || rsc.source.isBlank()) {
            SuperResolution.LOGGER.error("配置错误：{}.source 不能为空", context);
            return false;
        }
        if (rsc.type == null || rsc.type.isBlank()) {
            SuperResolution.LOGGER.error("配置错误：{}.type 不能为空", context);
            return false;
        }
        // 先校验枚举字符串是否合法（利用 SourceConfig 的 fromString 做统一校验）
        try {
            SRShaderCompatData.SourceConfig.SourceType.fromString(rsc.source);
            SRShaderCompatData.SourceConfig.ValueType.fromString(rsc.type);
        } catch (IllegalArgumentException ex) {
            SuperResolution.LOGGER.error("配置错误：{} 中 source/type 非法: {}", context, ex.getMessage());
            return false;
        }

        SRShaderCompatData.SourceConfig.SourceType sType = SRShaderCompatData.SourceConfig.SourceType.fromString(rsc.source);
        SRShaderCompatData.SourceConfig.ValueType vType = SRShaderCompatData.SourceConfig.ValueType.fromString(rsc.type);

        // 校验 value 与 source/type 的匹配
        if (sType == SRShaderCompatData.SourceConfig.SourceType.CONST) {
            if (rsc.value == null) {
                SuperResolution.LOGGER.error("配置错误：{} 为 CONST 时 value 不能为空", context);
                return false;
            }
            // 根据 vType 校验具体类型或数组长度
            switch (vType) {
                case FLOAT, INT, UINT -> {
                    if (!(rsc.value instanceof Number)) {
                        SuperResolution.LOGGER.error("配置错误：{} 为 CONST 且 type 为 {} 时，value 必须是数字", context, vType);
                        return false;
                    }
                }
                case VECTOR2F -> {
                    if (!(rsc.value instanceof List)) {
                        SuperResolution.LOGGER.error("配置错误：{} 为 CONST 且 type 为 VECTOR2F 时，value 必须为长度为 2 的数值数组", context);
                        return false;
                    }
                    List<?> list = (List<?>) rsc.value;
                    if (list.size() != 2) {
                        SuperResolution.LOGGER.error("配置错误：{} 为 CONST 且 type 为 VECTOR2F 时，value 长度应为 2，实际: {}", context, list.size());
                        return false;
                    }
                    if (!allElementsAreNumbers(list)) {
                        SuperResolution.LOGGER.error("配置错误：{} 为 CONST 且 type 为 VECTOR2F 时，value 中的元素必须为数字", context);
                        return false;
                    }
                }
                case VECTOR3F -> {
                    if (!(rsc.value instanceof List)) {
                        SuperResolution.LOGGER.error("配置错误：{} 为 CONST 且 type 为 VECTOR3F 时，value 必须为长度为 3 的数值数组", context);
                        return false;
                    }
                    List<?> list = (List<?>) rsc.value;
                    if (list.size() != 3) {
                        SuperResolution.LOGGER.error("配置错误：{} 为 CONST 且 type 为 VECTOR3F 时，value 长度应为 3，实际: {}", context, list.size());
                        return false;
                    }
                    if (!allElementsAreNumbers(list)) {
                        SuperResolution.LOGGER.error("配置错误：{} 为 CONST 且 type 为 VECTOR3F 时，value 中的元素必须为数字", context);
                        return false;
                    }
                }
                case VECTOR4F -> {
                    if (!(rsc.value instanceof List)) {
                        SuperResolution.LOGGER.error("配置错误：{} 为 CONST 且 type 为 VECTOR4F 时，value 必须为长度为 4 的数值数组", context);
                        return false;
                    }
                    List<?> list = (List<?>) rsc.value;
                    if (list.size() != 4) {
                        SuperResolution.LOGGER.error("配置错误：{} 为 CONST 且 type 为 VECTOR4F 时，value 长度应为 4，实际: {}", context, list.size());
                        return false;
                    }
                    if (!allElementsAreNumbers(list)) {
                        SuperResolution.LOGGER.error("配置错误：{} 为 CONST 且 type 为 VECTOR4F 时，value 中的元素必须为数字", context);
                        return false;
                    }
                }
            }
        } else {
            // VARIABLE 或 UNIFORM 时 value 必须是 String 表示变量/uniform 名称
            if (!(rsc.value instanceof String)) {
                SuperResolution.LOGGER.error("配置错误：{} 为 {} 时，value 必须为字符串，表示变量或 uniform 名称", context, rsc.source);
                return false;
            }
            if (((String) rsc.value).isBlank()) {
                SuperResolution.LOGGER.error("配置错误：{} 的 value 不能为空字符串", context);
                return false;
            }
        }

        return true;
    }

    private static boolean allElementsAreNumbers(List<?> list) {
        for (Object o : list) {
            if (!(o instanceof Number)) return false;
        }
        return true;
    }

    private static Map<String, SRShaderCompatData.InputTexture> mapInputTextures(Map<String, RawSchemaV1.RawInputTexture> source) {
        return mapInputTextures(source, "unknown");
    }

    private static Map<String, SRShaderCompatData.OutputTexture> mapOutputTextures(Map<String, RawSchemaV1.RawOutputTexture> source) {
        return mapOutputTextures(source, "unknown");
    }

    private static class RawSchemaV1 {
        int schema_version;
        Map<String, RawProfile> profiles;

        static class RawProfile {
            RawUpscale upscale;
            RawJitter jitter;
        }

        static class RawUpscale {
            boolean enabled;
            RawTrigger trigger;
            String internal_format;
            Map<String, RawInputTexture> inputs;
            Map<String, RawOutputTexture> outputs;
            RawSourceConfig pre_exposure;
            boolean hdr;
            boolean auto_exposure;
            boolean motion_jittered;
        }

        static class RawTrigger {
            String type; // "before" | "after"
            String pass;
        }

        static class RawJitter {
            boolean enabled;
            String source; // "mod" | "shaderpack"
            RawJitterSourceConfig source_config;
        }

        static class RawJitterSourceConfig {
            RawSourceConfig jitter_offset;
            RawSourceConfig jitter_sequence_length;
        }

        static class RawSourceConfig {
            String source; // const/variable/uniform
            String type; // float,int,uint,vector2f,vector3f,vector4f
            Object value; // single value or array
        }

        static class RawInputTexture {
            boolean enabled;
            String src;
            List<Integer> region;
        }

        static class RawOutputTexture {
            boolean enabled;
            List<String> target;
            List<Integer> region;
        }
    }
}