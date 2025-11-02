/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.node;

import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaDisplay;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaNode;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Java implementation of LayoutableChildren, providing iteration over YogaNode children
 * with special handling for Display.Contents nodes.
 */
public class LayoutableChildren implements Iterable<YogaNode> {
    private final YogaNode node;

    /**
     * Constructs a new LayoutableChildren for the given node.
     *
     * @param node The parent node whose children will be iterated
     */
    public LayoutableChildren(YogaNode node) {
        this.node = Objects.requireNonNull(node);
    }

    @Override
    public LayoutIterator iterator() {
        if (node.getChildCount() > 0) {
            LayoutIterator iterator = new LayoutIterator(node, 0);
            if (node.getChild(0).getStyle().getDisplay() == YogaDisplay.CONTENTS) {
                iterator.skipContentsNodes();
            }
            return iterator;
        } else {
            return new LayoutIterator(null, 0);
        }
    }

    /**
     * Iterator implementation for LayoutableChildren that handles Display.CONTENTS nodes.
     * Includes C++-style current() method for accessing the current element without advancing.
     */
    public static class LayoutIterator implements Iterator<YogaNode> {
        private YogaNode currentNode;
        private int childIndex;
        private final LinkedList<NodeIndexPair> backtrack = new LinkedList<>();

        /**
         * Creates a new iterator starting at the specified node and child index.
         *
         * @param node       The node to iterate children from
         * @param childIndex The starting child index
         */
        public LayoutIterator(YogaNode node, int childIndex) {
            this.currentNode = node;
            this.childIndex = childIndex;
        }

        @Override
        public boolean hasNext() {
            return currentNode != null;
        }

        @Override
        public YogaNode next() {
            if (!hasNext()) {
                throw new NoSuchElementException("No more elements in iterator");
            }

            // Get the current child
            YogaNode child = current();

            // Move to the next position for subsequent calls
            advance();

            return child;
        }

        /**
         * Returns the current element without advancing the iterator.
         * This provides C++-style access to the current element.
         *
         * @return The current element
         * @throws NoSuchElementException if there is no current element
         */
        public YogaNode current() {
            if (!hasNext()) {
                throw new NoSuchElementException("No element available");
            }
            return currentNode.getChild(childIndex);
        }

        /**
         * Advances the iterator to the next valid position.
         */
        private void advance() {
            if (childIndex + 1 >= currentNode.getChildCount()) {
                // If the current node has no more children, try to backtrack
                if (backtrack.isEmpty()) {
                    // If there are no nodes to backtrack to, iteration is complete
                    currentNode = null;
                    childIndex = 0;
                } else {
                    // Pop and restore the latest backtrack entry
                    NodeIndexPair back = backtrack.removeFirst();
                    currentNode = back.node;
                    childIndex = back.index;

                    // Go to the next position
                    advance();
                }
            } else {
                // Current node has more children to visit, go to next
                childIndex++;

                // Skip all display: contents nodes, possibly going deeper into the tree
                if (currentNode.getChild(childIndex).getStyle().getDisplay() == YogaDisplay.CONTENTS) {
                    skipContentsNodes();
                }
            }
        }

        /**
         * Skips over YogaDisplay.CONTENTS nodes by diving deeper into the tree when needed.
         */
        void skipContentsNodes() {
            // Get the node that would be returned from the iterator
            YogaNode contentsNode = currentNode.getChild(childIndex);

            while (contentsNode.getStyle().getDisplay() == YogaDisplay.CONTENTS &&
                    contentsNode.getChildCount() > 0) {
                // Push the current state so it can be restored when backtracking
                backtrack.addFirst(new NodeIndexPair(currentNode, childIndex));

                // Traverse the child
                currentNode = contentsNode;
                childIndex = 0;

                // Repeat until a node without display: contents is found in the
                // subtree or a leaf is reached
                contentsNode = currentNode.getChild(childIndex);
            }

            // If no node without display: contents was found, try to backtrack
            if (contentsNode.getStyle().getDisplay() == YogaDisplay.CONTENTS) {
                advance();
            }
        }

        /**
         * Checks if this iterator equals another iterator.
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof LayoutIterator other)) return false;

            return currentNode == other.currentNode && childIndex == other.childIndex;
        }

        @Override
        public int hashCode() {
            return Objects.hash(currentNode, childIndex);
        }

        public LayoutIterator copy() {
            var result = new LayoutIterator(this.currentNode, this.childIndex);
            result.backtrack.addAll(backtrack);
            return result;
        }
    }

    /**
     * Helper class to store node and index pairs for backtracking.
     */
    private record NodeIndexPair(YogaNode node, int index) {
    }
}
