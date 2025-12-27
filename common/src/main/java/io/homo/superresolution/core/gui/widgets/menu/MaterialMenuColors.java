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

import io.homo.superresolution.core.gui.MaterialScheme;
import io.homo.superresolution.core.utils.Color;

public interface MaterialMenuColors {
    Color menuBackground(MaterialScheme scheme);

    Color itemText(MaterialScheme scheme);

    Color itemIcon(MaterialScheme scheme);

    Color selectedItemBackground(MaterialScheme scheme);

    Color selectedItemText(MaterialScheme scheme);

    Color selectedItemIcon(MaterialScheme scheme);

    Color stateLayer(MaterialScheme scheme);

    MaterialMenuColors STANDARD = new MaterialMenuColors() {
        @Override
        public Color menuBackground(MaterialScheme scheme) {
            return scheme.surfaceContainerLow().copy();
        }

        @Override
        public Color itemText(MaterialScheme scheme) {
            return scheme.onSurface().copy();
        }

        @Override
        public Color itemIcon(MaterialScheme scheme) {
            return scheme.onSurfaceVariant().copy();
        }

        @Override
        public Color selectedItemBackground(MaterialScheme scheme) {
            return scheme.tertiaryContainer().copy();
        }

        @Override
        public Color selectedItemText(MaterialScheme scheme) {
            return scheme.onTertiaryContainer().copy();
        }

        @Override
        public Color selectedItemIcon(MaterialScheme scheme) {
            return scheme.onTertiaryContainer().copy();
        }

        @Override
        public Color stateLayer(MaterialScheme scheme) {
            return scheme.onSurface().copy();
        }

    };

    MaterialMenuColors VIBRANT = new MaterialMenuColors() {
        @Override
        public Color menuBackground(MaterialScheme scheme) {
            return scheme.tertiaryContainer().copy();
        }

        @Override
        public Color itemText(MaterialScheme scheme) {
            return scheme.onTertiaryContainer().copy();
        }

        @Override
        public Color itemIcon(MaterialScheme scheme) {
            return scheme.onTertiaryContainer().copy();
        }

        @Override
        public Color selectedItemBackground(MaterialScheme scheme) {
            return scheme.tertiary().copy();
        }

        @Override
        public Color selectedItemText(MaterialScheme scheme) {
            return scheme.onTertiary().copy();
        }

        @Override
        public Color selectedItemIcon(MaterialScheme scheme) {
            return scheme.onTertiary().copy();
        }

        @Override
        public Color stateLayer(MaterialScheme scheme) {
            return scheme.onTertiaryContainer().copy();
        }
    };
}
