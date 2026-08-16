/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package io.homo.superresolution.api.registry;

public enum LowLatencyMarker {
    SIMULATION_START,
    SIMULATION_END,
    RENDER_SUBMIT_START,
    RENDER_SUBMIT_END,
    PRESENT_START,
    PRESENT_END,
    TRIGGER_FLASH,
    LATENCY_PING
}
