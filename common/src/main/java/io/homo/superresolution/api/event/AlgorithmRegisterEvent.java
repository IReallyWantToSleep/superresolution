package io.homo.superresolution.api.event;

public interface AlgorithmRegisterEvent {
    Event<AlgorithmRegisterEvent> EVENT = EventFactory.create(
            AlgorithmRegisterEvent.class,
            (listeners) -> () -> {
                for (AlgorithmRegisterEvent listener : listeners) {
                    listener.onAlgorithmRegister();
                }
            }
    );

    void onAlgorithmRegister();
}
