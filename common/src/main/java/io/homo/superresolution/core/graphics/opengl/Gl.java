package io.homo.superresolution.core.graphics.opengl;

import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.core.graphics.GraphicsCapabilities;
import io.homo.superresolution.core.graphics.opengl.dsa.CompatDirectStateAccessImpl;
import io.homo.superresolution.core.graphics.opengl.dsa.GL45OrEXTDirectStateAccessImpl;
import io.homo.superresolution.core.graphics.opengl.dsa.IGlDirectStateAccess;
import org.lwjgl.opengl.GL11C;

public class Gl {
    public static final IGlDirectStateAccess DSA;

    static {
        if (!isSupportDSA()) {
            SuperResolution.LOGGER.info("不支持DSA 使用 CompatDirectStateAccessImpl");
            DSA = new CompatDirectStateAccessImpl();
        } else {
            SuperResolution.LOGGER.info("支持DSA 使用 GL45OrEXTDirectStateAccessImpl");
            DSA = new GL45OrEXTDirectStateAccessImpl();
        }
    }

    public static boolean isLegacy() {
        return GraphicsCapabilities.getGLVersion()[0] >= 4 && GraphicsCapabilities.getGLVersion()[1] < 3;
    }

    public static boolean isSupportDSA() {
        return GraphicsCapabilities.getGLVersion()[0] >= 4 && GraphicsCapabilities.getGLVersion()[1] >= 5;
    }

    private static int glGetInteger(int pname) {
        return GL11C.glGetInteger(pname);
    }

}
