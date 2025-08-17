package io.homo.superresolution.api.event;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class Event<T> {
    private final Class<T> listenerType;
    private final Function<T[], T> invokerFactory;
    private final List<T> listeners = new ArrayList<>();
    private T invoker;

    public Event(Class<T> listenerType, Function<T[], T> invokerFactory) {
        this.listenerType = listenerType;
        this.invokerFactory = invokerFactory;
        updateInvoker();
    }

    public void register(T listener) {
        listeners.add(listener);
        updateInvoker();
    }

    public T invoker() {
        return invoker;
    }

    @SuppressWarnings("unchecked")
    private void updateInvoker() {
        T[] listenersArray = listeners.toArray((T[]) Array.newInstance(listenerType, 0));
        invoker = invokerFactory.apply(listenersArray);
    }

    public boolean hasEvent() {
        return !listeners.isEmpty();
    }
}
