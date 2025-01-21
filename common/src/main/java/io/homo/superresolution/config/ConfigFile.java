package io.homo.superresolution.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.homo.superresolution.SuperResolution;
import net.minecraft.client.Minecraft;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class ConfigFile {
    public static final Path configPath = Path.of(Minecraft.getInstance().gameDirectory.toString(), "config", "superresolution.json");

    public static void write() {
        try (OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(configPath.toString()), StandardCharsets.UTF_8)) {
            osw.write(build());
        } catch (IOException e) {
            SuperResolution.LOGGER.error("配置写入失败 {}", e.toString());
        }
    }

    public static ConfigData read() {
        if (!exists()) {
            write();
            return Config.buildData();
        }
        ConfigData data = Config.buildData();
        String text = readText();
        if (text == null) {
            return data;
        }
        Gson gson = new Gson();
        JsonElement json = gson.fromJson(text, JsonElement.class);
        if (json.isJsonObject()) {
            JsonObject jsonObject = json.getAsJsonObject();
            data.setSharpness(getJsonElementFloat(jsonObject, "sharpness", ConfigData.defaultConfig.sharpness));
            data.setUpscaleRatio(getJsonElementFloat(jsonObject, "upscaleRatio", ConfigData.defaultConfig.upscaleRatio));
            data.setUpscaleAlgo(getJsonElementString(jsonObject, "upscaleAlgo", ConfigData.defaultConfig.upscaleAlgo));
            data.setEnableUpscale(getJsonElementBoolean(jsonObject, "enableUpscale", ConfigData.defaultConfig.enableUpscale));
        } else {
            SuperResolution.LOGGER.error("配置读取失败");
        }
        return data;
    }

    private static boolean getJsonElementBoolean(JsonObject jsonObject, String name, boolean d) {
        return jsonObject.get(name) != null ? jsonObject.get(name).getAsBoolean() : d;
    }

    private static float getJsonElementFloat(JsonObject jsonObject, String name, float d) {
        return jsonObject.get(name) != null ? jsonObject.get(name).getAsFloat() : d;
    }

    private static String getJsonElementString(JsonObject jsonObject, String name, String d) {
        return jsonObject.get(name) != null ? jsonObject.get(name).getAsString() : d;
    }

    public static boolean exists() {
        return configPath.toFile().isFile() &&
                configPath.toFile().exists() &&
                configPath.toFile().canRead() &&
                configPath.toFile().canWrite();
    }

    private static String build() {
        Map<String, Object> config = new HashMap<>();
        ConfigData data = Config.buildData();
        config.put("sharpness", data.sharpness);
        config.put("upscaleRatio", data.upscaleRatio);
        config.put("upscaleAlgo", data.upscaleAlgo);
        config.put("enableUpscale", data.enableUpscale);
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.setPrettyPrinting();
        gsonBuilder.disableHtmlEscaping();
        Gson gson = gsonBuilder.create();
        return gson.toJson(config);
    }

    private static String readText() {
        try (BufferedReader br = new BufferedReader(new FileReader(configPath.toString(), StandardCharsets.UTF_8))) {
            StringBuilder jsonBuilder = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                jsonBuilder.append(line).append("\n");
            }
            return jsonBuilder.toString();
        } catch (IOException e) {
            SuperResolution.LOGGER.error("读取配置文件失败 {}", e.toString());
        }
        return null;
    }
}
