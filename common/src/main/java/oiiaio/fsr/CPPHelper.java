package oiiaio.fsr;

import io.homo.superresolution.common.SuperResolution;
import org.lwjgl.glfw.GLFW;
import static org.lwjgl.vulkan.VK10.vkGetDeviceProcAddr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CPPHelper {
    public static final Logger LOGGER_CPP = LoggerFactory.getLogger("oiiaio.fsr.cpp");
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
