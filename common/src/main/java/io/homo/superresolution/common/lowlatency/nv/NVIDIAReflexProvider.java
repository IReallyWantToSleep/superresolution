/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package io.homo.superresolution.common.lowlatency.nv;

import io.homo.superresolution.api.registry.LowLatencyMarker;
import io.homo.superresolution.api.registry.LowLatencyProvider;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.framegeneration.FrameGeneration;
import io.homo.superresolution.common.framegeneration.FrameGenerationBackend;
import io.homo.superresolution.core.streamline.Streamline;

public final class NVIDIAReflexProvider implements LowLatencyProvider {
    private ReflexImplementation active;

    @Override
    public void setMarker(LowLatencyMarker marker) {
        ReflexImplementation impl = activeImpl();
        if (impl != null) {
            impl.setMarker(marker);
        }
    }

    @Override
    public void release() {
        if (active != null) {
            active.release();
            active = null;
        }
    }

    @Override
    public void refresh() {
        ensureActiveImpl();
        if (active != null) {
            active.refresh(configuredMode());
        }
    }

    @Override
    public void sleep() {
        ReflexImplementation impl = activeImpl();
        if (impl != null) {
            impl.sleep();
        }
    }

    @Override
    public void invalidatePacing() {
        if (active != null) {
            active.invalidatePacing();
        }
    }

    private ReflexImplementation activeImpl() {
        ensureActiveImpl();
        return active;
    }

    private void ensureActiveImpl() {
        ReflexImplementation desired = selectImpl();
        if (desired.getClass().isInstance(active)) {
            return;
        }
        if (active != null) {
            active.release();
        }
        active = desired;
    }

    private ReflexImplementation selectImpl() {
        FrameGenerationBackend fgBackend = FrameGeneration.activeBackend();
        if (fgBackend == FrameGenerationBackend.STREAMLINE) {
            return new NVIDIAReflexStreamlineImpl();
        }
        if (fgBackend == FrameGenerationBackend.NGX) {
            return new NVIDIAReflexVulkanImpl();
        }
        if (Streamline.isInitialized()) {
            return new NVIDIAReflexStreamlineImpl();
        }
        return new NVIDIAReflexVulkanImpl();
    }

    private static int configuredMode() {
        return SuperResolutionConfig.getNVIDIAReflexMode().ordinal();
    }
}
