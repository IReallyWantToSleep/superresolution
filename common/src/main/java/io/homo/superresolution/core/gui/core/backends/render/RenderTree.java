/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
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

package io.homo.superresolution.core.gui.core.backends.render;

import java.util.*;

public class RenderTree {
    private final Map<RenderLayer, List<RenderNode>> nodesByLayer = new EnumMap<>(RenderLayer.class);

    public RenderTree() {
        for (RenderLayer layer : RenderLayer.values()) {
            nodesByLayer.put(layer, new ArrayList<>());
        }
    }

    public void addNode(RenderNode node) {
        nodesByLayer.get(node.layer()).add(node);
    }

    public boolean hasNodesOnLayer(RenderLayer layer) {
        return !nodesByLayer.get(layer).isEmpty();
    }

    public List<RenderNode> getNodesOnLayer(RenderLayer layer) {
        return nodesByLayer.get(layer);
    }

    public void render(RenderContext ctx, Runnable beforeLayer, Runnable afterLayer) {
        for (RenderLayer layer : RenderLayer.values()) {
            List<RenderNode> nodes = nodesByLayer.get(layer);
            if (nodes.isEmpty()) continue;

            if (beforeLayer != null) {
                beforeLayer.run();
            }

            nodes.sort(Comparator.comparing(RenderNode::zIndex));

            for (RenderNode node : nodes) {
                node.render(ctx);
            }

            if (afterLayer != null) {
                afterLayer.run();
            }
        }
    }

    public void render(RenderContext ctx) {
        render(ctx, null, null);
    }

    public void clear() {
        for (List<RenderNode> nodes : nodesByLayer.values()) {
            nodes.clear();
        }
    }

    public void clearLayer(RenderLayer layer) {
        nodesByLayer.get(layer).clear();
    }

    public int nodeCount() {
        int count = 0;
        for (List<RenderNode> nodes : nodesByLayer.values()) {
            count += nodes.size();
        }
        return count;
    }

    /**
     * 获取指定层的节点数量
     */
    public int nodeCountOnLayer(RenderLayer layer) {
        return nodesByLayer.get(layer).size();
    }
}
