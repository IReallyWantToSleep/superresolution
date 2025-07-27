package io.homo.superresolution.core.utils;

import org.lwjgl.util.tinyfd.TinyFileDialogs;

public class MessageBox {
    private static void createMsgBox(String text, String caption, String type) {
        TinyFileDialogs.tinyfd_messageBox(
                caption,
                text,
                "ok",
                type,
                true
        );
    }

    public static void createError(String text, String caption) {
        createMsgBox(text, caption, "error");
    }

    public static void createWarn(String text, String caption) {
        createMsgBox(text, caption, "warning");
    }

    public static void createInfo(String text, String caption) {
        createMsgBox(text, caption, "info");
    }

    public static void main(String[] args) {
        createError("114514", "114514");
        createWarn("114514", "114514");
        createInfo("114514", "114514");
    }
}
