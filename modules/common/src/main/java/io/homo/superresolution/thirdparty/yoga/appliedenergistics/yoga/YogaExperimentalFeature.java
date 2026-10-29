/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga;

/**
 * ExperimentalFeature enum corresponding to YGExperimentalFeature from Yoga.
 */
public enum YogaExperimentalFeature {
    WEB_FLEX_BASIS;

    /**
     * Returns the string representation of the enum value.
     *
     * @return String representation of the enum value.
     */
    @Override
    public String toString() {
        return switch (this) {
            case WEB_FLEX_BASIS -> "web-flex-basis";
        };
    }
}
