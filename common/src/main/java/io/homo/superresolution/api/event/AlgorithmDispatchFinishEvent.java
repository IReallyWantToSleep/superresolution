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
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import net.neoforged.bus.api.Event;

public class AlgorithmDispatchFinishEvent extends Event {
    private final AbstractAlgorithm algorithm;
    private final IFrameBuffer output;

    public AlgorithmDispatchFinishEvent(AbstractAlgorithm algorithm, IFrameBuffer output) {
        this.algorithm = algorithm;
        this.output = output;
    }

    public AbstractAlgorithm getAlgorithm() {
        return algorithm;
    }

    public IFrameBuffer getOutput() {
        return output;
    }
}
