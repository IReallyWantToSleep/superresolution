package io.homo.superresolution.common.config.enums;

import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public enum CaptureMode {
    A(Component.literal("A")), //gameRenderer + noHand
    B(Component.literal("B")), //levelRenderer + noHand
    C(Component.literal("C")); //gameRenderer + hand
    public static final Map<String, CaptureMode> TEXT_MAP = new HashMap<>();

    static {
        CaptureMode.TEXT_MAP.put("a", A);
        CaptureMode.TEXT_MAP.put("b", B);
        CaptureMode.TEXT_MAP.put("c", C);


    }

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

    public static CaptureMode fromString(String string) {
        return TEXT_MAP.get(string.toLowerCase());
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
