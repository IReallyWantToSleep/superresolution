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
import org.joml.Vector2f;

import java.util.*;

public class ListLayout extends AbstractLayout {

    public enum HorizontalAlignment {
        LEFT, CENTER, RIGHT
    }

    public enum VerticalAlignment {
        TOP, CENTER, BOTTOM
    }

    public static class ListLayoutData extends LayoutData {
        public final HorizontalAlignment horizontalAlignment;
        public final VerticalAlignment verticalAlignment;

        public ListLayoutData(HorizontalAlignment horizontalAlignment, VerticalAlignment verticalAlignment) {
            this.horizontalAlignment = horizontalAlignment;
            this.verticalAlignment = verticalAlignment;
        }

        public ListLayoutData() {
            this(HorizontalAlignment.LEFT, VerticalAlignment.TOP);
        }
    }

    private Rectangle layoutBounds = new Rectangle(0, 0, Float.MAX_VALUE, Float.MAX_VALUE);
    private float rowHeight = 0;
    private float verticalGap = 0;
    private HorizontalAlignment defaultHorizontalAlignment = HorizontalAlignment.LEFT;
    private VerticalAlignment defaultVerticalAlignment = VerticalAlignment.TOP;
    private final Map<ILayoutElement, Vector2f> computedPositions = new HashMap<>();

    public void setLayoutBounds(Rectangle bounds) {
        this.layoutBounds = bounds != null ? bounds.clone() : new Rectangle(0, 0, Float.MAX_VALUE, Float.MAX_VALUE);
    }

    public void setRowHeight(float rowHeight) {
        this.rowHeight = Math.max(0, rowHeight);
    }

    public void setVerticalGap(float gap) {
        this.verticalGap = gap;
    }

    public void setDefaultAlignment(HorizontalAlignment horizontal, VerticalAlignment vertical) {
        this.defaultHorizontalAlignment = horizontal;
        this.defaultVerticalAlignment = vertical;
    }

    @Override
    protected void performLayout(ILayoutContainer container) {
        computedPositions.clear();

        if (container.getChildren().isEmpty()) {
            return;
        }

        List<ILayoutElement> children = container.getChildren();
        float currentY = layoutBounds.y;

        for (ILayoutElement element : children) {
            ListLayoutData data = (ListLayoutData) getElementData(element);
            if (data == null) {
                data = new ListLayoutData(defaultHorizontalAlignment, defaultVerticalAlignment);
            }

            Rectangle elementBounds = element.getBounds();

            float xPos = calculateHorizontalPosition(data.horizontalAlignment, elementBounds.width);

            float yPos = currentY;
            if (rowHeight > 0) {
                yPos += calculateVerticalOffset(data.verticalAlignment, elementBounds.height, rowHeight);
            } else {
                yPos += calculateVerticalOffset(data.verticalAlignment, elementBounds.height, elementBounds.height);
            }

            computedPositions.put(element, new Vector2f(xPos, yPos));

            if (rowHeight > 0) {
                currentY += rowHeight + verticalGap;
            } else {
                currentY += elementBounds.height + verticalGap;
            }
        }
    }

    private float calculateHorizontalPosition(HorizontalAlignment alignment, float elementWidth) {
        switch (alignment) {
            case LEFT:
                return layoutBounds.x;
            case CENTER:
                return layoutBounds.x + (layoutBounds.width - elementWidth) / 2;
            case RIGHT:
                return layoutBounds.x + layoutBounds.width - elementWidth;
            default:
                return layoutBounds.x;
        }
    }

    private float calculateVerticalOffset(VerticalAlignment alignment, float elementHeight, float availableHeight) {
        switch (alignment) {
            case TOP:
                return 0;
            case CENTER:
                return (availableHeight - elementHeight) / 2;
            case BOTTOM:
                return availableHeight - elementHeight;
            default:
                return 0;
        }
    }

    @Override
    public Vector2f getElementPosition(ILayoutElement element) {
        Vector2f computedPos = computedPositions.get(element);
        if (computedPos != null) {
            return new Vector2f(computedPos);
        }
        return new Vector2f(0, 0);
    }

    public void setElementPosition(ILayoutElement element, HorizontalAlignment horizontalAlign, VerticalAlignment verticalAlign) {
        setElementData(element, new ListLayoutData(horizontalAlign, verticalAlign));
    }

    @Override
    protected void calculateContainerBounds(ILayoutContainer container) {
        if (container.getChildren().isEmpty()) {
            containerBounds.setBounds(layoutBounds.x, layoutBounds.y, 0, 0);
            return;
        }

        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = Float.MIN_VALUE;
        float maxY = Float.MIN_VALUE;

        for (ILayoutElement element : container.getChildren()) {
            Vector2f pos = getElementPosition(element);
            Rectangle bounds = element.getBounds();

            minX = Math.min(minX, pos.x);
            minY = Math.min(minY, pos.y);
            maxX = Math.max(maxX, pos.x + bounds.width);
            maxY = Math.max(maxY, pos.y + bounds.height);
        }

        containerBounds.setBounds(
                Math.min(minX, layoutBounds.x),
                Math.min(minY, layoutBounds.y),
                Math.max(maxX - minX, layoutBounds.width),
                Math.max(maxY - minY, layoutBounds.height)
        );
    }
}