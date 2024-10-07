package io.homo.superresolution.upscale;

import io.homo.superresolution.SuperResolution;
import org.lwjgl.glfw.GLFW;

public class CPPHelper {
    public static void CPP_Log(String msg,int level){
        switch (level){
            case 0 -> SuperResolution.LOGGER_CPP.info(msg);
            case 1 -> SuperResolution.LOGGER_CPP.warn(msg);
            case 2 -> SuperResolution.LOGGER_CPP.error(msg);
            case 3 -> SuperResolution.LOGGER_CPP.debug(msg);
        }
    }

    public static long CPP_glfwGetProcAddress(String name) {
        return GLFW.glfwGetProcAddress(name);
    }

}
