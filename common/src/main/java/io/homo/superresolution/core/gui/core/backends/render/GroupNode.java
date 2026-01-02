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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class GroupNode extends RenderNode {
    private final List<RenderNode> children = new ArrayList<>();

    public GroupNode() {
        super();
    }

    public GroupNode(float zIndex, RenderLayer layer) {
        super(zIndex, layer);
    }

    public GroupNode addChild(RenderNode child) {
        children.add(child);
        return this;
    }

    public GroupNode removeChild(RenderNode child) {
        children.remove(child);
        return this;
    }

    public GroupNode clearChildren() {
        children.clear();
        return this;
    }

    public List<RenderNode> children() {
        return children;
    }

    public boolean hasChildren() {
        return !children.isEmpty();
    }

    @Override
    public void render(RenderContext ctx) {
        if (children.isEmpty()) return;

        ctx.save();
        applyState(ctx);

        children.sort(Comparator.comparing(RenderNode::zIndex));

        for (RenderNode child : children) {
            child.render(ctx);
        }

        restoreState(ctx);
        ctx.restore();
    }
}
