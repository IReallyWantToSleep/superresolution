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

package io.homo.superresolution.core.gui.widgets;

import io.homo.superresolution.core.gui.MaterialScheme;
import io.homo.superresolution.core.gui.core.AbstractWidget;
import io.homo.superresolution.core.gui.core.WidgetStyle;
import io.homo.superresolution.core.gui.core.animator.AnimationSet;

public abstract class MaterialWidget<
        T extends AbstractWidget<?, ?, ?>,
        STYLE extends WidgetStyle<?>,
        ANIM extends AnimationSet
        > extends AbstractWidget<T, STYLE, ANIM> {
    protected MaterialScheme scheme = MaterialScheme.defaultLight;

    public MaterialScheme scheme() {
        return scheme;
    }

    @SuppressWarnings("unchecked")
    public T scheme(MaterialScheme scheme) {
        this.scheme = scheme;
        return (T) this;
    }
}
