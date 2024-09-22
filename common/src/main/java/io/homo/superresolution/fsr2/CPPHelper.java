package io.homo.superresolution.fsr2;

import io.homo.superresolution.SuperResolution;
import org.lwjgl.glfw.GLFW;

public class CPPHelper {
    public static void CPP_Log(String msg,int level){
        switch (level){
            case 0 -> SuperResolution.LOGGER.info(msg);
            case 1 -> SuperResolution.LOGGER.warn(msg);
            case 2 -> SuperResolution.LOGGER.error(msg);
            case 3 -> SuperResolution.LOGGER.debug(msg);
        }
    }

    public static long CPP_glfwGetProcAddress(String name) {
        SuperResolution.LOGGER.info(String.valueOf(GLFW.glfwGetProcAddress(name)));
        return GLFW.glfwGetProcAddress(name);
    }

}
