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

package io.homo.superresolution.api.event;

import io.homo.superresolution.api.AbstractAlgorithm;

public interface AlgorithmResizeEvent {
    Event<AlgorithmResizeEvent> EVENT = EventFactory.create(
            AlgorithmResizeEvent.class,
            (listeners) -> (algorithm, screenWidth, screenHeight, renderWidth, renderHeight) -> {
                for (AlgorithmResizeEvent listener : listeners) {
                    listener.onAlgorithmResize(algorithm, screenWidth, screenHeight, renderWidth, renderHeight);
                }
            }
    );

    void onAlgorithmResize(
            AbstractAlgorithm algorithm,
            int screenWidth, int screenHeight,
            int renderWidth, int renderHeight
    );
}
