package io.homo.superresolution.api.event;

import java.util.function.Function;

public class EventFactory {
    public static <T> Event<T> create(Class<T> listenerType, Function<T[], T> invokerFactory) {
        return new Event<>(listenerType, invokerFactory);
    }
}