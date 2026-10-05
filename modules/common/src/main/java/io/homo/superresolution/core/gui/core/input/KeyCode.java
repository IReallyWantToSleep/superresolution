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

package io.homo.superresolution.core.gui.core.input;

import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

public enum KeyCode {
    UNKNOWN(GLFW.GLFW_KEY_UNKNOWN),
    SPACE(GLFW.GLFW_KEY_SPACE),
    APOSTROPHE(GLFW.GLFW_KEY_APOSTROPHE),
    COMMA(GLFW.GLFW_KEY_COMMA),
    MINUS(GLFW.GLFW_KEY_MINUS),
    PERIOD(GLFW.GLFW_KEY_PERIOD),
    SLASH(GLFW.GLFW_KEY_SLASH),
    DIGIT_0(GLFW.GLFW_KEY_0),
    DIGIT_1(GLFW.GLFW_KEY_1),
    DIGIT_2(GLFW.GLFW_KEY_2),
    DIGIT_3(GLFW.GLFW_KEY_3),
    DIGIT_4(GLFW.GLFW_KEY_4),
    DIGIT_5(GLFW.GLFW_KEY_5),
    DIGIT_6(GLFW.GLFW_KEY_6),
    DIGIT_7(GLFW.GLFW_KEY_7),
    DIGIT_8(GLFW.GLFW_KEY_8),
    DIGIT_9(GLFW.GLFW_KEY_9),
    SEMICOLON(GLFW.GLFW_KEY_SEMICOLON),
    EQUAL(GLFW.GLFW_KEY_EQUAL),
    A(GLFW.GLFW_KEY_A),
    B(GLFW.GLFW_KEY_B),
    C(GLFW.GLFW_KEY_C),
    D(GLFW.GLFW_KEY_D),
    E(GLFW.GLFW_KEY_E),
    F(GLFW.GLFW_KEY_F),
    G(GLFW.GLFW_KEY_G),
    H(GLFW.GLFW_KEY_H),
    I(GLFW.GLFW_KEY_I),
    J(GLFW.GLFW_KEY_J),
    K(GLFW.GLFW_KEY_K),
    L(GLFW.GLFW_KEY_L),
    M(GLFW.GLFW_KEY_M),
    N(GLFW.GLFW_KEY_N),
    O(GLFW.GLFW_KEY_O),
    P(GLFW.GLFW_KEY_P),
    Q(GLFW.GLFW_KEY_Q),
    R(GLFW.GLFW_KEY_R),
    S(GLFW.GLFW_KEY_S),
    T(GLFW.GLFW_KEY_T),
    U(GLFW.GLFW_KEY_U),
    V(GLFW.GLFW_KEY_V),
    W(GLFW.GLFW_KEY_W),
    X(GLFW.GLFW_KEY_X),
    Y(GLFW.GLFW_KEY_Y),
    Z(GLFW.GLFW_KEY_Z),
    LEFT_BRACKET(GLFW.GLFW_KEY_LEFT_BRACKET),
    BACKSLASH(GLFW.GLFW_KEY_BACKSLASH),
    RIGHT_BRACKET(GLFW.GLFW_KEY_RIGHT_BRACKET),
    GRAVE_ACCENT(GLFW.GLFW_KEY_GRAVE_ACCENT),
    WORLD_1(GLFW.GLFW_KEY_WORLD_1),
    WORLD_2(GLFW.GLFW_KEY_WORLD_2),
    ENTER(GLFW.GLFW_KEY_ENTER),
    ESCAPE(GLFW.GLFW_KEY_ESCAPE),
    TAB(GLFW.GLFW_KEY_TAB),
    BACKSPACE(GLFW.GLFW_KEY_BACKSPACE),
    INSERT(GLFW.GLFW_KEY_INSERT),
    DELETE(GLFW.GLFW_KEY_DELETE),
    RIGHT(GLFW.GLFW_KEY_RIGHT),
    LEFT(GLFW.GLFW_KEY_LEFT),
    DOWN(GLFW.GLFW_KEY_DOWN),
    UP(GLFW.GLFW_KEY_UP),
    PAGE_UP(GLFW.GLFW_KEY_PAGE_UP),
    PAGE_DOWN(GLFW.GLFW_KEY_PAGE_DOWN),
    HOME(GLFW.GLFW_KEY_HOME),
    END(GLFW.GLFW_KEY_END),
    CAPS_LOCK(GLFW.GLFW_KEY_CAPS_LOCK),
    SCROLL_LOCK(GLFW.GLFW_KEY_SCROLL_LOCK),
    NUM_LOCK(GLFW.GLFW_KEY_NUM_LOCK),
    PRINT_SCREEN(GLFW.GLFW_KEY_PRINT_SCREEN),
    PAUSE(GLFW.GLFW_KEY_PAUSE),
    F1(GLFW.GLFW_KEY_F1),
    F2(GLFW.GLFW_KEY_F2),
    F3(GLFW.GLFW_KEY_F3),
    F4(GLFW.GLFW_KEY_F4),
    F5(GLFW.GLFW_KEY_F5),
    F6(GLFW.GLFW_KEY_F6),
    F7(GLFW.GLFW_KEY_F7),
    F8(GLFW.GLFW_KEY_F8),
    F9(GLFW.GLFW_KEY_F9),
    F10(GLFW.GLFW_KEY_F10),
    F11(GLFW.GLFW_KEY_F11),
    F12(GLFW.GLFW_KEY_F12),
    F13(GLFW.GLFW_KEY_F13),
    F14(GLFW.GLFW_KEY_F14),
    F15(GLFW.GLFW_KEY_F15),
    F16(GLFW.GLFW_KEY_F16),
    F17(GLFW.GLFW_KEY_F17),
    F18(GLFW.GLFW_KEY_F18),
    F19(GLFW.GLFW_KEY_F19),
    F20(GLFW.GLFW_KEY_F20),
    F21(GLFW.GLFW_KEY_F21),
    F22(GLFW.GLFW_KEY_F22),
    F23(GLFW.GLFW_KEY_F23),
    F24(GLFW.GLFW_KEY_F24),
    F25(GLFW.GLFW_KEY_F25),
    KP_0(GLFW.GLFW_KEY_KP_0),
    KP_1(GLFW.GLFW_KEY_KP_1),
    KP_2(GLFW.GLFW_KEY_KP_2),
    KP_3(GLFW.GLFW_KEY_KP_3),
    KP_4(GLFW.GLFW_KEY_KP_4),
    KP_5(GLFW.GLFW_KEY_KP_5),
    KP_6(GLFW.GLFW_KEY_KP_6),
    KP_7(GLFW.GLFW_KEY_KP_7),
    KP_8(GLFW.GLFW_KEY_KP_8),
    KP_9(GLFW.GLFW_KEY_KP_9),
    KP_DECIMAL(GLFW.GLFW_KEY_KP_DECIMAL),
    KP_DIVIDE(GLFW.GLFW_KEY_KP_DIVIDE),
    KP_MULTIPLY(GLFW.GLFW_KEY_KP_MULTIPLY),
    KP_SUBTRACT(GLFW.GLFW_KEY_KP_SUBTRACT),
    KP_ADD(GLFW.GLFW_KEY_KP_ADD),
    KP_ENTER(GLFW.GLFW_KEY_KP_ENTER),
    KP_EQUAL(GLFW.GLFW_KEY_KP_EQUAL),
    LEFT_SHIFT(GLFW.GLFW_KEY_LEFT_SHIFT),
    LEFT_CONTROL(GLFW.GLFW_KEY_LEFT_CONTROL),
    LEFT_ALT(GLFW.GLFW_KEY_LEFT_ALT),
    LEFT_SUPER(GLFW.GLFW_KEY_LEFT_SUPER),
    RIGHT_SHIFT(GLFW.GLFW_KEY_RIGHT_SHIFT),
    RIGHT_CONTROL(GLFW.GLFW_KEY_RIGHT_CONTROL),
    RIGHT_ALT(GLFW.GLFW_KEY_RIGHT_ALT),
    RIGHT_SUPER(GLFW.GLFW_KEY_RIGHT_SUPER),
    MENU(GLFW.GLFW_KEY_MENU);

    private static final Map<Integer, KeyCode> BY_CODE = new HashMap<>();

    static {
        for (KeyCode keyCode : values()) {
            BY_CODE.put(keyCode.code, keyCode);
        }
    }

    private final int code;

    KeyCode(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static KeyCode fromCode(int code) {
        return BY_CODE.getOrDefault(code, UNKNOWN);
    }
}
