package io.homo.superresolution.common.debug;

import java.util.HashMap;
import java.util.Map;

public class PerformanceInfo {
    private static final Map<String, Long> beginTimeMap = new HashMap<>();
    private static final Map<String, Long> usingTimeMap = new HashMap<>();


    public static long begin(String name) {
        beginTimeMap.put(name, System.nanoTime());
        return beginTimeMap.get(name);
    }

    public static long end(String name) {
        usingTimeMap.put(name, System.nanoTime() - beginTimeMap.get(name));
        return usingTimeMap.get(name);
    }

    public static long end(String name, long time) {
        usingTimeMap.put(name, time);
        return usingTimeMap.get(name);
    }

    public static long getAsNano(String name) {
        if (usingTimeMap.get(name) == null) return -1L;
        return usingTimeMap.get(name);
    }

    public static float getAsMillis(String name) {
        if (usingTimeMap.get(name) == null) return -1L;
        return (float) usingTimeMap.get(name) / 1000000L;
    }
}
