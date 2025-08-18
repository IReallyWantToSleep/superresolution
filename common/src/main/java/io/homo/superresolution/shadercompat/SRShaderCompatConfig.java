package io.homo.superresolution.shadercompat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;

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
        public Map<String, InputTextureConfig> input_textures;
        public Map<String, OutputTextureConfig> output_textures;
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
