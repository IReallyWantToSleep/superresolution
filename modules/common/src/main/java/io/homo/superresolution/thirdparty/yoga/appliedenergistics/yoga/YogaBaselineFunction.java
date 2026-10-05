/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga;

/**
 * Interface for node baseline function.
 */
@FunctionalInterface
public interface YogaBaselineFunction {
    /**
     * Returns the baseline of a node given its width and height.
     *
     * @param node   The node to calculate baseline for
     * @param width  The width of the node
     * @param height The height of the node
     *
     * @return The baseline position
     */
    float baseline(YogaNode node, float width, float height);
}
