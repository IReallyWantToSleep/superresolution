package io.homo.superresolution.api.event;

public interface ConfigChangedEvent {
    Event<ConfigChangedEvent> EVENT = EventFactory.create(
            ConfigChangedEvent.class,
            (listeners) -> () -> {
                for (ConfigChangedEvent listener : listeners) {
                    listener.onConfigReload();
                }
            }
    );

    void onConfigReload();
}
