/*
 * Super Resolution
 * Copyright (c) 2025-2026. 187J3X1-114514
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

public enum DLSSRenderPreset {
    F(6, Component.literal("F")),
    J(10, Component.literal("J")),
    K(11, Component.literal("K")),
    L(12, Component.literal("L")),
    M(13, Component.literal("M"));

    private final int code;
    private final Component component;

    DLSSRenderPreset(int code, Component component) {
        this.code = code;
        this.component = component;
    }

    public int getCode() {
        return code;
    }

    public Component getComponent() {
        return component;
    }
}
