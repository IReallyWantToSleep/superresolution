/*
 * Super Resolution
 * Copyright (c) 2025-2026. 187J3X1-114514
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
