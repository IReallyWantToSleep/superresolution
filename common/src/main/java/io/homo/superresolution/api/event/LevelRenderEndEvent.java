package io.homo.superresolution.api.event;

import io.homo.superresolution.api.AbstractAlgorithm;
import io.homo.superresolution.common.upscale.DispatchResource;

public interface LevelRenderEndEvent {
    Event<LevelRenderEndEvent> EVENT = EventFactory.create(
            LevelRenderEndEvent.class,
            (listeners) -> () -> {
                for (LevelRenderEndEvent listener : listeners) {
                    listener.onLevelRenderEnd();
                }
            }
    );

    void onLevelRenderEnd();
}
