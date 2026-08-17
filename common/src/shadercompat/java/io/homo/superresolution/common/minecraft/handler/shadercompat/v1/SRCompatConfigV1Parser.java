/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.homo.superresolution.common.minecraft.handler.shadercompat.v1;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.minecraft.handler.shadercompat.SRShaderCompatData;
import io.homo.superresolution.common.minecraft.handler.shadercompat.TextureRegion;
import io.homo.superresolution.core.graphics.impl.texture.TextureFormat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SRCompatConfigV1Parser {
    private static final Gson GSON = new GsonBuilder().create();

    public static SRShaderCompatData parse(JsonObject root) {
        RawSchemaV1 dto = GSON.fromJson(root, RawSchemaV1.class);

        Map<String, SRShaderCompatData.WorldProfile> profiles = new HashMap<>();
        SRShaderCompatData.WorldProfile defaultProfile = null;

        if (dto.profiles != null) {
            for (Map.Entry<String, RawSchemaV1.RawProfile> entry : dto.profiles.entrySet()) {
                String worldKey = entry.getKey();
                RawSchemaV1.RawProfile rawProfile = entry.getValue();
                if (rawProfile == null) {
                    SuperResolution.LOGGER.error("Configuration error: profile '{}' is null", worldKey);
                    return null;
                }

                // --- 验证 upscale 部分 ---
                SRShaderCompatData.PipelineTrigger trigger = null;
                if (rawProfile.upscale != null && rawProfile.upscale.trigger != null) {
                    RawSchemaV1.RawTrigger rt = rawProfile.upscale.trigger;
                    if (rt.type == null || (!"before".equalsIgnoreCase(rt.type) && !"after".equalsIgnoreCase(rt.type))) {
                        SuperResolution.LOGGER.error("Configuration error: profile '{}' upscale.trigger.type must be 'before' or 'after', but was: {}", worldKey, rt.type);
                        return null;
                    }
                    if (rt.pass == null || rt.pass.isBlank()) {
                        SuperResolution.LOGGER.error("Configuration error: profile '{}' upscale.trigger.pass must not be empty.", worldKey);
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
                        SuperResolution.LOGGER.error("Configuration error: profile '{}' has an invalid upscale.internal_format: {}", worldKey, internalFormat);
                        return null;
                    }

                    // 校验 inputs
                    if (rawProfile.upscale.inputs != null) {
                        for (Map.Entry<String, RawSchemaV1.RawInputTexture> inEntry : rawProfile.upscale.inputs.entrySet()) {
                            String inKey = inEntry.getKey();
                            RawSchemaV1.RawInputTexture rit = inEntry.getValue();
                            if (rit == null) {
                                SuperResolution.LOGGER.error("Configuration error: profile '{}' upscale.inputs.{} is null", worldKey, inKey);
                                return null;
                            }
                            if (rit.enabled && (rit.src == null || rit.src.isBlank())) {
                                SuperResolution.LOGGER.error("Configuration error: profile '{}' upscale.inputs.{} is enabled but src is not specified.", worldKey, inKey);
                                return null;
                            }
                            if (!isValidRegionList(rit.region)) {
                                SuperResolution.LOGGER.error("Configuration error: profile '{}' upscale.inputs.{} region must be an integer array of length 4.", worldKey, inKey);
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
                                SuperResolution.LOGGER.error("Configuration error: profile '{}' upscale.outputs.{} is null", worldKey, outKey);
                                return null;
                            }
                            if (rot.enabled && (rot.target == null || rot.target.isEmpty())) {
                                SuperResolution.LOGGER.error("Configuration error: profile '{}' upscale.outputs.{} is enabled but target is not specified.", worldKey, outKey);
                                return null;
                            }
                            if (!isValidRegionList(rot.region)) {
                                SuperResolution.LOGGER.error("Configuration error: profile '{}' upscale.outputs.{} region must be an integer array of length 4.", worldKey, outKey);
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
                            SuperResolution.LOGGER.error("Configuration error: profile '{}' has an invalid upscale.pre_exposure.type: {}", worldKey, ex.getMessage());
                            return null;
                        }
                        if (vType != SRShaderCompatData.SourceConfig.ValueType.FLOAT &&
                                vType != SRShaderCompatData.SourceConfig.ValueType.INT &&
                                vType != SRShaderCompatData.SourceConfig.ValueType.UINT) {
                            SuperResolution.LOGGER.error("Configuration error: profile '{}' upscale.pre_exposure.type must be a scalar type (float/int/uint), but was: {}", worldKey, rawProfile.upscale.pre_exposure.type);
                            return null;
                        }
                        try {
                            preExposureConfig = new SRShaderCompatData.SourceConfig(
                                    rawProfile.upscale.pre_exposure.source,
                                    rawProfile.upscale.pre_exposure.type,
                                    rawProfile.upscale.pre_exposure.value
                            );
                        } catch (IllegalArgumentException ex) {
                            SuperResolution.LOGGER.error("Configuration error: profile '{}' upscale.pre_exposure contains invalid values: {}", worldKey, ex.getMessage());
                            return null;
                        }
                    }
                    RawSchemaV1.RawInputTexture exposureInput =
                            rawProfile.upscale.inputs == null
                                    ? null
                                    : rawProfile.upscale.inputs.get("exposure");
                    boolean externalExposureEnabled =
                            exposureInput != null && exposureInput.enabled;
                    boolean autoExposureEffective = !externalExposureEnabled;
                    if (
                            externalExposureEnabled &&
                                    rawProfile.upscale.auto_exposure
                    ){
                        SuperResolution.LOGGER.warn("Configuration warning: profile '{}' has upscale.auto_exposure set to true while the exposure input texture is enabled; ignoring auto_exposure and defaulting it to false.", worldKey);
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
                            SuperResolution.LOGGER.error("Configuration error: profile '{}' jitter.source must be 'mod' or 'shaderpack', but was: {}", worldKey, rj.source);
                            return null;
                        }
                    }

                    SRShaderCompatData.JitterSourceConfig sc = null;
                    if (rj.source_config != null) {
                        RawSchemaV1.RawJitterSourceConfig rc = rj.source_config;
                        if (rc.jitter_offset == null) {
                            SuperResolution.LOGGER.error("Configuration error: profile '{}' jitter.source_config.jitter_offset must not be empty.", worldKey);
                            return null;
                        }
                        if (rc.jitter_sequence_length == null) {
                            SuperResolution.LOGGER.error("Configuration error: profile '{}' jitter.source_config.jitter_sequence_length must not be empty.", worldKey);
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
                            SuperResolution.LOGGER.error("Configuration error: profile '{}' jitter.source_config contains invalid source/type values: {}", worldKey, ex.getMessage());
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

        return new SRShaderCompatData(1, profiles, defaultProfile, new SRCompatV1Processor());
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
                SuperResolution.LOGGER.error("Configuration error: profile '{}' upscale.inputs.{} is null", worldKey, k);
                return new HashMap<>();
            }
            if (v.enabled && (v.src == null || v.src.isBlank())) {
                SuperResolution.LOGGER.error("Configuration error: profile '{}' upscale.inputs.{} is enabled but src is not specified.", worldKey, k);
                return new HashMap<>();
            }
            if (!isValidRegionList(v.region)) {
                SuperResolution.LOGGER.error("Configuration error: profile '{}' upscale.inputs.{} region must be an integer array of length 4.", worldKey, k);
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
                SuperResolution.LOGGER.error("Configuration error: profile '{}' upscale.outputs.{} is null", worldKey, k);
                return new HashMap<>();
            }
            if (v.enabled && (v.target == null || v.target.isEmpty())) {
                SuperResolution.LOGGER.error("Configuration error: profile '{}' upscale.outputs.{} is enabled but target is not specified.", worldKey, k);
                return new HashMap<>();
            }
            if (!isValidRegionList(v.region)) {
                SuperResolution.LOGGER.error("Configuration error: profile '{}' upscale.outputs.{} region must be an integer array of length 4.", worldKey, k);
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
            SuperResolution.LOGGER.error("Configuration error: {} is null", context);
            return false;
        }
        if (rsc.source == null || rsc.source.isBlank()) {
            SuperResolution.LOGGER.error("Configuration error: {}.source must not be empty", context);
            return false;
        }
        if (rsc.type == null || rsc.type.isBlank()) {
            SuperResolution.LOGGER.error("Configuration error: {}.type must not be empty", context);
            return false;
        }
        try {
            SRShaderCompatData.SourceConfig.SourceType.fromString(rsc.source);
            SRShaderCompatData.SourceConfig.ValueType.fromString(rsc.type);
        } catch (IllegalArgumentException ex) {
            SuperResolution.LOGGER.error("Configuration error: {} has an invalid source/type: {}", context, ex.getMessage());
            return false;
        }

        SRShaderCompatData.SourceConfig.SourceType sType = SRShaderCompatData.SourceConfig.SourceType.fromString(rsc.source);
        SRShaderCompatData.SourceConfig.ValueType vType = SRShaderCompatData.SourceConfig.ValueType.fromString(rsc.type);

        if (sType == SRShaderCompatData.SourceConfig.SourceType.CONST) {
            if (rsc.value == null) {
                SuperResolution.LOGGER.error("Configuration error: {} uses CONST but value is empty", context);
                return false;
            }
            switch (vType) {
                case FLOAT, INT, UINT -> {
                    if (!(rsc.value instanceof Number)) {
                        SuperResolution.LOGGER.error("Configuration error: {} uses CONST with type {}; value must be numeric", context, vType);
                        return false;
                    }
                }
                case VECTOR2F -> {
                    if (!(rsc.value instanceof List)) {
                        SuperResolution.LOGGER.error("Configuration error: {} uses CONST with type VECTOR2F; value must be a numeric array of length 2", context);
                        return false;
                    }
                    List<?> list = (List<?>) rsc.value;
                    if (list.size() != 2) {
                        SuperResolution.LOGGER.error("Configuration error: {} uses CONST with type VECTOR2F; value must contain 2 elements, but has: {}", context, list.size());
                        return false;
                    }
                    if (!allElementsAreNumbers(list)) {
                        SuperResolution.LOGGER.error("Configuration error: {} uses CONST with type VECTOR2F; value elements must be numeric", context);
                        return false;
                    }
                }
                case VECTOR3F -> {
                    if (!(rsc.value instanceof List)) {
                        SuperResolution.LOGGER.error("Configuration error: {} uses CONST with type VECTOR3F; value must be a numeric array of length 3", context);
                        return false;
                    }
                    List<?> list = (List<?>) rsc.value;
                    if (list.size() != 3) {
                        SuperResolution.LOGGER.error("Configuration error: {} uses CONST with type VECTOR3F; value must contain 3 elements, but has: {}", context, list.size());
                        return false;
                    }
                    if (!allElementsAreNumbers(list)) {
                        SuperResolution.LOGGER.error("Configuration error: {} uses CONST with type VECTOR3F; value elements must be numeric", context);
                        return false;
                    }
                }
                case VECTOR4F -> {
                    if (!(rsc.value instanceof List)) {
                        SuperResolution.LOGGER.error("Configuration error: {} uses CONST with type VECTOR4F; value must be a numeric array of length 4", context);
                        return false;
                    }
                    List<?> list = (List<?>) rsc.value;
                    if (list.size() != 4) {
                        SuperResolution.LOGGER.error("Configuration error: {} uses CONST with type VECTOR4F; value must contain 4 elements, but has: {}", context, list.size());
                        return false;
                    }
                    if (!allElementsAreNumbers(list)) {
                        SuperResolution.LOGGER.error("Configuration error: {} uses CONST with type VECTOR4F; value elements must be numeric", context);
                        return false;
                    }
                }
            }
        } else {
            if (!(rsc.value instanceof String)) {
                SuperResolution.LOGGER.error("Configuration error: {} uses {}; value must be a string representing a variable or uniform name", context, rsc.source);
                return false;
            }
            if (((String) rsc.value).isBlank()) {
                SuperResolution.LOGGER.error("Configuration error: {}.value must not be an empty string", context);
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
            String type;
            String pass;
        }

        static class RawJitter {
            boolean enabled;
            String source;
            RawJitterSourceConfig source_config;
        }

        static class RawJitterSourceConfig {
            RawSourceConfig jitter_offset;
            RawSourceConfig jitter_sequence_length;
        }

        static class RawSourceConfig {
            String source;
            String type;
            Object value;
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
