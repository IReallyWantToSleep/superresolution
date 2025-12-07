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

package io.homo.irisapi.handlers;

import io.homo.irisapi.*;
import io.homo.irisapi.mixin.composite.CompositeRendererAccessor;
import net.irisshaders.iris.pipeline.CompositeRenderer;

public class IrisRenderingPipelineHandler {
    public static void onCompositePassStart(
            CompositeRenderer compositeRenderer,
            NamedCompositePass compositePass,
            IrisCompositePassType passType
    ) {
        IrisAPI.EVENT_BUS.post(
                new IrisCompositePassRenderingEvent.PassBegin(
                        compositeRenderer,
                        IrisCompositeRenderingPhase.from(
                                ((CompositeRendererAccessor) compositeRenderer).getPipeline(),
                                compositeRenderer
                        ),
                        compositePass.superresolution$getName(),
                        passType,
                        compositePass
                )
        );
    }

    public static void onCompositePassEnd(
            CompositeRenderer compositeRenderer,
            NamedCompositePass compositePass,
            IrisCompositePassType passType
    ) {
        IrisAPI.EVENT_BUS.post(
                new IrisCompositePassRenderingEvent.PassEnd(
                        compositeRenderer,
                        IrisCompositeRenderingPhase.from(
                                ((CompositeRendererAccessor) compositeRenderer).getPipeline(),
                                compositeRenderer
                        ),
                        compositePass.superresolution$getName(),
                        passType,
                        compositePass
                )
        );
    }


    public static void onCompositePassDispatchBefore(
            CompositeRenderer compositeRenderer,
            NamedCompositePass compositePass,
            IrisCompositePassType passType
    ) {
        IrisAPI.EVENT_BUS.post(
                new IrisCompositePassRenderingEvent.BeforePassRender(
                        compositeRenderer,
                        IrisCompositeRenderingPhase.from(
                                ((CompositeRendererAccessor) compositeRenderer).getPipeline(),
                                compositeRenderer
                        ),
                        compositePass.superresolution$getName(),
                        passType,
                        compositePass
                )
        );
    }

    public static void onCompositePassDispatchAfter(
            CompositeRenderer compositeRenderer,
            NamedCompositePass compositePass,
            IrisCompositePassType passType
    ) {
        IrisAPI.EVENT_BUS.post(
                new IrisCompositePassRenderingEvent.AfterPassRender(
                        compositeRenderer,
                        IrisCompositeRenderingPhase.from(
                                ((CompositeRendererAccessor) compositeRenderer).getPipeline(),
                                compositeRenderer
                        ),
                        compositePass.superresolution$getName(),
                        passType,
                        compositePass
                )
        );
    }
}
