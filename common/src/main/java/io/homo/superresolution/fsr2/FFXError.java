package io.homo.superresolution.fsr2;

import io.homo.superresolution.SuperResolution;
import org.lwjgl.glfw.GLFW;

public class FFXError {
    public static final int FFX_OK = 0;
    public static final int FFX_ERROR_INVALID_POINTER = 0x80000000;
    public static final int FFX_ERROR_INVALID_ALIGNMENT = 0x80000001;
    public static final int FFX_ERROR_INVALID_SIZE = 0x80000002;
    public static final int FFX_EOF = 0x80000003;
    public static final int FFX_ERROR_INVALID_PATH = 0x80000004;
    public static final int FFX_ERROR_EOF = 0x80000005;
    public static final int FFX_ERROR_MALFORMED_DATA = 0x80000006;
    public static final int FFX_ERROR_OUT_OF_MEMORY = 0x80000007;
    public static final int FFX_ERROR_INCOMPLETE_INTERFACE = 0x80000008;
    public static final int FFX_ERROR_INVALID_ENUM = 0x80000009;
    public static final int FFX_ERROR_INVALID_ARGUMENT = 0x8000000a;
    public static final int FFX_ERROR_OUT_OF_RANGE = 0x8000000b;
    public static final int FFX_ERROR_NULL_DEVICE = 0x8000000c;
    public static final int FFX_ERROR_BACKEND_API_ERROR = 0x8000000d;
    public static final int FFX_ERROR_INSUFFICIENT_MEMORY = 0x8000000e;

    public static String returnErrorText(int errorCode) {

            switch (errorCode){
                case FFX_OK ->{
                    return "FFX_OK";
                }
                case FFX_ERROR_INVALID_POINTER -> {
                    return "FFX_ERROR_INVALID_POINTER";
                }
                case FFX_ERROR_INVALID_ALIGNMENT -> {
                    return "FFX_ERROR_INVALID_ALIGNMENT";
                }
                case FFX_ERROR_INVALID_SIZE -> {
                    return "FFX_ERROR_INVALID_SIZE";
                }
                case FFX_EOF -> {
                    return "FFX_EOF";
                }
                case FFX_ERROR_INVALID_PATH -> {
                    return "FFX_ERROR_INVALID_PATH";
                }
                case FFX_ERROR_EOF -> {
                    return "FFX_ERROR_EOF";
                }
                case FFX_ERROR_MALFORMED_DATA -> {
                    return "FFX_ERROR_MALFORMED_DATA";
                }
                case FFX_ERROR_OUT_OF_MEMORY -> {
                    return "FFX_ERROR_OUT_OF_MEMORY";
                }
                case FFX_ERROR_INCOMPLETE_INTERFACE -> {
                    return "FFX_ERROR_INCOMPLETE_INTERFACE";
                }
                case FFX_ERROR_INVALID_ENUM -> {
                    return "FFX_ERROR_INVALID_ENUM";
                }
                case FFX_ERROR_INVALID_ARGUMENT -> {
                    return "FFX_ERROR_INVALID_ARGUMENT";
                }
                case FFX_ERROR_OUT_OF_RANGE -> {
                    return "FFX_ERROR_OUT_OF_RANGE";
                }
                case FFX_ERROR_NULL_DEVICE -> {
                    return "FFX_ERROR_NULL_DEVICE";
                }
                case FFX_ERROR_BACKEND_API_ERROR -> {
                    return "FFX_ERROR_BACKEND_API_ERROR";
                }
                case FFX_ERROR_INSUFFICIENT_MEMORY -> {
                    return "FFX_ERROR_INSUFFICIENT_MEMORY";
                }
            }
            return "WTF";
    }
}