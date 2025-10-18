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

import io.homo.superresolution.core.gui.core.backends.interfaces.CommandsBatch;
import io.homo.superresolution.core.gui.core.impl.Rectangle;
import org.joml.Vector2f;

import java.util.*;

public class LinearLayout extends AbstractLayout {

    public enum HorizontalAlignment {
        LEFT, CENTER, RIGHT
    }

    public enum VerticalAlignment {
        TOP, CENTER, BOTTOM
    }

    public static class LinearLayoutData extends LayoutData {
        public final HorizontalAlignment horizontalAlignment;
        public final VerticalAlignment verticalAlignment;
        public final int index;

        public LinearLayoutData(int index, HorizontalAlignment horizontalAlignment, VerticalAlignment verticalAlignment) {
            this.horizontalAlignment = horizontalAlignment;
            this.verticalAlignment = verticalAlignment;
            this.index = index;
        }

        public LinearLayoutData(HorizontalAlignment horizontalAlignment, VerticalAlignment verticalAlignment) {
            this(0, horizontalAlignment, verticalAlignment);
        }

        public LinearLayoutData() {
            this(HorizontalAlignment.LEFT, VerticalAlignment.TOP);
        }
    }

    private Rectangle layoutBounds = new Rectangle(0, 0, Float.MAX_VALUE, Float.MAX_VALUE);
    private float horizontalGap = 0;
    private final Map<ILayoutElement, Vector2f> computedPositions = new HashMap<>();

    public void setLayoutBounds(Rectangle bounds) {
        this.layoutBounds = bounds != null ? bounds.clone() : new Rectangle(0, 0, Float.MAX_VALUE, Float.MAX_VALUE);
    }

    public void setHorizontalGap(float gap) {
        this.horizontalGap = gap;
    }

    @Override
    protected void performLayout(ILayoutContainer container) {
        computedPositions.clear();

        if (container.getChildren().isEmpty()) {
            return;
        }
        List<Map.Entry<ILayoutElement, LinearLayoutData>> leftElements = new ArrayList<>();
        List<Map.Entry<ILayoutElement, LinearLayoutData>> centerElements = new ArrayList<>();
        List<Map.Entry<ILayoutElement, LinearLayoutData>> rightElements = new ArrayList<>();

        elementData.entrySet().forEach((entry) -> {
            switch (((LinearLayoutData) entry.getValue()).horizontalAlignment) {
                case LEFT -> {
                    leftElements.add((Map.Entry<ILayoutElement, LinearLayoutData>) ((Object) entry));
                    computedPositions.put(entry.getKey(), new Vector2f(0));
                }
                case CENTER -> {
                    centerElements.add((Map.Entry<ILayoutElement, LinearLayoutData>) ((Object) entry));
                    computedPositions.put(entry.getKey(), new Vector2f(0));
                }
                case RIGHT -> {
                    rightElements.add((Map.Entry<ILayoutElement, LinearLayoutData>) ((Object) entry));
                    computedPositions.put(entry.getKey(), new Vector2f(0));
                }
            }
        });
        leftElements.sort(Comparator.comparingInt((e) -> e.getValue().index));
        centerElements.sort(Comparator.comparingInt((e) -> e.getValue().index));
        rightElements.sort(Comparator.comparingInt((e) -> e.getValue().index));
        {
            float currentX = 0;
            for (Map.Entry<ILayoutElement, LinearLayoutData> entry : leftElements) {
                computedPositions.get(entry.getKey()).setComponent(0, currentX);
                currentX += entry.getKey().getBounds().width + horizontalGap;
                float yPos = 0;
                switch (entry.getValue().verticalAlignment) {
                    case TOP -> yPos = layoutBounds.y;
                    case CENTER ->
                            yPos = layoutBounds.y + (layoutBounds.height - entry.getKey().getBounds().height) / 2;
                    case BOTTOM -> yPos = layoutBounds.y + layoutBounds.height - entry.getKey().getBounds().height;
                }
                computedPositions.get(entry.getKey()).setComponent(1, yPos);
            }
        }
        {
            float currentX = layoutBounds.x + layoutBounds.width;
            for (Map.Entry<ILayoutElement, LinearLayoutData> entry : rightElements) {
                currentX -= entry.getKey().getBounds().width;
                computedPositions.get(entry.getKey()).setComponent(0, currentX);
                currentX -= horizontalGap;
                float yPos = 0;
                switch (entry.getValue().verticalAlignment) {
                    case TOP -> yPos = layoutBounds.y;
                    case CENTER ->
                            yPos = layoutBounds.y + (layoutBounds.height - entry.getKey().getBounds().height) / 2;
                    case BOTTOM -> yPos = layoutBounds.y + layoutBounds.height - entry.getKey().getBounds().height;
                }
                computedPositions.get(entry.getKey()).setComponent(1, yPos);
            }
        }
        {
            float centerElementWidth = -horizontalGap;
            for (Map.Entry<ILayoutElement, LinearLayoutData> entry : centerElements) {
                centerElementWidth += entry.getKey().getBounds().width + horizontalGap;
            }
            float centerElementX = layoutBounds.getCenterX() - centerElementWidth / 2;
            for (Map.Entry<ILayoutElement, LinearLayoutData> entry : centerElements) {
                computedPositions.get(entry.getKey()).setComponent(0, centerElementX);
                centerElementX += entry.getKey().getBounds().width + horizontalGap;
                float yPos = 0;
                switch (entry.getValue().verticalAlignment) {
                    case TOP -> yPos = layoutBounds.y;
                    case CENTER ->
                            yPos = layoutBounds.y + (layoutBounds.height - entry.getKey().getBounds().height) / 2;
                    case BOTTOM -> yPos = layoutBounds.y + layoutBounds.height - entry.getKey().getBounds().height;
                }
                computedPositions.get(entry.getKey()).setComponent(1, yPos);
            }
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
        setElementData(element, new LinearLayoutData(horizontalAlign, verticalAlign));
    }

    public void setElementPosition(ILayoutElement element, int index, HorizontalAlignment horizontalAlign, VerticalAlignment verticalAlign) {
        setElementData(element, new LinearLayoutData(index, horizontalAlign, verticalAlign));
    }


    @Override
    protected void calculateContainerBounds(ILayoutContainer container) {
        if (container.getChildren().isEmpty()) {
            containerBounds.setBounds(layoutBounds.x, layoutBounds.y, 0, 0);
        }
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
        float maxX = Float.MIN_VALUE, maxY = Float.MIN_VALUE;

        for (ILayoutElement element : container.getChildren()) {
            Vector2f pos = getElementPosition(element);
            Rectangle bounds = element.getBounds();
            minX = Math.min(minX, pos.x);
            minY = Math.min(minY, pos.y);
            maxX = Math.max(maxX, pos.x + bounds.width);
            maxY = Math.max(maxY, pos.y + bounds.height);
        }

        containerBounds.setBounds(minX, minY, maxX - minX, maxY - minY);
    }

    @Override
    protected void updateContainerBounds(ILayoutContainer container) {
    }
}