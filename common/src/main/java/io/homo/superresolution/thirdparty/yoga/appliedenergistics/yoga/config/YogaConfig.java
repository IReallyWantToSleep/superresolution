package io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.config;

import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.LogLevel;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaErrata;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaExperimentalFeature;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaNode;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.config.MutableYogaConfig;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.config.YogaLogger;

import java.util.EnumSet;
import java.util.Set;

public interface YogaConfig {
    /**
     * Determines if moving a node from an old to new config should dirty previously
     * calculated layout results.
     *
     * @param oldConfig The old configuration
     * @param newConfig The new configuration
     * @return true if layout needs to be recalculated
     */
    static boolean configUpdateInvalidatesLayout(YogaConfig oldConfig, YogaConfig newConfig) {
        return !oldConfig.getErrata().equals(newConfig.getErrata()) ||
                !oldConfig.getEnabledExperiments().equals(newConfig.getEnabledExperiments()) ||
                oldConfig.getPointScaleFactor() != newConfig.getPointScaleFactor() ||
                oldConfig.useWebDefaults() != newConfig.useWebDefaults();
    }

    /**
     * Gets the default configuration.
     *
     * @return The default configuration
     */
    static YogaConfig getDefault() {
        return MutableYogaConfig.DefaultConfigHolder.DEFAULT_CONFIG;
    }

    static MutableYogaConfig create() {
        return new MutableYogaConfig();
    }

    static MutableYogaConfig create(YogaLogger logger) {
        return new MutableYogaConfig(logger);
    }

    boolean useWebDefaults();

    boolean isExperimentalFeatureEnabled(YogaExperimentalFeature feature);

    Set<YogaExperimentalFeature> getEnabledExperiments();

    EnumSet<YogaErrata> getErrata();

    boolean hasErrata(YogaErrata errata);

    float getPointScaleFactor();

    int getVersion();

    void log(YogaNode node, LogLevel logLevel, String format, Object... args);

    YogaNode cloneNode(YogaNode node, YogaNode owner, int childIndex);
}
