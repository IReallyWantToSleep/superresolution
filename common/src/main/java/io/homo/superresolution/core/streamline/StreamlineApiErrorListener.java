package io.homo.superresolution.core.streamline;

@FunctionalInterface
public interface StreamlineApiErrorListener {
    void onApiError(int vulkanResult);
}
