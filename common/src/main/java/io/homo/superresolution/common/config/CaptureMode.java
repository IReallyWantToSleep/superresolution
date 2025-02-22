package io.homo.superresolution.common.config;

import net.minecraft.network.chat.Component;

import java.util.Arrays;

public enum CaptureMode {
    /**
     * 兼容性好，但是会导致手部渲染效果一坨答辩
     */
    A(Component.literal("")),
    /**
     * 兼容性差（与DH和YSM工作时有问题），但是不会影响手部渲染（但只有不开光影生效 <-- 笑死）
     */
    B(Component.literal(""));
    private final Component tooltip;

    CaptureMode(Component tooltip) {
        this.tooltip = tooltip;
    }

    public static int getId(CaptureMode mode) {
        return Arrays.stream(CaptureMode.values()).toList().indexOf(mode);
    }

    public static CaptureMode getMode(int mode) {
        return Arrays.stream(CaptureMode.values()).toList().get(mode);
    }

    public Component get() {
        return tooltip;
    }

    @Override
    public String toString() {
        return tooltip.getString();
    }

    public String getString() {
        return this.toString();
    }
}
