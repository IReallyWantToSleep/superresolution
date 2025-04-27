package io.homo.superresolution.common.render.gl;

import io.homo.superresolution.common.render.GraphicsCapabilities;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.opengl.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static io.homo.superresolution.common.render.gl.GlConst.*;

public class Gl {
    public static int[] getVersion() {
        return new int[]{glGetInteger(GL_MAJOR_VERSION), glGetInteger(GL_MINOR_VERSION)};
    }

    public static void glBindSampler(int unit, int sampler) {
        GL33C.glBindSampler(unit, sampler);
    }

    public static void glUniform4fv(int location, FloatBuffer value) {
        GL20C.glUniform4fv(location, value);
    }

    public static void glUniform3ui(int location, int v0, int v1, int v2) {
        GL30C.glUniform3ui(location, v0, v1, v2);
    }

    public static void glUniform2ui(int location, int v0, int v1) {
        GL30C.glUniform2ui(location, v0, v1);
    }

    public static void glBindVertexArray(int array) {
        GL30C.glBindVertexArray(array);
    }

    public static void glDeleteVertexArrays(int array) {
        GL30C.glDeleteVertexArrays(array);
    }

    public static int glGenVertexArrays() {
        return GL30C.glGenVertexArrays();
    }

    public static int glGenBuffers() {
        return GL15C.glGenBuffers();
    }

    public static void glDeleteBuffers(int buffer) {
        GL15C.glDeleteBuffers(buffer);
    }

    public static void glBufferData(int target, FloatBuffer data, int usage) {
        GL15C.glBufferData(target, data, usage);
    }

    public static void glBufferData(int target, IntBuffer data, int usage) {
        GL15C.glBufferData(target, data, usage);
    }

    public static void glBindBuffer(int target, int buffer) {
        GL15C.glBindBuffer(target, buffer);
    }

    public static void glSamplerParameteri(int sampler, int pname, int param) {
        GL33C.glSamplerParameteri(sampler, pname, param);
    }

    public static int glGenSamplers() {
        return GL33C.glGenSamplers();
    }

    public static void glTexParameteri(int target, int pname, int param) {
        GL11C.glTexParameteri(target, pname, param);
    }

    public static void glViewport(int x, int y, int w, int h) {
        GL11C.glViewport(x, y, w, h);
    }

    public static int glCheckFramebufferStatus(int target) {
        return GL30C.glCheckFramebufferStatus(target);
    }


    public static void glTexImage2D(int target, int level, int internalformat, int width, int height, int border, int format, int type, IntBuffer pixels) {
        GL11C.glTexImage2D(target, level, internalformat, width, height, border, format, type, pixels);
    }

    public static void glTexImage2D(int target, int level, int internalformat, int width, int height, int border, int format, int type, ByteBuffer pixels) {
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

    public static void glEnableVertexAttribArray(int index) {
        GL20C.glEnableVertexAttribArray(index);
    }

    public static void glVertexAttribPointer(int index, int size, int type, boolean normalized, int stride, long pointer) {
        GL20C.glVertexAttribPointer(index, size, type, normalized, stride, pointer);
    }

    public static void glDrawArrays(int mode, int first, int count) {
        GL11C.glDrawArrays(mode, first, count);
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

    public static void glDeleteProgram(int program) {
        GL20C.glDeleteProgram(program);
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

    public static void glClearColor(float r, float g, float b, float alpha) {
        GL20C.glClearColor(r, g, b, alpha);
    }

    public static void glUniform3f(int location, float v0, float v1, float v2) {
        GL20C.glUniform3f(location, v0, v1, v2);
    }

    public static int glGetMaxTextureSize() {
        return glGetInteger(GL_MAX_TEXTURE_SIZE);
    }

    public static int glGenFramebuffers() {
        return GL30C.glGenFramebuffers();
    }

    public static void glDeleteFramebuffers(int framebuffer) {
        GL30C.glDeleteFramebuffers(framebuffer);
    }

    public static void glCopyImageSubData(int srcName, int srcTarget, int srcLevel, int srcX, int srcY, int srcZ, int dstName, int dstTarget, int dstLevel, int dstX, int dstY, int dstZ, int srcWidth, int srcHeight, int srcDepth) {
        GL43C.glCopyImageSubData(srcName, srcTarget, srcLevel, srcX, srcY, srcZ, dstName, dstTarget, dstLevel, dstX, dstY, dstZ, srcWidth, srcHeight, srcDepth);
    }

    public static void glUniform1i(int location, int x) {
        GL20.glUniform1i(location, x);
    }

    public static void glTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, int type, ByteBuffer pixels) {
        GL11C.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, pixels);
    }

    public static void glPixelStorei(int pname, int param) {
        GL11C.glPixelStorei(pname, param);
    }

    public static void glSafeObjectLabel(int type, int id, String label) {
        if (GraphicsCapabilities.hasGLExtension("GL_KHR_debug")) {
            KHRDebug.glObjectLabel(type, id, StringUtils.abbreviate(label, 255));
        }
    }
}
