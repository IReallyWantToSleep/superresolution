package io.homo.superresolution.core.streamline;

@FunctionalInterface
public interface StreamlineLogListener {
    void onLog(int type, String message);
}
