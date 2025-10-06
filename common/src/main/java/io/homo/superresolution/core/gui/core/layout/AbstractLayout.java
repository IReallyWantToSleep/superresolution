/*
 * Super Resolution
 * Copyright (c) 2025. 187J3X1-114514
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

package io.homo.superresolution.core.gui.core.layout;

import io.homo.superresolution.core.gui.core.impl.Rectangle;
import io.homo.superresolution.core.math.Vector2f;

import java.util.*;

public abstract class AbstractLayout implements ILayout {
    protected final Map<ILayoutElement, LayoutData> elementData = new HashMap<>();
    protected final Rectangle containerBounds = new Rectangle();

    @Override
    public Rectangle getContainerBounds() {
        return containerBounds;
    }

    public void setElementData(ILayoutElement element, LayoutData data) {
        elementData.put(element, data);
    }

    public LayoutData getElementData(ILayoutElement element) {
        return elementData.get(element);
    }

    protected abstract void performLayout(ILayoutContainer container);

    @Override
    public void layout(ILayoutContainer container) {
        performLayout(container);
        calculateContainerBounds(container);

        container.getBounds().setBounds(
                containerBounds.x,
                containerBounds.y,
                containerBounds.width,
                containerBounds.height
        );
    }

    protected void calculateContainerBounds(ILayoutContainer container) {
        if (container.getChildren().isEmpty()) {
            containerBounds.setBounds(0, 0, 0, 0);
            return;
        }

        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = Float.MIN_VALUE;
        float maxY = Float.MIN_VALUE;

        for (ILayoutElement child : container.getChildren()) {
            Vector2f pos = getElementPosition(child);
            Rectangle childBounds = child.getBounds();

            minX = Math.min(minX, pos.x);
            minY = Math.min(minY, pos.y);
            maxX = Math.max(maxX, pos.x + childBounds.width);
            maxY = Math.max(maxY, pos.y + childBounds.height);
        }

        containerBounds.setBounds(minX, minY, maxX - minX, maxY - minY);
    }

    public static class LayoutData {
    }
}