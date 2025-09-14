/*
 * Super Resolution
 * Copyright (c) 2025. 187J3X1-114514
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public enum CaptureMode {
    A(Component.translatable("superresolution.capture_mode.a")), //gameRenderer + noHand
    B(Component.translatable("superresolution.capture_mode.b")), //levelRenderer + noHand
    C(Component.translatable("superresolution.capture_mode.c")); //gameRenderer + hand
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
