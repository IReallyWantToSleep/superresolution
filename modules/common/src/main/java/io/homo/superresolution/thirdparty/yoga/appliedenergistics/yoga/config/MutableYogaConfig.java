/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.config;

import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.*;

import java.util.EnumSet;
import java.util.Set;

/**
 * Configuration class for Yoga layout engine.
 */
public class MutableYogaConfig implements io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.config.YogaConfig {
    private YogaCloneNodeFunction cloneNodeCallback = null;
    private io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.config.YogaLogger logger;

    private boolean useWebDefaults = false;

    private int version = 0;
    private EnumSet<YogaExperimentalFeature> experimentalFeatures = EnumSet.noneOf(YogaExperimentalFeature.class);
    private EnumSet<YogaErrata> errata = EnumSet.noneOf(YogaErrata.class);
    private float pointScaleFactor = 1.0f;
    private Object context = null;

    /**
     * Creates a new Config with the specified logger.
     *
     * @param logger The logger to use for this config
     */
    MutableYogaConfig(io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.config.YogaLogger logger) {
        this.logger = logger;
    }

    MutableYogaConfig() {
        this(io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.config.YogaLogger.getDefaultLogger());
    }

    /**
     * Sets whether to use web defaults for layout calculations.
     *
     * @param useWebDefaults true to use web defaults
     */
    public void setUseWebDefaults(boolean useWebDefaults) {
        this.useWebDefaults = useWebDefaults;
    }

    /**
     * Gets whether web defaults are being used.
     *
     * @return true if web defaults are being used
     */
    @Override
    public boolean useWebDefaults() {
        return useWebDefaults;
    }

    /**
     * Checks if an experimental feature is enabled.
     *
     * @param feature The feature to check
     *
     * @return true if the feature is enabled
     */
    @Override
    public boolean isExperimentalFeatureEnabled(YogaExperimentalFeature feature) {
        return experimentalFeatures.contains(feature);
    }

    /**
     * Gets all enabled experimental features.
     *
     * @return BitSet of enabled experimental features
     */
    @Override
    public Set<YogaExperimentalFeature> getEnabledExperiments() {
        return EnumSet.copyOf(experimentalFeatures);
    }

    /**
     * Gets the current errata flags.
     *
     * @return The current errata flags
     */
    @Override
    public EnumSet<YogaErrata> getErrata() {
        return EnumSet.copyOf(errata);
    }

    /**
     * Sets the errata flags.
     *
     * @param errata The errata flags to set
     */
    public void setErrata(Set<YogaErrata> errata) {
        if (!this.errata.equals(errata)) {
            this.errata = EnumSet.copyOf(errata);
            version++;
        }
    }

    /**
     * Sets a single errata flag, replacing any existing flags.
     *
     * @param errata The errata flag to set
     */
    public void setErrata(YogaErrata errata) {
        EnumSet<YogaErrata> newErrata = EnumSet.of(errata);
        if (!this.errata.equals(newErrata)) {
            this.errata = newErrata;
            version++;
        }
    }

    /**
     * Checks if a specific errata flag is set.
     *
     * @param errata The errata flag to check
     *
     * @return true if the errata flag is set
     */
    @Override
    public boolean hasErrata(YogaErrata errata) {
        return this.errata.contains(errata);
    }

    /**
     * Gets the current point scale factor.
     *
     * @return The current point scale factor
     */
    @Override
    public float getPointScaleFactor() {
        return pointScaleFactor;
    }

    /**
     * Sets the point scale factor for layout calculations.
     *
     * @param pointScaleFactor The point scale factor to use
     */
    public void setPointScaleFactor(float pointScaleFactor) {
        if (this.pointScaleFactor != pointScaleFactor) {
            this.pointScaleFactor = pointScaleFactor;
            version++;
        }
    }

    /**
     * Gets the current version of this configuration.
     *
     * @return The current version
     */
    @Override
    public int getVersion() {
        return version;
    }

    /**
     * Logs a message with the configured logger.
     *
     * @param node     The node associated with the log message
     * @param logLevel The log level
     * @param format   The format string
     * @param args     The arguments for the format string
     */
    @Override
    public void log(YogaNode node, LogLevel logLevel, String format, Object... args) {
        if (logger != null) {
            logger.log(this, node, logLevel, format, args);
        }
    }

    /**
     * Clones a node using the configured clone node callback.
     *
     * @param node       The node to clone
     * @param owner      The owner of the node
     * @param childIndex The index of the node in its parent
     *
     * @return The cloned node
     */
    @Override
    public YogaNode cloneNode(YogaNode node, YogaNode owner, int childIndex) {
        YogaNode clone = null;
        if (cloneNodeCallback != null) {
            clone = cloneNodeCallback.cloneNode(node, owner, childIndex);
        }
        if (clone == null) {
            clone = node.cloneWithChildren();
        }
        return clone;
    }

    /**
     * Enables or disables an experimental feature.
     *
     * @param feature The experimental feature to toggle
     * @param enabled true to enable the feature, false to disable
     */
    public void setExperimentalFeatureEnabled(YogaExperimentalFeature feature, boolean enabled) {
        if (isExperimentalFeatureEnabled(feature) != enabled) {
            if (enabled) {
                experimentalFeatures.add(feature);
            } else {
                experimentalFeatures.remove(feature);
            }
            version++;
        }
    }

    public void setErrata(YogaErrata errata, boolean enabled) {
        if (enabled && !this.errata.contains(errata)) {
            this.errata.add(errata);
            version++;
        } else if (!enabled && this.errata.contains(errata)) {
            this.errata.remove(errata);
            version++;
        }
    }

    /**
     * Adds an errata flag.
     *
     * @param errata The errata flag to add
     */
    public void addErrata(YogaErrata errata) {
        if (!hasErrata(errata)) {
            this.errata.add(errata);
            version++;
        }
    }

    /**
     * Removes an errata flag.
     *
     * @param errata The errata flag to remove
     */
    public void removeErrata(YogaErrata errata) {
        if (hasErrata(errata)) {
            this.errata.remove(errata);
            version++;
        }
    }

    /**
     * Gets the context object.
     *
     * @return The context object
     */
    public Object getContext() {
        return context;
    }

    /**
     * Sets a context object for this configuration.
     *
     * @param context The context object to set
     */
    public void setContext(Object context) {
        this.context = context;
    }

    /**
     * Sets the logger for this configuration.
     *
     * @param logger The logger to use
     */
    public void setLogger(io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.config.YogaLogger logger) {
        this.logger = logger;
    }

    /**
     * Sets the clone node callback function.
     *
     * @param cloneNode The callback function to use for cloning nodes
     */
    public void setCloneNodeCallback(YogaCloneNodeFunction cloneNode) {
        this.cloneNodeCallback = cloneNode;
    }

    // Lazy initialization holder class
    static class DefaultConfigHolder {
        static final io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.config.YogaConfig DEFAULT_CONFIG = YogaConfig.create(YogaLogger.getDefaultLogger());
    }
}