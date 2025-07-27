package io.homo.superresolution.core.graphics.opengl;

import io.homo.superresolution.core.graphics.GraphicsCapabilities;
import io.homo.superresolution.core.graphics.opengl.dsa.GL41DirectStateAccessImpl;
import io.homo.superresolution.core.graphics.opengl.dsa.GL45OrEXTDirectStateAccessImpl;
import io.homo.superresolution.core.graphics.opengl.dsa.IGlDirectStateAccess;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.KHRDebug;

import static io.homo.superresolution.core.graphics.opengl.GlConst.GL_MAJOR_VERSION;
import static io.homo.superresolution.core.graphics.opengl.GlConst.GL_MINOR_VERSION;

public class Gl {
    public static final IGlDirectStateAccess DSA;

    static {
        if (!isSupportDSA()) {
            DSA = new GL41DirectStateAccessImpl();
        } else {
            DSA = new GL45OrEXTDirectStateAccessImpl();
        }
    }

    public static int[] getVersion() {
        return new int[]{glGetInteger(GL_MAJOR_VERSION), glGetInteger(GL_MINOR_VERSION)};
    }

    public static boolean isLegacy() {
        return GraphicsCapabilities.getGLVersion()[0] >= 4 && GraphicsCapabilities.getGLVersion()[1] < 3;
    }

    public static boolean isSupportDSA() {
        return (GraphicsCapabilities.getGLVersion()[0] >= 4 && GraphicsCapabilities.getGLVersion()[1] >= 5) || GraphicsCapabilities.hasGLExtension("GL_EXT_direct_state_access");
    }

    private static int glGetInteger(int pname) {
        return GL11C.glGetInteger(pname);
    }

    public static void setGlObjectLabel(int type, int id, String label) {
        if (GraphicsCapabilities.hasGLExtension("GL_KHR_debug")) {
            KHRDebug.glObjectLabel(type, id, StringUtils.abbreviate(label, 255));
        }
    }
}
