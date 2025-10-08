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

package io.homo.superresolution.core.gui.core.event;

public interface GuiEventListener {
    default void mousePress(float x, float y, int button) {
    }

    default void mouseRelease(float x, float y, int button) {
    }

    default void mouseMove(float x, float y) {
    }

    default void mouseDrag(float mouseX, float mouseY, float dragX, float dragY, int button) {
    }

    default void mouseScroll(float x, float y, double scrollX) {
    }

    default void keyPress(int keyCode, int scancode, int modifiers) {
    }

    default void keyRelease(int keyCode, int scancode, int modifiers) {
    }

    default void charTyped(char codePoint, int modifiers) {
    }
}