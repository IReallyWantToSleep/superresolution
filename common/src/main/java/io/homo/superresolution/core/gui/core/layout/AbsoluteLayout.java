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

import java.util.HashMap;
import java.util.Map;

public class AbsoluteLayout extends AbstractLayout {

    public static class AbsoluteLayoutData extends LayoutData {
        public final Vector2f position;

        public AbsoluteLayoutData(Vector2f position) {
            this.position = position;
        }
    }

    @Override
    protected void performLayout(ILayoutContainer container) {
    }

    @Override
    public Vector2f getElementPosition(ILayoutElement element) {
        AbsoluteLayoutData data = (AbsoluteLayoutData) getElementData(element);
        return data != null ? data.position.copy() : new Vector2f(0, 0);
    }

    public void setPosition(ILayoutElement element, Vector2f position) {
        setElementData(element, new AbsoluteLayoutData(position));
    }
}
