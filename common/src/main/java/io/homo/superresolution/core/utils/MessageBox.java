package io.homo.superresolution.core.utils;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.win32.W32APIOptions;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.awt.*;
import java.io.IOException;
import java.net.URI;

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
