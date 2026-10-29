/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga;

/**
 * Functional interface for cloning nodes in the yoga layout engine.
 */
@FunctionalInterface
public interface YogaCloneNodeFunction {
    /**
     * Clone a node.
     *
     * @param node       The node to clone
     * @param owner      The owner of the node
     * @param childIndex The index of the node in its parent
     *
     * @return The cloned node, or null to use the default cloning mechanism
     */
    io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaNode cloneNode(io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaNode node, YogaNode owner, int childIndex);
}
