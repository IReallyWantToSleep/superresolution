package io.homo.superresolution.core.utils;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.win32.W32APIOptions;

public class MessageBox {
    private static final boolean ON_WINDOWS = System.getProperty("os.name").toLowerCase().contains("windows");

    public static void createError(String text, String caption) {
        if (!ON_WINDOWS)
            return;
        User32.INSTANCE.MessageBoxW(0, text, caption, 0x00000010);
    }

    public static void createWarn(String text, String caption) {
        if (!ON_WINDOWS)
            return;
        User32.INSTANCE.MessageBoxW(0, text, caption, 0x00000030);
    }

    public static void createInfo(String text, String caption) {
        if (!ON_WINDOWS)
            return;
        User32.INSTANCE.MessageBoxW(0, text, caption, 0x00000040);
    }

    public static void main(String[] args) {
        createError("114514", "114514");
        createWarn("114514", "114514");
        createInfo("114514", "114514");
    }

    private interface User32 extends Library {
        User32 INSTANCE = Native.load("user32", User32.class, W32APIOptions.DEFAULT_OPTIONS);

        int MessageBoxW(int hWnd, String lpText, String lpCaption, int uType);
    }

}
