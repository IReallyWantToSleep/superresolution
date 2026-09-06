package io.homo.superresolution.common.presentation.opengl;

import io.homo.superresolution.common.presentation.api.PresentationBackend;
import io.homo.superresolution.common.presentation.api.PresentationBackendType;

public final class OpenGLPresentationBackend implements PresentationBackend {
    @Override
    public PresentationBackendType type() {
        return PresentationBackendType.OPENGL;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public boolean isInitialized() {
        return true;
    }

    @Override
    public void endMinecraftFrame() {
    }

    @Override
    public void flushCapturedFrame() {
    }

    @Override
    public void setVsync(boolean enabled) {
    }

    @Override
    public boolean shutdownApplicationManagedProvider(String providerId, Runnable teardown) {
        return false;
    }

    @Override
    public void shutdown() {
    }
}
