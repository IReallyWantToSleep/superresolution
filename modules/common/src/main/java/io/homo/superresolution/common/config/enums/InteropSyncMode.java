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

package io.homo.superresolution.common.config.enums;

import net.minecraft.network.chat.Component;

public enum InteropSyncMode {
    LowLatency(Component.translatable("superresolution.screen.config.options.label.interop_sync_mode.low_latency")),
    HighPerformance(Component.translatable("superresolution.screen.config.options.label.interop_sync_mode.high_performance"));

    private final Component component;

    InteropSyncMode(Component component) {
        this.component = component;
    }

    public Component getComponent() {
        return component;
    }

    @Override
    public String toString() {
        return component.getString();
    }
}
