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

public abstract class AbstractLayoutElement implements ILayoutElement {
    protected ILayoutContainer parent;
    protected final Rectangle bounds = new Rectangle();

    @Override
    public Rectangle getBounds() {
        return bounds;
    }

    @Override
    public Vector2f getAbsolutePosition() {
        if (parent != null) {
            Vector2f parentPos = parent.getAbsolutePosition();
            Vector2f relativePos = parent.getLayout().getElementPosition(this);
            return new Vector2f(parentPos.x + relativePos.x, parentPos.y + relativePos.y);
        }
        return new Vector2f(bounds.x, bounds.y);
    }

    @Override
    public void setParent(ILayoutContainer parent) {
        this.parent = parent;
    }

    @Override
    public ILayoutContainer getParent() {
        return parent;
    }
}