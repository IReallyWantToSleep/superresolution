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
import net.neoforged.bus.api.Event;

public class AlgorithmResizeEvent extends Event {
    private final AbstractAlgorithm algorithm;
    private final int screenWidth;
    private final int screenHeight;
    private final int renderWidth;
    private final int renderHeight;

    public AlgorithmResizeEvent(
            AbstractAlgorithm algorithm,
            int screenWidth, int screenHeight,
            int renderWidth, int renderHeight
    ) {
        this.algorithm = algorithm;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.renderWidth = renderWidth;
        this.renderHeight = renderHeight;
    }

    public AbstractAlgorithm getAlgorithm() {
        return algorithm;
    }

    public int getScreenWidth() {
        return screenWidth;
    }

    public int getScreenHeight() {
        return screenHeight;
    }

    public int getRenderWidth() {
        return renderWidth;
    }

    public int getRenderHeight() {
        return renderHeight;
    }
}
