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

package io.homo.superresolution.core.gui.widgets.menu;

import io.homo.superresolution.core.gui.core.UIInputState;
import io.homo.superresolution.core.gui.core.backends.interfaces.IUIDrawContext;
import io.homo.superresolution.core.gui.core.impl.Rectangle;
import io.homo.superresolution.core.gui.core.layout.ILayoutElement;
import io.homo.superresolution.core.gui.widgets.MaterialContainerWidget;
import io.homo.superresolution.core.utils.Color;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaEdge;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaFlexDirection;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaGutter;

public class MaterialMenuGroup extends MaterialContainerWidget<MaterialMenuGroup> {

    public MaterialMenuGroup() {
        this.style = new MaterialMenuStyle();
        layout().setFlexDirection(YogaFlexDirection.COLUMN);
        updateSize();
    }

    public void updateSize() {
        layout().setWidthPercent(100);
        layout().setPadding(YogaEdge.ALL, 4);
        layout().setGap(YogaGutter.ROW, 4);
    }

    public float computeContentWidth() {
        float max = 0;
        for (ILayoutElement child : getChildren()) {
            if (child instanceof MaterialMenuItem item) {
                max = Math.max(max, item.computeContentWidth());
            }
        }
        return max;
    }

    public static MaterialMenuGroup create() {
        return new MaterialMenuGroup();
    }

    @Override
    protected Rectangle getViewRegion() {
        return getAbsoluteViewRect();
    }

    @Override
    public MaterialMenuStyle style() {
        return (MaterialMenuStyle) super.style();
    }

    @Override
    protected void init() {

    }

    public MaterialMenuGroup addItem(MaterialMenuItem item) {
        addChild(item);
        return this;
    }

    @Override
    protected void renderSelf(IUIDrawContext drawContext, UIInputState inputState) {
        MaterialMenuSize size = style().size();
        Color backgroundColor = style().colors().menuBackground(scheme());
        Rectangle bounds = getBounds();
        updateSize();
        for (ILayoutElement child : getChildren()) {
            if (child instanceof MaterialMenuItem item) {
                item.style().colors = style().colors();
                item.scheme(scheme());
            }
        }
        float radius = size.cornerRadius();
        float topLeft = 0, topRight = 0, bottomLeft = 0, bottomRight = 0;

        if (getParent() instanceof MaterialMenu menu) {
            int index = menu.getChildren().indexOf(this);
            int count = menu.getChildren().size();

            //第一个组
            if (index == 0) {
                topLeft = radius;
                topRight = radius;
                bottomLeft = 8;
                bottomRight = 8;
            }
            //最后一个组
            if (index == count - 1) {
                topLeft = 8;
                topRight = 8;
                bottomLeft = radius;
                bottomRight = radius;
            }
            //唯一组
            if (count == 1) {
                topLeft = radius;
                topRight = radius;
                bottomLeft = radius;
                bottomRight = radius;
            }
        } else {
            throw new IllegalStateException();
        }

        drawContext.beginPath();
        drawContext.roundedRectComplex(bounds.x, bounds.y, bounds.width, bounds.height, bottomLeft, bottomRight, topLeft, topRight);
        drawContext.fillColor(backgroundColor);
        drawContext.endPath(true);
    }
}
