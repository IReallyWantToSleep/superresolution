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

package io.homo.superresolution.core.gui.core;

import io.homo.superresolution.core.gui.core.animator.AnimationSet;
import io.homo.superresolution.core.gui.core.backends.interfaces.IUIDrawContext;
import io.homo.superresolution.core.gui.core.impl.Rectangle;
import io.homo.superresolution.core.gui.core.layout.ILayout;

public class ContainerWidget extends AbstractContainerWidget<ContainerWidget, WidgetStyle<?>, AnimationSet> {
    private final Rectangle rectangle = new Rectangle();

    public Rectangle getBounds() {
        return rectangle;
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    protected Rectangle getViewRegion() {
        return rectangle;
    }

    @Override
    protected void renderSelf(IUIDrawContext drawContext, UIInputState inputState) {

    }

    public static ContainerWidget create() {
        return new ContainerWidget();
    }

    public ContainerWidget layout(ILayout layout) {
        setLayout(layout);
        return this;
    }
}