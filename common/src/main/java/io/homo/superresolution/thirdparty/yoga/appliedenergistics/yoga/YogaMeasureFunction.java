/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga;

import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaMeasureMode;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaNode;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaSize;

/**
 * Interface for node measurement function.
 */
@FunctionalInterface
public interface YogaMeasureFunction {
    /**
     * Measures a node given constraints.
     *
     * @param node       The node to measure
     * @param width      The available width
     * @param widthMode  The mode in which the width is being constrained
     * @param height     The available height
     * @param heightMode The mode in which the height is being constrained
     * @return The measured dimensions for the node
     */
    YogaSize measure(
            YogaNode node,
            float width,
            io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaMeasureMode widthMode,
            float height,
            YogaMeasureMode heightMode);
}
