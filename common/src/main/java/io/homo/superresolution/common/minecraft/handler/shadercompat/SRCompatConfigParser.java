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

package io.homo.superresolution.common.minecraft.handler.shadercompat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.minecraft.handler.shadercompat.v1.SRCompatConfigV1Parser;
import io.homo.superresolution.common.minecraft.handler.shadercompat.v1.SRCompatV1Processor;
import io.homo.superresolution.common.minecraft.handler.shadercompat.v2.SRCompatConfigV2Parser;
import io.homo.superresolution.common.minecraft.handler.shadercompat.v2.SRCompatV2Processor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SRCompatConfigParser {
    private static final Gson GSON = new GsonBuilder().create();
    public static final int LATEST_CONFIG_VERSION = 2;
    private static final Pattern SCHEMA_VERSION_PATTERN = Pattern.compile("\"schema_version\"\\s*:\\s*(\\d+)");

    public static SRShaderCompatData load(Path file) {
        return load(file, null);
    }

    public static SRShaderCompatData load(Path file, JsonMacroPreprocessor preprocessor) {
        try {
            if (!Files.exists(file)) return null;
            String jsonContent = Files.readString(file);

            if (preprocessor != null) {
                jsonContent = preprocessor.process(jsonContent);
            }

            JsonObject rootObj = GSON.fromJson(jsonContent, JsonObject.class);

            if (!rootObj.has("schema_version")) {
                SuperResolution.LOGGER.error("无效的光影接口配置：缺少 schema_version 字段。");
                return null;
            }

            int version = rootObj.get("schema_version").getAsInt();

            if (version == 1) {
                return SRCompatConfigV1Parser.parse(rootObj);
            } else if (version == 2) {
                return SRCompatConfigV2Parser.parse(rootObj);
            } else {
                SuperResolution.LOGGER.error("不支持的光影接口配置版本: " + version);
                return null;
            }

        } catch (Exception e) {
            SuperResolution.LOGGER.error("解析光影接口配置文件失败", e);
            return null;
        }
    }

    public static Path findConfigFile(Path root) {
        for (int ver = LATEST_CONFIG_VERSION; ver >= 1; ver--) {
            Path candidate = root.resolve("superresolution.v" + ver + ".json");
            if (Files.exists(candidate)) {
                return candidate;
            }
        }
        Path candidate = root.resolve("superresolution.json");
        if (Files.exists(candidate)) {
            return candidate;
        }
        return null;
    }

    public static int readVersion(Path file) {
        try {
            if (!Files.exists(file)) return -1;
            String content = Files.readString(file);
            Matcher matcher = SCHEMA_VERSION_PATTERN.matcher(content);
            if (matcher.find()) {
                return Integer.parseInt(matcher.group(1));
            }
            SuperResolution.LOGGER.error("无效的光影接口配置：缺少 schema_version 字段。");
            return -1;
        } catch (Exception e) {
            SuperResolution.LOGGER.error("读取光影接口配置版本失败", e);
            return -1;
        }
    }

    public static SRCompatProcessor createProcessor(int version) {
        if (version == 1) return new SRCompatV1Processor();
        if (version == 2) return new SRCompatV2Processor();
        SuperResolution.LOGGER.error("不支持的光影接口配置版本: " + version);
        return null;
    }

}
