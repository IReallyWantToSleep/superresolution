package io.homo.superresolution.core.graphics.opengl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GlStates {
    private static final Map<Object, GlState> states = new ConcurrentHashMap<>();

    public static GlState save(Object id) {
        return states.put(id, new GlState());
    }

    public static GlState pop(Object id) {
        return states.remove(id);
    }

    public static GlState get(Object id) {
        return states.get(id);
    }

    public static void remove(Object id) {
        states.remove(id);
    }

    public static void clear() {
        states.clear();
    }
}