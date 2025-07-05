package io.homo.superresolution.api.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfigBuilder;
import com.electronwill.nightconfig.core.io.WritingMode;
import io.homo.superresolution.api.config.values.ConfigValue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ModConfigSpec {
    protected final CommentedFileConfig configData;
    protected final Map<List<String>, ConfigValue<?>> configValues = new LinkedHashMap<>();
    protected final Map<List<String>, String> comments = new HashMap<>();

    protected ModConfigSpec(ModConfigSpecBuilder builder, Path configPath) {
        CommentedFileConfigBuilder configDataBuilder = CommentedFileConfig.builder(configPath);
        if (builder.autoSave) {
            configDataBuilder.autosave();
        }
        if (builder.autoReload) {
            configDataBuilder.autoreload();
        }
        configDataBuilder.onFileNotFound((file, configFormat) -> {
            Files.createDirectories(file.getParent());
            initializeDefaultValues();
            return true;
        });
        configDataBuilder.writingMode(WritingMode.REPLACE);
        this.configData = configDataBuilder.build();
    }

    protected void initializeDefaultValues() {
        applyComment();
        for (Map.Entry<List<String>, ConfigValue<?>> entry : configValues.entrySet()) {
            List<String> path = entry.getKey();
            ConfigValue<?> value = entry.getValue();
            Object defaultValue = value.getDefault();
            setConfigValue(path, defaultValue);
        }
    }

    private void setConfigValue(List<String> path, Object value) {
        Config current = configData;
        for (int i = 0; i < path.size() - 1; i++) {
            String segment = path.get(i);
            if (!current.contains(segment)) {
                current.add(segment, CommentedConfig.inMemory());
            }
            current = current.get(segment);
        }

        current.set(path.get(path.size() - 1), value);
    }

    public void load() {
        configData.load();
        for (Map.Entry<List<String>, ConfigValue<?>> entry : configValues.entrySet()) {
            List<String> path = entry.getKey();
            ConfigValue<?> value = entry.getValue();
            if (configData.contains(path)) {
                Object configValue = configData.get(path);
                if (value.isValid(configValue)) {
                    value.set(configValue);
                } else {
                    System.err.println("Invalid value for config path " + path + ": " + configValue);
                    value.resetToDefault();
                }
            } else {
                value.resetToDefault();
            }
        }
    }

    private void applyComment() {
        for (var commentEntry : comments.entrySet()) {
            configData.setComment(commentEntry.getKey(), commentEntry.getValue());
        }
    }

    public void save() {
        applyComment();
        for (Map.Entry<List<String>, ConfigValue<?>> entry : configValues.entrySet()) {
            List<String> path = entry.getKey();
            ConfigValue<?> value = entry.getValue();
            setConfigValue(path, value.get());
        }
        configData.save();
    }

    public CommentedConfig getConfigData() {
        return configData;
    }

    public void close() {
        configData.close();
    }

}