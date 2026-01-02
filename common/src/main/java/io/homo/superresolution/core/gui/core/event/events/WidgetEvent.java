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

package io.homo.superresolution.core.gui.core.event.events;

import net.neoforged.bus.api.Event;
import org.joml.Vector2f;

public class WidgetEvent {
    public static class HoverEvent extends Event {
        private final Vector2f mousePosition;
        private final boolean hovering;

        public HoverEvent(Vector2f mousePosition, boolean hovering) {
            this.mousePosition = mousePosition;
            this.hovering = hovering;
        }

        public Vector2f getMousePosition() {
            return mousePosition;
        }

        public boolean isHovering() {
            return hovering;
        }
    }

    public static class FocusEvent extends Event {
        private final boolean focusing;

        public FocusEvent(Vector2f mousePosition, boolean focusing) {
            this.focusing = focusing;
        }

        public boolean isFocusing() {
            return focusing;
        }
    }

    public static class ClickEvent<T> extends Event {
        private final T widget;


        public ClickEvent(T widget) {
            this.widget = widget;
        }

        public T getWidget() {
            return widget;
        }
    }

    public static class ChangeEvent<T> extends Event {
        private final T newValue;

        public T getOldValue() {
            return oldValue;
        }

        private final T oldValue;


        public ChangeEvent(T oldValue, T newValue) {
            this.newValue = newValue;
            this.oldValue = oldValue;

        }

        public T getNewValue() {
            return newValue;
        }
    }
}
