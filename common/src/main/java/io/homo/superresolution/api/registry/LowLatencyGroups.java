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

package io.homo.superresolution.api.registry;

import net.minecraft.network.chat.Component;

/**
 * Low-latency algorithm groups predefined by Super Resolution. The group ids intentionally
 * match the pre-migration low-latency mode ids ({@code superresolution:none},
 * {@code superresolution:nv_reflex}) so that stored configuration values continue to load.
 */
public final class LowLatencyGroups {
    /** Sentinel for "no low latency". No backends belong to this group. */
    public static final BackendGroup NONE = BackendGroup.of(
            "superresolution:none",
            Component.translatable("superresolution.algorithm.low_latency.none")
    );

    /** NVIDIA Reflex. Both the Streamline and VK_NV_low_latency2 backends belong here. */
    public static final BackendGroup NV_REFLEX = BackendGroup.of(
            "superresolution:nv_reflex",
            Component.translatable("superresolution.algorithm.low_latency.nv_reflex")
    );

    private LowLatencyGroups() {
    }
}
