package io.homo.superresolution.shadercompat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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
        public String target;
    }

    public static class UpscaleConfig {
        public boolean enabled;
        public String before_upscale_shader_name;
        public Map<String, InputTextureConfig> input_textures;
        public Map<String, OutputTextureConfig> output_textures;
    }

    public static class WorldConfig {
        public boolean enabled;
        public UpscaleConfig upscale_config;
    }

    public static class RootConfig {
        public boolean enabled;
        public Map<String, WorldConfig> worlds;
    }

    public RootConfig sr;

    public static SRShaderCompatConfig loadFromJson(File file) throws IOException {
        try (FileReader reader = new FileReader(file)) {
            Gson gson = new GsonBuilder().create();
            return gson.fromJson(reader, SRShaderCompatConfig.class);
        }
    }

    public UpscaleConfig getUpscaleConfigForWorld(String worldId) {
        if (sr == null || sr.worlds == null) return null;
        WorldConfig wc = sr.worlds.get(worldId);
        if (wc != null && wc.upscale_config != null) {
            return wc.upscale_config;
        }
        return sr.worlds.get("*") != null ? sr.worlds.get("*").upscale_config : null;
    }
}
