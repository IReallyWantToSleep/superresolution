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

public interface LowLatencyProvider {
    void setMarker(LowLatencyMarker marker);

    void release();

    void refresh();

    void sleep();

    default void invalidatePacing() {
    }
}
