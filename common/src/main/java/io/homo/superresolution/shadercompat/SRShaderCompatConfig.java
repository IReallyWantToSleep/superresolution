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

package io.homo.superresolution.shadercompat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.homo.superresolution.core.graphics.impl.texture.TextureFormat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class SRShaderCompatConfig {

    public static class TextureConfig {
        public boolean enabled;
        public List<Integer> region;
    }

    public static class InputTextureConfig extends TextureConfig {
        public String src;
    }

    public static class OutputTextureConfig extends TextureConfig {
        public List<String> target;
    }

    public static class WorldUpscaleConfig {
        public String before_upscale_shader_name;
        public String sr_internal_texture_format;
        public Map<String, InputTextureConfig> input_textures;
        public Map<String, OutputTextureConfig> output_textures;

        public TextureFormat getSrInternalTextureFormat() {
            return switch (sr_internal_texture_format) {
                case "r11b11g10f" -> TextureFormat.R11G11B10F;
                case "rgb8" -> TextureFormat.RGB8;
                case "rgba8" -> TextureFormat.RGBA8;
                case "rgba16f" -> TextureFormat.RGBA16F;
                case "rgba16" -> TextureFormat.RGBA16;
                case "rgb16f" -> TextureFormat.RGB16F;
                default -> TextureFormat.R11G11B10F;
            };
        }
    }

    public static class WorldConfig {
        public boolean enabled;
        public WorldUpscaleConfig upscale_config;
    }

    public static class UpscaleConfig {
        public boolean enabled;
        public Map<String, WorldConfig> worlds;
    }

    public static class WorldUpscaleJitterConfig {
        public boolean enabled;
    }

    public static class UpscaleJitterConfig {
        public boolean enabled;
        public Map<String, WorldUpscaleJitterConfig> worlds;
    }

    public UpscaleConfig sr;
    public UpscaleJitterConfig sr_jitter;

    public static SRShaderCompatConfig loadFromJson(Path file) throws IOException {
        Gson gson = new GsonBuilder().create();
        return gson.fromJson(Files.readString(file), SRShaderCompatConfig.class);
    }

    public WorldConfig getUpscaleConfigForWorld(String worldId) {
        if (sr == null || sr.worlds == null) return null;
        worldId = worldId.replace("world", "");
        WorldConfig wc = sr.worlds.get(worldId);
        if (wc != null && wc.upscale_config != null) {
            return wc;
        }
        return sr.worlds.get("*") != null ? sr.worlds.get("*") : null;
    }

    public WorldUpscaleJitterConfig getUpscaleJitterConfigForWorld(String worldId) {
        if (sr_jitter == null || sr_jitter.worlds == null) return null;
        worldId = worldId.replace("world", "");
        WorldUpscaleJitterConfig wc = sr_jitter.worlds.get(worldId);
        if (wc != null) {
            return wc;
        }
        return sr_jitter.worlds.get("*") != null ? sr_jitter.worlds.get("*") : null;
    }
}
