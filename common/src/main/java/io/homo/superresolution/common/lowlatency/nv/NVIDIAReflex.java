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

import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.lowlatency.ILowLatency;
import io.homo.superresolution.common.lowlatency.LowLatency;
import io.homo.superresolution.common.lowlatency.LowLatencyMarker;
import io.homo.superresolution.core.streamline.Streamline;
import io.homo.superresolution.core.streamline.StreamlineSession;
import io.homo.superresolution.core.streamline.StreamlineTypes;

public final class NVIDIAReflex implements ILowLatency {
    private OptionsKey lastAppliedOptions;
    private StreamlineSession observedSession;

    @Override
    public void setMarker(LowLatencyMarker marker) {
        StreamlineSession session = sessionOrNull();
        StreamlineTypes.FrameToken token = tokenOrNull();
        if (session == null || token == null) {
            return;
        }
        session.pclSetMarker(
                switch (marker) {
                    case SIMULATION_START -> StreamlineTypes.PclMarker.SIMULATION_START;
                    case SIMULATION_END -> StreamlineTypes.PclMarker.SIMULATION_END;
                    case RENDER_SUBMIT_START -> StreamlineTypes.PclMarker.RENDER_SUBMIT_START;
                    case RENDER_SUBMIT_END -> StreamlineTypes.PclMarker.RENDER_SUBMIT_END;
                    case PRESENT_START -> StreamlineTypes.PclMarker.PRESENT_START;
                    case PRESENT_END -> StreamlineTypes.PclMarker.PRESENT_END;
                    case TRIGGER_FLASH -> StreamlineTypes.PclMarker.TRIGGER_FLASH;
                    case LATENCY_PING -> StreamlineTypes.PclMarker.LATENCY_PING;
                },
                token
        );
    }

    @Override
    public void release() {
        StreamlineSession session = sessionOrNull();
        if (session == null) {
            lastAppliedOptions = null;
            return;
        }
        applyOptions(session, new OptionsKey(
                StreamlineTypes.ReflexMode.OFF,
                LowLatency.frameLimitUs(),
                StreamlineTypes.PclHotKey.VK_F13,
                WinThreadId.INSTANCE.GetCurrentThreadId(),
                false
        ), true);
        lastAppliedOptions = null;
        observedSession = null;
    }

    @Override
    public void refresh() {
        StreamlineSession session = sessionOrNull();
        if (session == null) {
            return;
        }
        OptionsKey desired = new OptionsKey(
                switch (SuperResolutionConfig.getNVIDIAReflexMode()) {
                    case OFF -> StreamlineTypes.ReflexMode.OFF;
                    case ON -> StreamlineTypes.ReflexMode.LOW_LATENCY;
                    case BOOST -> StreamlineTypes.ReflexMode.LOW_LATENCY_WITH_BOOST;
                },
                LowLatency.frameLimitUs(),
                StreamlineTypes.PclHotKey.VK_F13,
                WinThreadId.INSTANCE.GetCurrentThreadId(),
                false
        );
        applyOptions(session, desired, false);
    }

    @Override
    public void sleep() {
        StreamlineSession session = sessionOrNull();
        StreamlineTypes.FrameToken token = tokenOrNull();
        if (session != null && token != null) {
            session.reflexSleep(token);
        }
    }

    private void applyOptions(StreamlineSession session, OptionsKey desired, boolean force) {
        if (!force && desired.equals(lastAppliedOptions)) {
            return;
        }
        StreamlineTypes.ReflexOptions options = new StreamlineTypes.ReflexOptions();
        options.mode = desired.mode();
        options.frameLimitUs = desired.frameLimitUs();
        options.virtualKey = desired.virtualKey();
        options.threadId = desired.threadId();
        options.useMarkersToOptimize = desired.useMarkersToOptimize();
        if (session.reflexSetOptions(options) == 0) {
            lastAppliedOptions = desired;
        }
    }

    private StreamlineSession sessionOrNull() {
        if (!Streamline.isInitialized()) {
            observedSession = null;
            lastAppliedOptions = null;
            return null;
        }
        StreamlineSession session = Streamline.session();
        if (session == null || session.isClosed()) {
            observedSession = null;
            lastAppliedOptions = null;
            return null;
        }
        if (observedSession != session) {
            observedSession = session;
            lastAppliedOptions = null;
        }
        return session;
    }

    private static StreamlineTypes.FrameToken tokenOrNull() {
        StreamlineTypes.FrameToken token = Streamline.currentFrame();
        return token != null && token.nativeHandle != 0L ? token : null;
    }

    private record OptionsKey(
            int mode,
            int frameLimitUs,
            int virtualKey,
            int threadId,
            boolean useMarkersToOptimize
    ) {
    }
}
