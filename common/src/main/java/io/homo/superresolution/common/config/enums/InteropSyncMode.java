package io.homo.superresolution.common.config.enums;

import net.minecraft.network.chat.Component;

public enum InteropSyncMode {
    LowLatency(Component.translatable("superresolution.screen.config.options.label.interop_sync_mode.low_latency")),
    HighPerformance(Component.translatable("superresolution.screen.config.options.label.interop_sync_mode.high_performance"));

    public Component getComponent() {
        return component;
    }

    private final Component component;

    InteropSyncMode(Component component) {
        this.component = component;
    }

    @Override
    public String toString() {
        return component.getString();
    }
}
