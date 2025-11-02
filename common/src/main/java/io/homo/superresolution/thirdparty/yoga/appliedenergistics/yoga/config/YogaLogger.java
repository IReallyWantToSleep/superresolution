/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.config;

import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.LogLevel;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaNode;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.config.YogaConfig;

/**
 * Interface for logging events from the yoga layout engine.
 */
@FunctionalInterface
public interface YogaLogger {
    /**
     * Log a message from the yoga layout engine.
     *
     * @param config The config that triggered the log
     * @param node   The node associated with the log
     * @param level  The log level
     * @param format The format string
     * @param args   Arguments for the format string
     */
    void log(YogaConfig config, YogaNode node, LogLevel level, String format, Object... args);

    /**
     * Returns the default logger implementation.
     *
     * @return The default logger
     */
    static YogaLogger getDefaultLogger() {
        return (config, node, level, format, args) -> {
            // Default implementation - could be empty or log to console
            // This is just a placeholder for the equivalent of getDefaultLogger in C++
        };
    }
}
