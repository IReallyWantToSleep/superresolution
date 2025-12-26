/*
 * Super Resolution
 * Copyright (c) 2025. 187J3X1-114514
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
                SRShaderCompatData.PipelineTrigger trigger = null;
                if (rawProfile.upscale != null && rawProfile.upscale.trigger != null) {
                    SRShaderCompatData.PipelineTrigger.Order order =
                            "before".equalsIgnoreCase(rawProfile.upscale.trigger.type) ?
                                    SRShaderCompatData.PipelineTrigger.Order.BEFORE :
                                    SRShaderCompatData.PipelineTrigger.Order.AFTER;

                    trigger = new SRShaderCompatData.PipelineTrigger(
                            order,
                            rawProfile.upscale.trigger.pass
                    );
                }
                SRShaderCompatData.UpscaleConfig upscaleConfig;
                if (rawProfile.upscale != null) {
                    upscaleConfig = new SRShaderCompatData.UpscaleConfig(
                            rawProfile.upscale.enabled,
                            trigger,
                            parseTextureFormat(rawProfile.upscale.internal_format),
                            mapInputTextures(rawProfile.upscale.inputs),
                            mapOutputTextures(rawProfile.upscale.outputs)
                    );
                } else {
                    upscaleConfig = new SRShaderCompatData.UpscaleConfig(
                            false,
                            null,
                            TextureFormat.R11G11B10F,
                            new HashMap<>(),
                            new HashMap<>()
                    );
                }
                SRShaderCompatData.JitterConfig jitterConfig;
                if (rawProfile.jitter != null) {
                    jitterConfig = new SRShaderCompatData.JitterConfig(rawProfile.jitter.enabled);
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
        if (formatStr == null) return TextureFormat.R11G11B10F;
        return switch (formatStr.toLowerCase()) {
            case "rgb8" -> TextureFormat.RGB8;
            case "rgba8" -> TextureFormat.RGBA8;
            case "rgba16f" -> TextureFormat.RGBA16F;
            case "rgba16" -> TextureFormat.RGBA16;
            case "rgb16f" -> TextureFormat.RGB16F;
            default -> TextureFormat.R11G11B10F;
        };
    }

    private static Map<String, SRShaderCompatData.InputTexture> mapInputTextures(Map<String, RawSchemaV1.RawInputTexture> source) {
        Map<String, SRShaderCompatData.InputTexture> result = new HashMap<>();
        if (source == null) return result;
        source.forEach((k, v) -> result.put(k, new SRShaderCompatData.InputTexture(
                v.enabled,
                v.src,
                TextureRegion.fromList(v.region)
        )));
        return result;
    }

    private static Map<String, SRShaderCompatData.OutputTexture> mapOutputTextures(Map<String, RawSchemaV1.RawOutputTexture> source) {
        Map<String, SRShaderCompatData.OutputTexture> result = new HashMap<>();
        if (source == null) return result;
        source.forEach((k, v) -> result.put(k, new SRShaderCompatData.OutputTexture(
                v.enabled,
                v.target,
                TextureRegion.fromList(v.region)
        )));
        return result;
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
        }

        static class RawTrigger {
            String type; // "before" | "after"
            String pass;
        }

        static class RawJitter {
            boolean enabled;
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
