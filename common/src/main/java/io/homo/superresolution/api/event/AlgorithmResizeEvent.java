package io.homo.superresolution.api.event;

import io.homo.superresolution.api.AbstractAlgorithm;

public interface AlgorithmResizeEvent {
    Event<AlgorithmResizeEvent> EVENT = EventFactory.create(
            AlgorithmResizeEvent.class,
            (listeners) -> (algorithm, screenWidth, screenHeight, renderWidth, renderHeight) -> {
                for (AlgorithmResizeEvent listener : listeners) {
                    listener.onAlgorithmRegister(algorithm, screenWidth, screenHeight, renderWidth, renderHeight);
                }
            }
    );

    void onAlgorithmRegister(
            AbstractAlgorithm algorithm,
            int screenWidth, int screenHeight,
            int renderWidth, int renderHeight
    );
}
