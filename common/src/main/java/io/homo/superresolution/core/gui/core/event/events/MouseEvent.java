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

package io.homo.superresolution.core.gui.core.event.events;

import io.homo.superresolution.core.gui.core.event.Event;
import org.joml.Vector2f;

public class MouseEvent {
    public static class MouseDragEvent extends Event {
        private final int button;
        private final Vector2f mousePosition;
        private final Vector2f dragDelta;

        public MouseDragEvent(int button, Vector2f mousePosition, Vector2f dragDelta) {
            this.button = button;
            this.mousePosition = mousePosition;
            this.dragDelta = dragDelta;
        }

        public int getButton() {
            return button;
        }

        public Vector2f getMousePosition() {
            return mousePosition;
        }

        public Vector2f getDragDelta() {
            return dragDelta;
        }
    }

    public static class MouseScrollEvent extends Event {
        private final Vector2f mousePosition;
        private final float scrollY;

        public MouseScrollEvent(Vector2f mousePosition, float scrollY) {
            this.mousePosition = mousePosition;
            this.scrollY = scrollY;
        }

        public Vector2f getMousePosition() {
            return mousePosition;
        }

        public float getScrollY() {
            return scrollY;
        }
    }


    public static class MousePressEvent extends Event {
        private final Vector2f mousePosition;
        private final int button;

        public MousePressEvent(Vector2f mousePosition, int button) {
            this.mousePosition = mousePosition;
            this.button = button;
        }

        public Vector2f getMousePosition() {
            return mousePosition;
        }

        public int getButton() {
            return button;
        }
    }

    public static class MouseMoveEvent extends Event {
        private final Vector2f mousePosition;

        public MouseMoveEvent(Vector2f mousePosition) {
            this.mousePosition = mousePosition;
        }

        public Vector2f getMousePosition() {
            return mousePosition;
        }
    }

    public static class MouseReleaseEvent extends Event {
        private final Vector2f mousePosition;
        private final int button;

        public MouseReleaseEvent(Vector2f mousePosition, int button) {
            this.mousePosition = mousePosition;
            this.button = button;
        }

        public Vector2f getMousePosition() {
            return mousePosition;
        }

        public int getButton() {
            return button;
        }
    }
}
