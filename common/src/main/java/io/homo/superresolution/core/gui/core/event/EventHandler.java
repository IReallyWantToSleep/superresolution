/*
 * Super Resolution
 * Copyright (c) 2025. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.homo.superresolution.core.gui.core.event;

import java.lang.reflect.Method;
import java.util.*;
import java.util.function.*;

public class EventHandler<T> implements EventHandle<T> {
    private final Set<String> registeredEventTypes;
    private final Map<String, List<Object>> eventListeners;
    private final Map<String, Class<?>[]> eventParameterTypes;
    private final T eventSource;

    public EventHandler(T eventSource) {
        this.eventSource = eventSource;
        this.registeredEventTypes = new HashSet<>();
        this.eventListeners = new HashMap<>();
        this.eventParameterTypes = new HashMap<>();
    }

    public boolean registerEventType(String eventType) {
        return registerEventType(eventType, new Class<?>[0]);
    }

    public <P1> boolean registerEventType(String eventType, Class<P1> param1Type) {
        return registerEventType(eventType, new Class<?>[]{param1Type});
    }

    public <P1, P2> boolean registerEventType(String eventType, Class<P1> param1Type, Class<P2> param2Type) {
        return registerEventType(eventType, new Class<?>[]{param1Type, param2Type});
    }

    public <P1, P2, P3> boolean registerEventType(String eventType, Class<P1> param1Type, Class<P2> param2Type, Class<P3> param3Type) {
        return registerEventType(eventType, new Class<?>[]{param1Type, param2Type, param3Type});
    }

    public <P1, P2, P3, P4> boolean registerEventType(String eventType, Class<P1> param1Type, Class<P2> param2Type, Class<P3> param3Type, Class<P4> param4Type) {
        return registerEventType(eventType, new Class<?>[]{param1Type, param2Type, param3Type, param4Type});
    }

    public <P1, P2, P3, P4, P5> boolean registerEventType(String eventType, Class<P1> param1Type, Class<P2> param2Type, Class<P3> param3Type, Class<P4> param4Type, Class<P5> param5Type) {
        return registerEventType(eventType, new Class<?>[]{param1Type, param2Type, param3Type, param4Type, param5Type});
    }

    public <P1, P2, P3, P4, P5, P6> boolean registerEventType(String eventType, Class<P1> param1Type, Class<P2> param2Type, Class<P3> param3Type, Class<P4> param4Type, Class<P5> param5Type, Class<P6> param6Type) {
        return registerEventType(eventType, new Class<?>[]{param1Type, param2Type, param3Type, param4Type, param5Type, param6Type});
    }

    public boolean registerEventType(String eventType, Class<?>... parameterTypes) {
        if (eventType == null || eventType.trim().isEmpty()) {
            throw new IllegalArgumentException("Event type cannot be null or empty");
        }

        if (registeredEventTypes.contains(eventType)) {
            return false;
        }

        registeredEventTypes.add(eventType);
        eventListeners.put(eventType, new ArrayList<>());
        eventParameterTypes.put(eventType, parameterTypes != null ? parameterTypes : new Class<?>[0]);
        return true;
    }

    public void registerEventTypes(Map<String, Class<?>[]> eventTypes) {
        for (Map.Entry<String, Class<?>[]> entry : eventTypes.entrySet()) {
            registerEventType(entry.getKey(), entry.getValue());
        }
    }

    public boolean isEventTypeRegistered(String eventType) {
        return registeredEventTypes.contains(eventType);
    }

    public Class<?>[] getEventParameterTypes(String eventType) {
        validateEventType(eventType);
        return eventParameterTypes.get(eventType);
    }

    public int getEventParameterCount(String eventType) {
        validateEventType(eventType);
        return eventParameterTypes.get(eventType).length;
    }

    @Override
    public void addEventListener(String eventType, Object listener) {
        validateEventType(eventType);

        if (listener == null) {
            throw new IllegalArgumentException("Listener cannot be null");
        }

        Class<?>[] expectedTypes = eventParameterTypes.get(eventType);

        if (!isListenerCompatible(listener, expectedTypes)) {
            throw new IllegalArgumentException(
                    "Listener type mismatch for event '" + eventType +
                            "'. Expected parameters: " + Arrays.toString(expectedTypes)
            );
        }

        List<Object> listeners = eventListeners.get(eventType);
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void addEventListener(String eventType, Consumer<T> listener) {
        addEventListener(eventType, (Object) listener);
    }

    public <P1> void addEventListener(String eventType, BiConsumer<T, P1> listener) {
        addEventListener(eventType, (Object) listener);
    }

    @Override
    public void removeEventListener(String eventType, Object listener) {
        validateEventType(eventType);

        List<Object> listeners = eventListeners.get(eventType);
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    public void triggerEvent(String eventType) {
        triggerEvent(eventType, new Object[0]);
    }

    public <P1> void triggerEvent(String eventType, P1 param1) {
        triggerEvent(eventType, new Object[]{param1});
    }

    public <P1, P2> void triggerEvent(String eventType, P1 param1, P2 param2) {
        triggerEvent(eventType, new Object[]{param1, param2});
    }

    public <P1, P2, P3> void triggerEvent(String eventType, P1 param1, P2 param2, P3 param3) {
        triggerEvent(eventType, new Object[]{param1, param2, param3});
    }

    public <P1, P2, P3, P4> void triggerEvent(String eventType, P1 param1, P2 param2, P3 param3, P4 param4) {
        triggerEvent(eventType, new Object[]{param1, param2, param3, param4});
    }

    public <P1, P2, P3, P4, P5> void triggerEvent(String eventType, P1 param1, P2 param2, P3 param3, P4 param4, P5 param5) {
        triggerEvent(eventType, new Object[]{param1, param2, param3, param4, param5});
    }

    public <P1, P2, P3, P4, P5, P6> void triggerEvent(String eventType, P1 param1, P2 param2, P3 param3, P4 param4, P5 param5, P6 param6) {
        triggerEvent(eventType, new Object[]{param1, param2, param3, param4, param5, param6});
    }

    public void triggerEvent(String eventType, Object... parameters) {
        validateEventType(eventType);

        Class<?>[] expectedTypes = eventParameterTypes.get(eventType);

        if (parameters.length != expectedTypes.length) {
            throw new IllegalArgumentException(
                    "Parameter count mismatch for event '" + eventType +
                            "'. Expected: " + expectedTypes.length + ", Got: " + parameters.length
            );
        }

        for (int i = 0; i < parameters.length; i++) {
            Object param = parameters[i];
            Class<?> expectedType = expectedTypes[i];

            if (param != null && !expectedType.isInstance(param)) {
                throw new IllegalArgumentException(
                        "Parameter type mismatch for event '" + eventType +
                                "' at position " + i + ". Expected: " + expectedType.getName() +
                                ", Got: " + param.getClass().getName()
                );
            }
        }

        List<Object> listeners = new ArrayList<>(eventListeners.get(eventType));
        for (Object listener : listeners) {
            try {
                invokeListener(listener, parameters);
            } catch (Exception e) {
                handleListenerException(eventType, e);
            }
        }
    }

    public int getListenerCount(String eventType) {
        validateEventType(eventType);
        return eventListeners.get(eventType).size();
    }

    public void clearEventListeners(String eventType) {
        validateEventType(eventType);
        eventListeners.get(eventType).clear();
    }

    public void clearAllEventListeners() {
        for (List<Object> listeners : eventListeners.values()) {
            listeners.clear();
        }
    }

    public Set<String> getRegisteredEventTypes() {
        return Collections.unmodifiableSet(registeredEventTypes);
    }

    public T getEventSource() {
        return eventSource;
    }

    private boolean isListenerCompatible(Object listener, Class<?>[] expectedTypes) {
        Class<?> listenerClass = listener.getClass();

        if (!listenerClass.isSynthetic() && !listenerClass.getName().contains("$$Lambda")) {
            Method[] methods = listenerClass.getMethods();
            for (Method method : methods) {
                if (method.getName().equals("accept") ||
                        method.getName().equals("apply") ||
                        method.getName().equals("test") ||
                        method.getName().equals("run")) {

                    Class<?>[] paramTypes = method.getParameterTypes();
                    if (isParameterTypesCompatible(paramTypes, expectedTypes)) {
                        return true;
                    }
                }
            }
            return false;
        }

        return true;
    }

    private boolean isParameterTypesCompatible(Class<?>[] actualTypes, Class<?>[] expectedTypes) {
        if (actualTypes.length != expectedTypes.length + 1) {
            return false;
        }

        if (!actualTypes[0].isAssignableFrom(eventSource.getClass())) {
            return false;
        }

        for (int i = 0; i < expectedTypes.length; i++) {
            if (!actualTypes[i + 1].isAssignableFrom(expectedTypes[i])) {
                return false;
            }
        }

        return true;
    }

    private void invokeListener(Object listener, Object[] parameters) throws Exception {
        Class<?> listenerClass = listener.getClass();
        Object[] invokeParams = new Object[parameters.length + 1];
        invokeParams[0] = eventSource;
        System.arraycopy(parameters, 0, invokeParams, 1, parameters.length);

        Method[] methods = listenerClass.getMethods();
        for (Method method : methods) {
            if (isMethodCompatible(method, invokeParams)) {
                method.setAccessible(true);
                method.invoke(listener, invokeParams);
                return;
            }
        }

        for (Method method : methods) {
            if ((method.getName().equals("accept") || method.getName().equals("apply") ||
                    method.getName().equals("test") || method.getName().equals("run")) &&
                    method.getParameterCount() == invokeParams.length) {
                try {
                    method.invoke(listener, invokeParams);
                    return;
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new IllegalStateException("Invoke event failed: " + listenerClass.getName());
                }
            }
        }

        throw new IllegalStateException("No compatible method found for listener: " + listenerClass.getName());
    }

    private boolean isMethodCompatible(Method method, Object[] parameters) {
        Class<?>[] paramTypes = method.getParameterTypes();
        if (paramTypes.length != parameters.length) {
            return false;
        }

        for (int i = 0; i < paramTypes.length; i++) {
            if (parameters[i] != null && !paramTypes[i].isInstance(parameters[i])) {
                return false;
            }
        }

        return true;
    }

    private void validateEventType(String eventType) {
        if (!registeredEventTypes.contains(eventType)) {
            throw new IllegalArgumentException("Event type '" + eventType + "' is not registered");
        }
    }

    private void handleListenerException(String eventType, Exception e) {
        System.err.println("Error executing event listener for type: " + eventType);
        e.printStackTrace();
    }
}