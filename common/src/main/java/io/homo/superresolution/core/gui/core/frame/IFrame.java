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

package io.homo.superresolution.core.gui.core.frame;

import io.homo.superresolution.core.gui.core.AbstractWidget;
import io.homo.superresolution.core.gui.core.UIInputState;
import io.homo.superresolution.core.gui.core.backends.render.RenderContext;
import io.homo.superresolution.core.gui.core.impl.Rectangle;
import org.joml.Vector2f;

public interface IFrame {
    AbstractWidget<?> getRoot();

    void setRoot(AbstractWidget<?> root);

    void setViewport(float width, float height);

    Rectangle getViewport();

    void calculateLayout();


    void render(RenderContext ctx, UIInputState inputState);


    void dispatchMouseMove(float x, float y);


    void dispatchMousePress(float x, float y, int button);


    void dispatchMouseRelease(float x, float y, int button);

    void dispatchMouseDrag(float mouseX, float mouseY, float dragX, float dragY, int button);

    void dispatchMouseScroll(float x, float y, double scrollX);

    void dispatchKeyPress(int keyCode, int scancode, int modifiers);

    void dispatchKeyRelease(int keyCode, int scancode, int modifiers);

    void dispatchCharTyped(char codePoint, int modifiers);

    AbstractWidget<?> findInteractiveWidgetAt(Vector2f pos);

    void markLayoutDirty();

    boolean isLayoutDirty();
}
