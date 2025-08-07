package io.homo.superresolution.shadercompat;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SRShaderCompatConfig {

    public static class TextureConfig {
        public boolean enabled;
        public String src;
        public List<Integer> region;
    }

    public static class UpscaleConfig {
        public boolean enabled;
        public String beforeUpscaleShaderName;
        public Map<String, TextureConfig> inputTextures = new HashMap<>();
    }

    private final Map<String, UpscaleConfig> worldConfigs = new HashMap<>();
    private final Set<String> knownTextures = Set.of(
            "color",
            "depth",
            "motion_vectors"
    );

    public Map<String, UpscaleConfig> getWorldConfigs() {
        return worldConfigs;
    }

    public static SRShaderCompatConfig load(String context) throws IOException {
        Properties props = new Properties();
        props.load(new StringReader(context));
        SRShaderCompatConfig config = new SRShaderCompatConfig();

        Pattern keyPattern = Pattern.compile("^sr\\.(world-?\\d+)\\.(.+)$");

        for (String key : props.stringPropertyNames()) {
            String value = props.getProperty(key).trim();
            Matcher matcher = keyPattern.matcher(key);
            if (!matcher.matches()) continue;

            String worldId = matcher.group(1);
            String subKey = matcher.group(2);

            UpscaleConfig upscale = config.worldConfigs.computeIfAbsent(worldId, k -> new UpscaleConfig());


            if (subKey.equals("enabled")) {
                upscale.enabled = Boolean.parseBoolean(value);
            } else if (subKey.equals("upscale_config.before_upscale_shader_name")) {
                upscale.beforeUpscaleShaderName = value;
            } else {
                Pattern texPattern = Pattern.compile("upscale_config\\.input_textures\\.(\\w+)\\.(enabled|src|region)");
                Matcher texMatcher = texPattern.matcher(subKey);
                if (texMatcher.matches()) {
                    String texName = texMatcher.group(1);
                    String texProp = texMatcher.group(2);

                    if (!config.knownTextures.contains(texName)) continue;

                    TextureConfig texConfig = upscale.inputTextures.computeIfAbsent(texName, k -> new TextureConfig());

                    switch (texProp) {
                        case "enabled":
                            texConfig.enabled = Boolean.parseBoolean(value);
                            break;
                        case "src":
                            texConfig.src = value;
                            break;
                        case "region":
                            texConfig.region = Arrays.stream(value.split(","))
                                    .map(String::trim)
                                    .map(Integer::parseInt)
                                    .toList();
                            break;
                    }
                }
            }
        }
        return config;
    }

    public static SRShaderCompatConfig load(File file) throws IOException {
        return load(Files.readString(file.toPath()));
    }

    public static void main(String[] args) throws IOException {
        File file = new File("G:\\superresolution\\runs\\neoforge\\shaderpacks\\photon\\shaders\\superresolution.properties");
        var a = load(file);
        System.out.println("0");
    }
}
