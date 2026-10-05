/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
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

package io.homo.irisapi;

import io.homo.irisapi.mixin.composite.IrisRenderingPipelineAccessor;
import net.irisshaders.iris.pipeline.CompositeRenderer;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;

public enum IrisCompositeRenderingPhase {
    Begin,
    Prepare,
    Deferred,
    Composite,
    Unknown;

    public static IrisCompositeRenderingPhase from(WorldRenderingPipeline pipeline, ICompositeRendererAccessor compositeRenderer) {
        if (!(pipeline instanceof IrisRenderingPipeline)) return Unknown;
        return compositeRenderer.getPhase();
    }
}
