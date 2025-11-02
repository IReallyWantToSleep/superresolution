/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga;

import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaNode;

/**
 * Interface for notification when a node becomes dirty.
 */
@FunctionalInterface
public interface YogaDirtiedFunction {
    /**
     * Called when a node is marked as dirty.
     *
     * @param node The node that was marked dirty
     */
    void onDirtied(YogaNode node);
}
