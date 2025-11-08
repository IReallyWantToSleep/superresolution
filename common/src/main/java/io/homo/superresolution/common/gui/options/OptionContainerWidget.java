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

package io.homo.superresolution.common.gui.options;

import io.homo.superresolution.core.gui.MaterialScheme;
import io.homo.superresolution.core.gui.core.ContainerWidget;
import io.homo.superresolution.core.gui.core.UIInputState;
import io.homo.superresolution.core.gui.core.backends.interfaces.IUIDrawContext;
import io.homo.superresolution.core.gui.core.impl.Rectangle;

public class OptionContainerWidget extends ContainerWidget {
    protected MaterialScheme scheme = MaterialScheme.defaultLight;

    public OptionContainerWidget(AbstractOptionEntry<?, ?> entry) {
        this.entry = entry;
    }

    protected AbstractOptionEntry<?, ?> entry;

    public MaterialScheme scheme() {
        return scheme;
    }

    public OptionContainerWidget scheme(MaterialScheme scheme) {
        this.scheme = scheme;
        entry.setScheme(scheme);
        return this;
    }

    @Override
    public void render(IUIDrawContext drawContext, UIInputState inputState) {
        setElementHeight(entry.getEntryHeight());
        super.render(drawContext, inputState);
    }

    @Override
    protected void renderSelf(IUIDrawContext drawContext, UIInputState inputState) {
        /*
        drawContext.beginBatch();
        Rectangle bounds = getBounds();
        drawContext.drawRect(
                bounds.x,
                bounds.y,
                bounds.width,
                bounds.height,
                Color.black(),
                false
        );
        drawContext.endBatch(0);
         */

    }

    @Override
    public Rectangle getBounds() {
        return super.getBounds();
    }

    @Override
    protected Rectangle getViewRegion() {
        return super.getViewRegion();
    }
}
