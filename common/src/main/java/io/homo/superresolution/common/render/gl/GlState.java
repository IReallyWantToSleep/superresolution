package io.homo.superresolution.common.render.gl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GlState {
    private static final Map<Object, State> states = new ConcurrentHashMap<>();

    public static void save(Object id) {
        int readFBO = Gl.glGetInteger(GlConst.GL_READ_FRAMEBUFFER_BINDING);
        int writeFBO = Gl.glGetInteger(GlConst.GL_DRAW_FRAMEBUFFER_BINDING);
        states.put(id, new State(readFBO, writeFBO));
    }

    public static State pop(Object id) {
        return states.remove(id);
    }

    public static State get(Object id) {
        return states.get(id);
    }

    public static void remove(Object id) {
        states.remove(id);
    }

    public static void clear() {
        states.clear();
    }

    public record State(int readFBO, int writeFBO) {
    }
}