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

import io.homo.superresolution.core.gui.core.IHitTest;
import io.homo.superresolution.core.gui.core.impl.Rectangle;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaNode;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaProps;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.config.MutableYogaConfig;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.config.YogaConfig;
import org.joml.Vector2f;

public abstract class AbstractLayoutElement implements ILayoutElement, IHitTest {
    public static final MutableYogaConfig yogaConfig = YogaConfig.create();
    private final Vector2f elementSize = new Vector2f(-1, -1);
    protected ILayoutContainer parent;
    private YogaNode layoutNode = new YogaNode(yogaConfig);

    public void setElementSize(float width, float height) {
        elementSize.set(width, height);
        if (width > 0) layoutNode.setWidth(width);
        if (height > 0) layoutNode.setHeight(height);
    }

    public void setElementWidth(float width) {
        setElementSize(width, elementSize.y);
    }

    public void setElementHeight(float height) {
        setElementSize(elementSize.x, height);
    }

    @Override
    public Rectangle getBounds() {
        return new Rectangle(
                getAbsolutePosition(),
                (elementSize.x <= 0 || elementSize.y <= 0) ? new Vector2f(layoutNode.getLayoutWidth(), layoutNode.getLayoutHeight()) : elementSize
        );
    }

    @Override
    public Vector2f getAbsolutePosition() {
        return new Vector2f(
                getLayoutNode().getAbsolutePositionX(),
                getLayoutNode().getAbsolutePositionY()
        );
    }

    @Override
    public ILayoutContainer getParent() {
        return parent;
    }

    @Override
    public void setParent(ILayoutContainer parent) {
        this.parent = parent;
    }

    @Override
    public boolean hitTest(Vector2f absolutePos) {
        Rectangle absBounds = getBounds();
        boolean result = absBounds.in(absolutePos);
        return result;
    }

    public YogaNode getLayoutNode() {
        return layoutNode;
    }

    @Override
    public YogaProps layout() {
        return layoutNode;
    }
}