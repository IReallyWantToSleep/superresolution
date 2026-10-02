package io.homo.superresolution.common.presentation.api;

public interface PresentationBackend {
    PresentationBackendType type();

    boolean isAvailable();

    boolean isInitialized();

    void beginRealFrameRendering();

    void endRealFrameRendering();

    void endMinecraftFrame();

    void flushCapturedFrame();

    void setVsync(boolean enabled);

    boolean shutdownApplicationManagedProvider(String providerId, Runnable teardown);

    void shutdown();
}
