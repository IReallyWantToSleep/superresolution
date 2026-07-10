package io.homo.superresolution.core.streamline;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class StreamlineInitConfig {
    public final boolean showConsole;
    public final int logLevel;
    public final long preferenceFlags;
    public final String[] pluginPaths;
    public final String logPath;
    public final int[] features;
    public final int applicationId;
    public final int engine;
    public final String engineVersion;
    public final String projectId;
    public final StreamlineLogListener logListener;

    private StreamlineInitConfig(Builder builder) {
        this.showConsole = builder.showConsole;
        this.logLevel = builder.logLevel;
        this.preferenceFlags = builder.preferenceFlags;
        this.pluginPaths = builder.pluginPaths.toArray(String[]::new);
        this.logPath = builder.logPath;
        this.features = Arrays.copyOf(builder.features, builder.features.length);
        this.applicationId = builder.applicationId;
        this.engine = builder.engine;
        this.engineVersion = builder.engineVersion;
        this.projectId = builder.projectId;
        this.logListener = builder.logListener;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static StreamlineInitConfig defaultDlss(Path pluginPath, Path logPath) {
        return builder()
                .pluginPath(pluginPath)
                .logPath(logPath)
                .features(StreamlineFeature.DLSS)
                .build();
    }

    public static final class Builder {
        private boolean showConsole;
        private int logLevel = StreamlineTypes.LogLevel.DEFAULT;
        private long preferenceFlags = StreamlineTypes.PreferenceFlags.DISABLE_COMMAND_LIST_STATE_TRACKING
                | StreamlineTypes.PreferenceFlags.ALLOW_OTA
                | StreamlineTypes.PreferenceFlags.LOAD_DOWNLOADED_PLUGINS
                | StreamlineTypes.PreferenceFlags.USE_FRAME_BASED_RESOURCE_TAGGING;
        private final List<String> pluginPaths = new ArrayList<>();
        private String logPath;
        private int[] features = {StreamlineFeature.DLSS};
        private int applicationId;
        private int engine = StreamlineTypes.EngineType.CUSTOM;
        private String engineVersion = "SuperResolution";
        private String projectId = "3a799712-b54a-407c-82b0-eb3366f0f1e3";
        private StreamlineLogListener logListener;

        public Builder showConsole(boolean value) {
            showConsole = value;
            return this;
        }

        public Builder logLevel(int value) {
            logLevel = value;
            return this;
        }

        public Builder preferenceFlags(long value) {
            preferenceFlags = value;
            return this;
        }

        public Builder pluginPath(Path value) {
            return pluginPath(value == null ? null : value.toAbsolutePath().toString());
        }

        public Builder pluginPath(String value) {
            if (value != null && !value.isBlank()) {
                pluginPaths.add(value);
            }
            return this;
        }

        public Builder pluginPaths(List<Path> values) {
            pluginPaths.clear();
            if (values != null) {
                for (Path value : values) {
                    pluginPath(value);
                }
            }
            return this;
        }

        public Builder logPath(Path value) {
            logPath = value == null ? null : value.toAbsolutePath().toString();
            return this;
        }

        public Builder logPath(String value) {
            logPath = value;
            return this;
        }

        public Builder features(int... values) {
            if (values == null || values.length == 0) {
                throw new IllegalArgumentException("At least one Streamline feature must be requested");
            }
            features = Arrays.copyOf(values, values.length);
            return this;
        }

        public Builder applicationId(int value) {
            applicationId = value;
            return this;
        }

        public Builder engine(int type, String version) {
            engine = type;
            engineVersion = version;
            return this;
        }

        public Builder projectId(String value) {
            projectId = value;
            return this;
        }

        public Builder logListener(StreamlineLogListener value) {
            logListener = value;
            return this;
        }

        public StreamlineInitConfig build() {
            if (applicationId == 0 && (engineVersion == null || engineVersion.isBlank())) {
                throw new IllegalStateException("engineVersion is required when applicationId is not set");
            }
            return new StreamlineInitConfig(this);
        }
    }
}
