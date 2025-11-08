/*
 * Super Resolution
 * Copyright (c) 2025. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.config;

import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.LogLevel;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaErrata;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaExperimentalFeature;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaNode;

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
