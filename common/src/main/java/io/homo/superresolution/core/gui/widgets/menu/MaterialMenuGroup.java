/*
 * Super Resolution
 * Copyright (c) 2025-2026. 187J3X1-114514
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

import io.homo.superresolution.core.gui.MaterialElevation;
import io.homo.superresolution.core.gui.core.AbstractWidget;
import io.homo.superresolution.core.gui.core.UIInputState;
import io.homo.superresolution.core.gui.core.backends.render.RenderContext;
import io.homo.superresolution.core.gui.core.impl.Rectangle;
import io.homo.superresolution.core.gui.core.layout.ILayoutElement;
import io.homo.superresolution.core.gui.widgets.MaterialContainerWidget;
import io.homo.superresolution.core.utils.Color;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaEdge;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaFlexDirection;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaGutter;

public class MaterialMenuGroup extends MaterialContainerWidget<MaterialMenuGroup> {

    private float expandProgress = 1f;

    public MaterialMenuGroup() {
        this.style = new MaterialMenuStyle();
        layout().setFlexDirection(YogaFlexDirection.COLUMN);
        updateSize();
    }

    public static MaterialMenuGroup create() {
        return new MaterialMenuGroup();
    }

    public void updateSize() {
        layout().setWidthPercent(100);
        layout().setPadding(YogaEdge.ALL, 4);
        layout().setGap(YogaGutter.ROW, 4);
    }

    @Override
    public void layouting(RenderContext ctx) {
        updateSize();
    }

    @Override
    public MaterialMenuStyle style() {
        return (MaterialMenuStyle) super.style();
    }

    @Override
    public void clearHover() {
        super.clearHover();
        for (ILayoutElement child : getChildren()) {
            if (child instanceof AbstractWidget<?> widget) {
                widget.clearHover();
            }
        }
    }

    @Override
    public boolean managesChildRendering() {
        return true;
    }

    @Override
    public boolean managesChildEvents() {
        return true;
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

    @Override
    protected void init() {

    }

    @Override
    public void mouseMove(float x, float y) {
        super.mouseMove(x, y);
        if (isDisabled() || !isVisible()) {
            return;
        }
        for (ILayoutElement child : getChildren()) {
            if (child instanceof AbstractWidget<?> widget) {
                if (widget.isVisible() && !widget.isDisabled()) {
                    //if (widget.hitTest(new org.joml.Vector2f(x, y))) {
                    widget.mouseMove(x, y);
                    // }
                }
            }
        }
    }

    @Override
    public void mousePress(float x, float y, int button) {
        super.mousePress(x, y, button);
        if (isDisabled() || !isVisible()) {
            return;
        }
        for (ILayoutElement child : getChildren()) {
            if (child instanceof AbstractWidget<?> widget) {
                if (widget.isVisible() && !widget.isDisabled()) {
                    if (widget.hitTest(new org.joml.Vector2f(x, y))) {
                        widget.mousePress(x, y, button);
                    }
                }
            }
        }
    }

    @Override
    public void mouseRelease(float x, float y, int button) {
        super.mouseRelease(x, y, button);
        if (isDisabled() || !isVisible()) {
            return;
        }
        for (ILayoutElement child : getChildren()) {
            if (child instanceof AbstractWidget<?> widget) {
                if (widget.isVisible() && !widget.isDisabled()) {
                    if (widget.hitTest(new org.joml.Vector2f(x, y))) {
                        widget.mouseRelease(x, y, button);
                    }
                }
            }
        }
    }

    @Override
    public void mouseScroll(float x, float y, double scrollX) {
        super.mouseScroll(x, y, scrollX);
        if (isDisabled() || !isVisible()) {
            return;
        }
        for (ILayoutElement child : getChildren()) {
            if (child instanceof AbstractWidget<?> widget) {
                if (widget.isVisible() && !widget.isDisabled()) {
                    if (widget.hitTest(new org.joml.Vector2f(x, y))) {
                        widget.mouseScroll(x, y, scrollX);
                    }
                }
            }
        }
    }

    @Override
    protected Rectangle getViewRegion() {
        return getAbsoluteViewRect();
    }

    @Override
    protected void renderSelf(RenderContext ctx, UIInputState inputState) {
        MaterialMenuSize size = style().size();
        Color backgroundColor = style().colors().menuBackground(scheme());
        Rectangle bounds = getRawBounds();
        if (!isVisible()) {
            return;
        }
        if (expandProgress <= 0) {
            return;
        }

        for (ILayoutElement child : getChildren()) {
            if (child instanceof MaterialMenuItem item) {
                item.style().colors = style().colors();
            }
        }
        float radius = size.cornerRadius();
        float topLeft = 0, topRight = 0, bottomLeft = 0, bottomRight = 0;

        if (getParent() instanceof MaterialMenu menu) {
            int index = menu.getChildren().indexOf(this);
            int count = menu.getChildren().size();

            if (index == 0) {
                topLeft = radius;
                topRight = radius;
                bottomLeft = 8;
                bottomRight = 8;
            }
            if (index == count - 1) {
                topLeft = 8;
                topRight = 8;
                bottomLeft = radius;
                bottomRight = radius;
            }
            if (count == 1) {
                topLeft = radius;
                topRight = radius;
                bottomLeft = radius;
                bottomRight = radius;
            }
        } else {
            throw new IllegalStateException();
        }

        float animatedHeight = bounds.height * expandProgress;

        ctx.save();
        MaterialElevation.draw(
                ctx,
                2,
                bounds.x,
                bounds.y,
                bounds.width,
                bounds.height,
                Math.min(bottomLeft, animatedHeight / 2),
                Math.min(bottomRight, animatedHeight / 2),
                Math.min(topLeft, animatedHeight / 2),
                Math.min(topRight, animatedHeight / 2)
        );
        ctx.beginPath();
        ctx.roundedRectComplex(bounds.x, bounds.y, bounds.width, animatedHeight,
                Math.min(bottomLeft, animatedHeight / 2), Math.min(bottomRight, animatedHeight / 2), Math.min(topLeft, animatedHeight / 2), Math.min(topRight, animatedHeight / 2));
        ctx.fillColor(backgroundColor);
        ctx.endPath(true);
        ctx.restore();

        for (ILayoutElement child : getChildren()) {
            if (child instanceof AbstractWidget<?> widget) {
                if (widget.isVisible()) {
                    widget.render(ctx, inputState);
                }
            }
        }
    }

    @Override
    public void render(RenderContext ctx, UIInputState inputState) {
        renderSelf(ctx, inputState);
    }

    public MaterialMenuGroup addItem(MaterialMenuItem item) {
        addChild(item);
        return this;
    }

    public float getExpandProgress() {
        return expandProgress;
    }

    void setExpandProgress(float progress) {
        this.expandProgress = progress;
    }
}
