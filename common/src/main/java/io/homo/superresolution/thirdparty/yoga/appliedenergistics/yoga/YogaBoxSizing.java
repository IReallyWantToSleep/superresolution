/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga;

/**
 * BoxSizing enum corresponding to YGBoxSizing from Yoga.
 */
public enum YogaBoxSizing {
    BORDER_BOX,
    CONTENT_BOX;

    /**
     * Returns the string representation of the enum value.
     *
     * @return String representation of the enum value.
     */
    @Override
    public String toString() {
        return switch (this) {
            case BORDER_BOX -> "border-box";
            case CONTENT_BOX -> "content-box";
        };
    }
}
