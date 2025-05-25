package io.homo.superresolution.api.event;

import io.homo.superresolution.api.AbstractAlgorithm;
import io.homo.superresolution.common.upscale.DispatchResource;

public interface LevelRenderStartEvent {
    Event<LevelRenderStartEvent> EVENT = EventFactory.create(
            LevelRenderStartEvent.class,
            (listeners) -> () -> {
                for (LevelRenderStartEvent listener : listeners) {
                    listener.onLevelRenderStart();
                }
            }
    );

    void onLevelRenderStart();
}
