/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga;

/**
 * LogLevel enum corresponding to YGLogLevel from Yoga.
 */
public enum LogLevel {
    ERROR,
    WARN,
    INFO,
    DEBUG,
    VERBOSE,
    FATAL;

    /**
     * Returns the string representation of the enum value.
     *
     * @return String representation of the enum value.
     */
    @Override
    public String toString() {
        return switch (this) {
            case ERROR -> "error";
            case WARN -> "warn";
            case INFO -> "info";
            case DEBUG -> "debug";
            case VERBOSE -> "verbose";
            case FATAL -> "fatal";
        };
    }
}
