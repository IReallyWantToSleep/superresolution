/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
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

package io.homo.superresolution.common.lowlatency.nv;

import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.lowlatency.ILowLatency;
import io.homo.superresolution.common.lowlatency.LowLatency;
import io.homo.superresolution.common.lowlatency.LowLatencyMarker;
import io.homo.superresolution.core.streamline.Streamline;
import io.homo.superresolution.core.streamline.StreamlineTypes;

public class NVIDIAReflex implements ILowLatency {
    @Override
    public void setMarker(LowLatencyMarker marker) {
        Streamline.session().pclSetMarker(
                switch (marker){
                    case SIMULATION_START -> StreamlineTypes.PclMarker.SIMULATION_START;
                    case SIMULATION_END -> StreamlineTypes.PclMarker.SIMULATION_END;
                    case RENDER_SUBMIT_START -> StreamlineTypes.PclMarker.RENDER_SUBMIT_START;
                    case RENDER_SUBMIT_END -> StreamlineTypes.PclMarker.RENDER_SUBMIT_END;
                    case PRESENT_START -> StreamlineTypes.PclMarker.PRESENT_START;
                    case PRESENT_END -> StreamlineTypes.PclMarker.PRESENT_END;
                    case TRIGGER_FLASH -> StreamlineTypes.PclMarker.TRIGGER_FLASH;
                    case LATENCY_PING -> StreamlineTypes.PclMarker.LATENCY_PING;
                },
                Streamline.currentFrame()
        );
    }

    @Override
    public void release() {
        StreamlineTypes.ReflexOptions options = new StreamlineTypes.ReflexOptions();
        options.mode = StreamlineTypes.ReflexMode.OFF;
        options.frameLimitUs = LowLatency.frameLimitUs();
        options.virtualKey = StreamlineTypes.PclHotKey.VK_F13;
        options.threadId = WinThreadId.INSTANCE.GetCurrentThreadId();
        options.useMarkersToOptimize = false;
        Streamline.session().reflexSetOptions(options);
    }

    @Override
    public void refresh() {
        StreamlineTypes.ReflexOptions options = new StreamlineTypes.ReflexOptions();
        options.mode = switch (SuperResolutionConfig.getNVIDIAReflexMode()){
            case OFF -> StreamlineTypes.ReflexMode.OFF;
            case ON -> StreamlineTypes.ReflexMode.LOW_LATENCY;
            case BOOST -> StreamlineTypes.ReflexMode.LOW_LATENCY_WITH_BOOST;
        };
        options.frameLimitUs = LowLatency.frameLimitUs();
        options.virtualKey = StreamlineTypes.PclHotKey.VK_F13;
        options.threadId = WinThreadId.INSTANCE.GetCurrentThreadId();
        options.useMarkersToOptimize = false;
        Streamline.session().reflexSetOptions(options);
    }

    @Override
    public void sleep() {
        Streamline.session().reflexSleep(
                Streamline.currentFrame()
        );
    }
}
