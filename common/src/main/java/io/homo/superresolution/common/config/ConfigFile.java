package io.homo.superresolution.common.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.platform.Platform;
import net.minecraft.client.Minecraft;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class ConfigFile {
    /*
    public static final Path configPath = Path.of(Platform.currentPlatform.getGameFolder().toString(), "config", "superresolution.json");

    public static void write() {
        try (OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(configPath.toString()), StandardCharsets.UTF_8)) {
            osw.write(build());
        } catch (IOException e) {
            SuperResolution.LOGGER.error("配置写入失败 {}", e.toString());
        }
    }

    public static void read() {
        if (!exists()) {
            write();
        }
        String text = readText();
        if (text == null || text.isEmpty()) {
            write();
            text = readText();
        }
        GsonBuilder gsonBuilder = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping();
        Config.registerTypeAdapter(gsonBuilder);
        Gson gson = gsonBuilder.create();
        try {
            ConfigData config = gson.fromJson(text, ConfigData.class);
            Config.setInstance(config);
        } catch (Exception e) {
            SuperResolution.LOGGER.info("读取配置发生错误: {}", e.toString());
            Config.setInstance(new ConfigData());
        }
    }

    public static boolean exists() {
        return configPath.toFile().isFile() &&
                configPath.toFile().exists() &&
                configPath.toFile().canRead() &&
                configPath.toFile().canWrite();
    }

    private static String build() {
        GsonBuilder gsonBuilder = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping();
        Config.registerTypeAdapter(gsonBuilder);
        Gson gson = gsonBuilder.create();
        if (Config.getInstance() == null) {
            return gson.toJson(new ConfigData());
        }
        return gson.toJson(Config.getInstance());
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

    public static void main(String[] args) {
        ConfigFile.read();
        System.out.println("0");
    }
    */
}
