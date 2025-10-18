package io.homo.superresolution.core.gui.core.event;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class EventHandler {
    private final Map<Class<? extends Event>, List<EventListener<?>>> listeners;

    public EventHandler() {
        this.listeners = new ConcurrentHashMap<>();
    }

    public <E extends Event> void addListener(Class<E> eventType, EventListener<E> listener) {
        List<EventListener<?>> eventListeners = listeners.computeIfAbsent(
                eventType, k -> new CopyOnWriteArrayList<>()
        );
        eventListeners.add(listener);
    }

    public <E extends Event> void removeListener(Class<E> eventType, EventListener<E> listener) {
        List<EventListener<?>> eventListeners = listeners.get(eventType);
        if (eventListeners != null) {
            eventListeners.remove(listener);
        }
    }

    public <E extends Event> void on(Class<E> eventType, EventListener<E> listener) {
        addListener(eventType, listener);
    }

    public <E extends Event> void off(Class<E> eventType, EventListener<E> listener) {
        removeListener(eventType, listener);
    }

    @SuppressWarnings("unchecked")
    public <E extends Event> E fire(E event) {
        List<EventListener<?>> eventListeners = listeners.get(event.getClass());
        if (eventListeners != null) {
            for (EventListener<?> listener : eventListeners) {
                try {
                    ((EventListener<E>) listener).handle(event);
                } catch (Exception e) {
                    System.err.println("Error handling event: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        return event;
    }

    public void clear() {
        listeners.clear();
    }

    public int getListenerCount(Class<? extends Event> eventType) {
        List<EventListener<?>> eventListeners = listeners.get(eventType);
        return eventListeners != null ? eventListeners.size() : 0;
    }

    public boolean hasListeners(Class<? extends Event> eventType) {
        List<EventListener<?>> eventListeners = listeners.get(eventType);
        return eventListeners != null && !eventListeners.isEmpty();
    }
}