package io.homo.superresolution.core.graphics.opengl;

import io.homo.superresolution.core.graphics.GraphicsCapabilities;
import io.homo.superresolution.core.graphics.opengl.dsa.GL41DirectStateAccessImpl;
import io.homo.superresolution.core.graphics.opengl.dsa.GL45OrEXTDirectStateAccessImpl;
import io.homo.superresolution.core.graphics.opengl.dsa.IGlDirectStateAccess;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.opengl.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static io.homo.superresolution.core.graphics.opengl.GlConst.*;

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

    public static void glBindSampler(int unit, int sampler) {
        GL33C.glBindSampler(unit, sampler);
    }

    public static void glViewport(int x, int y, int w, int h) {
        GL11C.glViewport(x, y, w, h);
    }

    public static void glBindFramebuffer(int target, int framebuffer) {
        GL30C.glBindFramebuffer(target, framebuffer);
    }

    public static void glBindTextureUnit(int unit, int texture) {
        GL45C.glBindTextureUnit(unit, texture);
    }

    public static void glBindImageTexture(int unit, int texture, int level, boolean layered, int layer, int access, int format) {
        GL42C.glBindImageTexture(unit, texture, level, layered, layer, access, format);
    }

    public static void glDispatchCompute(int num_groups_x, int num_groups_y, int num_groups_z) {
        GL43C.glDispatchCompute(num_groups_x, num_groups_y, num_groups_z);
    }

    public static void glDeleteProgram(int program) {
        GL20C.glDeleteProgram(program);
    }


    public static int glGetInteger(int pname) {
        return GL11C.glGetInteger(pname);
    }


    public static int glCreateShader(int type) {
        return GL20C.glCreateShader(type);
    }

    public static int glCreateProgram() {
        return GL20C.glCreateProgram();
    }

    public static void glAttachShader(int program, int shader) {
        GL20C.glAttachShader(program, shader);
    }

    public static void glLinkProgram(int program) {
        GL20C.glLinkProgram(program);
    }

    public static void glDeleteShader(int shader) {
        GL20C.glDeleteShader(shader);
    }

    public static void glUseProgram(int program) {
        GL20C.glUseProgram(program);
    }

    public static void glSafeObjectLabel(int type, int id, String label) {
        if (GraphicsCapabilities.hasGLExtension("GL_KHR_debug")) {
            KHRDebug.glObjectLabel(type, id, StringUtils.abbreviate(label, 255));
        }
    }
}
