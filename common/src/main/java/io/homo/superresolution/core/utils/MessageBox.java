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

package io.homo.superresolution.core.utils;

import javax.swing.*;

public class MessageBox {
    private static void createMsgBox(String text, String caption, int messageType) {
        JOptionPane.showMessageDialog(
                null,
                text,
                caption,
                messageType
        );
    }

    public static void createError(String text, String caption) {
        createMsgBox(text, caption, JOptionPane.ERROR_MESSAGE);
    }

    public static void createWarn(String text, String caption) {
        createMsgBox(text, caption, JOptionPane.WARNING_MESSAGE);
    }

    public static void createInfo(String text, String caption) {
        createMsgBox(text, caption, JOptionPane.INFORMATION_MESSAGE);
    }

    public static void main(String[] args) {
        createError("114514", "114514");
        createWarn("114514", "114514");
        createInfo("114514", "114514");
    }
}
