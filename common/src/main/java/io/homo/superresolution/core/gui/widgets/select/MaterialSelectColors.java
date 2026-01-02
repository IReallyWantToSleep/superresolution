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

package io.homo.superresolution.core.gui.widgets.select;

import io.homo.superresolution.core.gui.MaterialScheme;
import io.homo.superresolution.core.utils.Color;

public interface MaterialSelectColors {
    Color outline(MaterialScheme scheme);

    Color outlineFocused(MaterialScheme scheme);

    Color label(MaterialScheme scheme);

    Color labelFocused(MaterialScheme scheme);

    Color inputText(MaterialScheme scheme);

    Color placeholder(MaterialScheme scheme);

    Color leadingIcon(MaterialScheme scheme);

    Color trailingIcon(MaterialScheme scheme);

    Color supportingText(MaterialScheme scheme);

    Color background(MaterialScheme scheme);

    MaterialSelectColors STANDARD = new MaterialSelectColors() {
        @Override
        public Color outline(MaterialScheme scheme) {
            return scheme.outline().copy();
        }

        @Override
        public Color outlineFocused(MaterialScheme scheme) {
            return scheme.primary().copy();
        }

        @Override
        public Color label(MaterialScheme scheme) {
            return scheme.onSurfaceVariant().copy();
        }

        @Override
        public Color labelFocused(MaterialScheme scheme) {
            return scheme.primary().copy();
        }

        @Override
        public Color inputText(MaterialScheme scheme) {
            return scheme.onSurface().copy();
        }

        @Override
        public Color placeholder(MaterialScheme scheme) {
            return scheme.onSurfaceVariant().copy();
        }

        @Override
        public Color leadingIcon(MaterialScheme scheme) {
            return scheme.onSurfaceVariant().copy();
        }

        @Override
        public Color trailingIcon(MaterialScheme scheme) {
            return scheme.onSurfaceVariant().copy();
        }

        @Override
        public Color supportingText(MaterialScheme scheme) {
            return scheme.onSurfaceVariant().copy();
        }

        @Override
        public Color background(MaterialScheme scheme) {
            return scheme.surface().copy();
        }
    };
}
