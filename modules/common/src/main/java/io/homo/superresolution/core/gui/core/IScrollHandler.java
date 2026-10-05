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

package io.homo.superresolution.core.gui.core;


import org.joml.Vector2f;

public interface IScrollHandler {
    record ScrollMetrics(
            float contentWidth,
            float contentHeight,
            float viewportWidth,
            float viewportHeight,
            boolean horizontalEnabled,
            boolean verticalEnabled
    ) {
        public ScrollMetrics {
            if (!Float.isFinite(contentWidth) || contentWidth < 0.0f) {
                throw new IllegalArgumentException("contentWidth must be finite and non-negative");
            }
            if (!Float.isFinite(contentHeight) || contentHeight < 0.0f) {
                throw new IllegalArgumentException("contentHeight must be finite and non-negative");
            }
            if (!Float.isFinite(viewportWidth) || viewportWidth < 0.0f) {
                throw new IllegalArgumentException("viewportWidth must be finite and non-negative");
            }
            if (!Float.isFinite(viewportHeight) || viewportHeight < 0.0f) {
                throw new IllegalArgumentException("viewportHeight must be finite and non-negative");
            }
        }

        public static ScrollMetrics empty() {
            return new ScrollMetrics(0.0f, 0.0f, 0.0f, 0.0f, false, false);
        }
    }

    void onDragStart(Vector2f position);

    void onDragMove(Vector2f position, Vector2f delta);

    void onDragEnd(Vector2f position);

    void onScroll(float deltaX, float deltaY);

    void scrollTo(Vector2f target);

    void setScroll(Vector2f target);

    void scrollBy(Vector2f delta);

    void update(float deltaTime);

    void stop();

    Vector2f getCurrentOffset();

    void setOnOffsetChanged(OnOffsetChangedListener listener);

    void setScrollMetrics(ScrollMetrics metrics);

    interface OnOffsetChangedListener {
        void onOffsetChanged(Vector2f newOffset);
    }
}
