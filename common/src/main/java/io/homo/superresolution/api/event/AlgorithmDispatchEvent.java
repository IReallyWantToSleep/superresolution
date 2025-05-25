package io.homo.superresolution.api.event;

import io.homo.superresolution.api.AbstractAlgorithm;
import io.homo.superresolution.common.upscale.DispatchResource;

public interface AlgorithmDispatchEvent {
    Event<AlgorithmDispatchEvent> EVENT = EventFactory.create(
            AlgorithmDispatchEvent.class,
            (listeners) -> (algorithm, dispatchResource) -> {
                for (AlgorithmDispatchEvent listener : listeners) {
                    listener.onAlgorithmDispatch(algorithm, dispatchResource);
                }
            }
    );

    void onAlgorithmDispatch(AbstractAlgorithm algorithm, DispatchResource dispatchResource);
}
