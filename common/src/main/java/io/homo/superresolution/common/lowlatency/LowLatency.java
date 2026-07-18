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

package io.homo.superresolution.common.lowlatency;

import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.lowlatency.nv.NVIDIAReflex;
import io.homo.superresolution.common.minecraft.MinecraftUtils;
import io.homo.superresolution.common.presentation.vulkan.VulkanPresentationFeature;
import io.homo.superresolution.core.streamline.StreamlineTypes;

import javax.annotation.Nullable;

public class LowLatency {
    private static boolean pendingLatencyPing;

    public static LowLatencyMode mode() {
        return mode;
    }

    private static LowLatencyMode mode;

    private static @Nullable ILowLatency lowLatency;

    public static ILowLatency lowLatency() {
        return lowLatency;
    }

    private static ILowLatency createLowLatency(LowLatencyMode newMode){
        return switch(newMode){
            case None -> new NoneLowLatency();
            case NVReflex ->  new NVIDIAReflex();
        };
    }

    public static void setMode(LowLatencyMode newMode) {
        if (!isAvailable()) return;
        if (mode != newMode) {
            mode = newMode;
            if (lowLatency != null) {
                lowLatency.release();
            }
            lowLatency = null;
            lowLatency = createLowLatency(newMode);
            lowLatency.refresh();
        }
    }

    public static int frameLimitUs() {
        int framerateLimit = MinecraftUtils.getFramerateLimit();
        if (framerateLimit <= 0) {
            return 0;
        }
        return (1_000_000 + framerateLimit - 1) / framerateLimit;
    }

    public static void beginPresent(){
        if (!SuperResolution.gameIsLoaded) return;
        if (lowLatency != null) {
            lowLatency.setMarker(LowLatencyMarker.PRESENT_START);
        }
    }

    public static void endPresent(){
        if (!SuperResolution.gameIsLoaded) return;

        if (lowLatency != null) {
            lowLatency.setMarker(LowLatencyMarker.PRESENT_END);
        }
    }

    public static void beginSimulation(){
        if (!SuperResolution.gameIsLoaded) return;

        if (lowLatency != null) {
            lowLatency.setMarker(LowLatencyMarker.SIMULATION_START);
        }
    }

    public static void endSimulation(){
        if (!SuperResolution.gameIsLoaded) return;

        if (lowLatency != null) {
            lowLatency.setMarker(LowLatencyMarker.SIMULATION_END);
        }
    }

    public static void beginSubmission(){
        if (!SuperResolution.gameIsLoaded) return;

        if (lowLatency != null) {
            lowLatency.setMarker(LowLatencyMarker.RENDER_SUBMIT_START);
        }
    }

    public static void endSubmission(){
        if (!SuperResolution.gameIsLoaded) return;

        if (lowLatency != null) {
            lowLatency.setMarker(LowLatencyMarker.RENDER_SUBMIT_END);
        }
    }

    public static void beginFrame(){
        if (!SuperResolution.gameIsLoaded) return;

        if (lowLatency == null && isAvailable()){
            lowLatency = createLowLatency(SuperResolutionConfig.getLowLatencyMode());
        }
        if (lowLatency != null) {
            lowLatency.refresh();
        }
    }

    public static void onLatencyPing(boolean pressed) {
        if (!SuperResolution.gameIsLoaded) return;

        if (!pressed) {
            return;
        }
        if (lowLatency != null) {
            lowLatency.setMarker(LowLatencyMarker.LATENCY_PING);
        }
    }

    public static void onTriggerFlash() {
        if (!SuperResolution.gameIsLoaded) return;

        if (lowLatency != null) {
            lowLatency.setMarker(LowLatencyMarker.TRIGGER_FLASH);
        }
    }

    public static void sleep(){
        if (!SuperResolution.gameIsLoaded) return;

        if (lowLatency != null) {
            lowLatency.sleep();
        }
    }

    public static boolean isAvailable(){
        return SuperResolutionConfig.isEnableVulkanPresentation() && VulkanPresentationFeature.isAvailable();
    }
}
