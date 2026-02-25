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

package io.homo.superresolution.core.gui.widgets;

import io.homo.superresolution.core.gui.core.UIInputState;
import io.homo.superresolution.core.gui.core.WidgetStyle;
import io.homo.superresolution.core.gui.core.backends.render.RenderContext;

public class SpacerWidget extends MaterialWidget<SpacerWidget> {

    private SpacerWidget() {
        this.style = new WidgetStyle<>();
        getLayoutNode().setDebugName("SpacerWidget");
    }

    public static SpacerWidget create() {
        return new SpacerWidget();
    }

    public static SpacerWidget horizontal(float width) {
        return create().width(width);
    }

    public static SpacerWidget vertical(float height) {
        return create().height(height);
    }

    public static SpacerWidget square(float size) {
        return create().width(size).height(size);
    }

    public SpacerWidget width(float width) {
        if (width > 0) {
            setElementWidth(width);
        }
        return this;
    }

    public SpacerWidget height(float height) {
        if (height > 0) {
            setElementHeight(height);
        }
        return this;
    }

    public SpacerWidget size(float width, float height) {
        if (width > 0 && height > 0) {
            setElementSize(width, height);
        }
        return this;
    }

    @Override
    protected void init() {
    }

    @Override
    protected boolean isInteractive() {
        return false;
    }

    @Override
    public void render(RenderContext ctx, UIInputState inputState) {
    }
}
