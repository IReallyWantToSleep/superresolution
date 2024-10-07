package io.homo.superresolution.render.gl;

import org.lwjgl.opengl.*;

import java.nio.IntBuffer;

public class Gl {
    public static void glTexParameteri(int target, int pname, int param) {
        GL11C.glTexParameteri(target, pname, param);
    }

    public static void glTexImage2D(int target, int level, int internalformat, int width, int height, int border, int format, int type, IntBuffer pixels) {
        GL11C.glTexImage2D(target, level, internalformat, width, height, border, format, type, pixels);
    }

    public static void glBindFramebuffer(int target, int framebuffer) {
        GL30C.glBindFramebuffer(target, framebuffer);
    }

    public static void glFramebufferTexture2D(int target, int attachment, int textarget, int texture, int level) {
        GL30C.glFramebufferTexture2D(target, attachment, textarget, texture, level);
    }

    public static void glBindTexture(int target, int texture) {
        GL11C.glBindTexture(target, texture);
    }

    public static void glDeleteTextures(int texture) {
        GL11C.glDeleteTextures(texture);
    }

    public static void glCopyTexSubImage2D(int target, int level, int xoffset, int yoffset, int x, int y, int width, int height) {
        GL11C.glCopyTexSubImage2D(target, level, xoffset, yoffset, x, y, width, height);
    }

    public static void glActiveTexture(int texture) {
        GL13C.glActiveTexture(texture);
    }

    public static void glTexStorage2D(int target, int levels, int internalformat, int width, int height) {
        GL42C.glTexStorage2D(target, levels, internalformat, width, height);
    }

    public static void glGenerateMipmap(int target) {
        GL30C.glGenerateMipmap(target);
    }

    public static int glGetUniformLocation(int program, CharSequence name) {
        return GL20C.glGetUniformLocation(program, name);
    }

    public static void glUniform1f(int location, float v0) {
        GL20C.glUniform1f(location, v0);
    }

    public static void glUniform2f(int location, float v0, float v1) {
        GL20C.glUniform2f(location, v0, v1);
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

    public static void glMemoryBarrier(int barriers) {
        GL42C.glMemoryBarrier(barriers);
    }

    public static void glGetIntegerv(int pname, int[] params) {
        GL11C.glGetIntegerv(pname, params);
    }

    public static void glUniformMatrix4fv(int location, boolean transpose, float[] value) {
        GL20C.glUniformMatrix4fv(location, transpose, value);
    }

    public static int glGetInteger(int pname) {
        return GL11C.glGetInteger(pname);
    }

    public static String glGetStringi(int name, int index) {
        return GL30C.glGetStringi(name, index);
    }

    public static void glShaderSource(int shader, CharSequence string) {
        GL20C.glShaderSource(shader, string);
    }

    public static int glCreateShader(int type) {
        return GL20C.glCreateShader(type);
    }

    public static void glCompileShader(int shader) {
        GL20C.glCompileShader(shader);
    }

    public static int glGetShaderi(int shader, int pname) {
        return GL20C.glGetShaderi(shader, pname);
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

    public static String glGetShaderInfoLog(int shader, int maxLength) {
        return GL20C.glGetShaderInfoLog(shader, maxLength);
    }
}
