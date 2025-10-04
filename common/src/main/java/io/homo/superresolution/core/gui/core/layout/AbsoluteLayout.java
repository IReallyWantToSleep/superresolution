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
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectRBTreeMap;

import java.util.Map;

public class AbsoluteLayout implements ILayout {
    private final Object2ObjectMap<ILayoutElement, Vector2f> elements = new Object2ObjectLinkedOpenHashMap<>();
    private final Rectangle rectangle = new Rectangle();
    private ILayoutElement parent;

    public AbsoluteLayout setPosition(ILayoutElement element, Vector2f position) {
        elements.replace(element, position);
        return this;
    }

    public AbsoluteLayout() {
    }

    @Override
    public void addElement(ILayoutElement element) {
        this.elements.put(element, new Vector2f(0));
        update();
    }

    @Override
    public void removeElement(ILayoutElement element) {
        this.elements.remove(element);
        update();

    }

    @Override
    public Vector2f getElementPosition(ILayoutElement element) {
        Vector2f position = elements.get(element);
        if (position == null) throw new IllegalArgumentException();
        if (parent != null) {
            Vector2f parentPos = parent.getRectangle().getPosition();
            return new Vector2f(parentPos.x + position.x, parentPos.y + position.y);
        }
        return position.copy();
    }

    @Override
    public void update() {
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = Float.MIN_VALUE;
        float maxY = Float.MIN_VALUE;
        for (Map.Entry<ILayoutElement, Vector2f> entry : elements.entrySet()) {
            ILayoutElement element = entry.getKey();
            Vector2f elementPosition = entry.getValue();
            Rectangle elementRectangle = element.getRectangle();
            minX = Math.min(minX, elementPosition.x);
            minY = Math.min(minY, elementPosition.y);
            maxX = Math.max(maxX, elementPosition.x + elementRectangle.width);
            maxY = Math.max(maxY, elementPosition.y + elementRectangle.height);
        }
        rectangle.x = minX;
        rectangle.y = minY;
        rectangle.width = maxX - minX;
        rectangle.height = maxY - minY;
    }

    @Override
    public Rectangle getRectangle() {
        return rectangle;
    }

    @Override
    public ILayoutElement getParent() {
        return parent;
    }

    @Override
    public void setParent(ILayoutElement parent) {
        this.parent = parent;
    }
}
