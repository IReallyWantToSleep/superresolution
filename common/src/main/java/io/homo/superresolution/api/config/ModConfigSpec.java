package io.homo.superresolution.api.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.ConfigSpec;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfigBuilder;
import com.electronwill.nightconfig.core.io.WritingMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class ModConfigSpec {
    public static final Logger LOGGER = LoggerFactory.getLogger("SRConfigAPI");
    protected final CommentedFileConfig configData;
    protected final Map<List<String>, ConfigValue<?>> configValues = new LinkedHashMap<>();
    protected final Map<List<String>, String> comments = new HashMap<>();
    protected final ConfigSpec spec;
    private final ReentrantLock lock = new ReentrantLock();

    protected ModConfigSpec(ModConfigSpecBuilder builder, Path configPath) {
        try {
            if (!Files.exists(configPath)) {
                Files.createDirectories(configPath.getParent());
                Files.createFile(configPath);
                LOGGER.info("Config file not found, creating new default at: {}", configPath);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to create config file:", e);
        }

        CommentedFileConfigBuilder configDataBuilder = CommentedFileConfig.builder(configPath);
        if (builder.autoSave) {
            configDataBuilder.autosave();
        }
        if (builder.autoReload) {
            configDataBuilder.autoreload();
        }
        configDataBuilder.sync();
        configDataBuilder.writingMode(WritingMode.REPLACE);
        this.configData = configDataBuilder.build();
        this.spec = builder.spec;
    }

    protected void fillSpec() {
        configValues.values().forEach((configValue -> configValue.configSpec = this));
        configValues.values().forEach((configValue -> configValue.fillSpec(spec)));
    }

    protected void initializeDefaultValues() {
        spec.correct(configData);
        applyComment();
    }

    public void load() {
        try {
            configData.load();
            if (spec.correct(configData) > 0) {
                configData.save();
            }
        } catch (Exception e) {
            LOGGER.error("Config is corrupted or unreadable, regenerating defaults.", e);
            configData.clear();
            initializeDefaultValues();
            save();
        }
    }

    private void applyComment() {
        for (var commentEntry : comments.entrySet()) {
            configData.setComment(commentEntry.getKey(), commentEntry.getValue());
        }
    }

    public void save() {
        spec.correct(configData);
        applyComment();
        configData.save();
    }

    public CommentedConfig getConfigData() {
        return configData;
    }

    public void close() {
        configData.close();
    }
}
