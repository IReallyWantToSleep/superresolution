package io.homo.superresolution.api.event;

import io.homo.superresolution.api.AbstractAlgorithm;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;

public interface AlgorithmDispatchFinishEvent {
    Event<AlgorithmDispatchFinishEvent> EVENT = EventFactory.create(
            AlgorithmDispatchFinishEvent.class,
            (listeners) -> (algorithm, outputTexture) -> {
                for (AlgorithmDispatchFinishEvent listener : listeners) {
                    listener.onAlgorithmDispatchFinish(algorithm, outputTexture);
                }
            }
    );

    void onAlgorithmDispatchFinish(AbstractAlgorithm algorithm, ITexture outputTexture);
}
