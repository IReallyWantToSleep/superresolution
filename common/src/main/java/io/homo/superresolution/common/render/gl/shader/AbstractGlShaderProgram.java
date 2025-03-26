package io.homo.superresolution.common.render.gl.shader;

import com.mojang.blaze3d.systems.RenderSystem;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.Config;
import io.homo.superresolution.common.impl.Destroyable;
import io.homo.superresolution.common.render.gl.buffer.GlUniformBuffer;
import io.homo.superresolution.common.utils.ShaderCache;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL41;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static io.homo.superresolution.common.render.gl.Gl.*;

public abstract class AbstractGlShaderProgram implements Destroyable {

    private final Map<String, Integer> uniformLocationCache = new HashMap<>();
    public String shaderName;
    public int shaderProgram;
    public boolean compiled;
    protected ArrayList<String> fragShaderTextList;
    protected ArrayList<String> vertShaderTextList;
    protected Map<String, GlGeneralShaderProgram.ShaderInclude> shaderIncludeList;
    protected ArrayList<String> shaderDefineList;
    protected boolean enableCache = false;

    protected AbstractGlShaderProgram() {
    }

    public ArrayList<String> getShaderDefineList() {
        return shaderDefineList;
    }

    public Map<String, GlGeneralShaderProgram.ShaderInclude> getShaderIncludeList() {
        return shaderIncludeList;
    }

    protected AbstractGlShaderProgram fromBin(ShaderCache.ShaderBinary shaderBin) {
        if (compiled) return this;
        int program = GL20.glCreateProgram();
        GL41.glProgramBinary(program, shaderBin.format(), shaderBin.binary());

        GL20.glValidateProgram(program);
        if (GL20.glGetProgrami(program, GL20.GL_VALIDATE_STATUS) == GL20.GL_FALSE) {
            String log = GL20.glGetProgramInfoLog(program);
            GL20.glDeleteProgram(program);
            throw new RuntimeException("Program validation failed:\n" + log);
        }

        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL20.GL_FALSE) {
            String log = GL20.glGetProgramInfoLog(program);
            GL20.glDeleteProgram(program);
            throw new RuntimeException("Program link status invalid:\n" + log);
        }
        this.shaderProgram = program;
        this.compiled = true;
        return this;
    }

    protected void checkProgram() {
        if (GL20.glGetProgrami(shaderProgram, GL20.GL_VALIDATE_STATUS) == GL20.GL_FALSE) {
            String log = GL20.glGetProgramInfoLog(shaderProgram);
            GL20.glDeleteProgram(shaderProgram);
            throw new RuntimeException("Program validation failed:\n" + log);
        }

        if (GL20.glGetProgrami(shaderProgram, GL20.GL_LINK_STATUS) == GL20.GL_FALSE) {
            String log = GL20.glGetProgramInfoLog(shaderProgram);
            GL20.glDeleteProgram(shaderProgram);
            throw new RuntimeException("Program link status invalid:\n" + log);
        }
    }

    @Override
    public void destroy() {
        glDeleteProgram(shaderProgram);
        fragShaderTextList.clear();
        vertShaderTextList.clear();
        shaderIncludeList.clear();
        shaderDefineList.clear();
    }

    protected ArrayList<String> processShaderText(ArrayList<String> src) {
        ArrayList<String> srcTextList = new ArrayList<>(src);
        ArrayList<String> processTextList = new ArrayList<>();
        int index = 0;
        while (true) {
            String srcText = null;

            if (srcTextList.size() == index) break;
            srcText = srcTextList.get(index);
            String text = srcText.trim();
            index++;
            try {
                if (text.startsWith("//") && text.endsWith("//")) { //开头结尾都有//
                    text = text.substring(2, text.length() - 2);
                    if (text.startsWith("--") && text.endsWith("--")) { //开头结尾都有--
                        text = text.substring(2, text.length() - 2);
                        String[] textList = text.split("--");
                        if (textList.length == 2) {
                            switch (textList[0]) {
                                case "insert" -> {
                                    processTextList.addAll(this.shaderDefineList);
                                }
                                case "include" -> {
                                    if (this.shaderIncludeList.get(textList[1]) != null) {
                                        processTextList.addAll(this.shaderIncludeList.get(textList[1]).textList);
                                    } else {
                                        SuperResolution.LOGGER.warn("着色器编译时缺少源文件 {}", textList[1]);
                                    }
                                }
                            }
                            continue;
                        }
                    }
                }
            } catch (Exception e) {
                processTextList.add(srcText);
                continue;
            }
            processTextList.add(srcText);

        }

        return processTextList;
    }

    public String getFragShaderText() {
        String code = String.join("\n", getFragShaderTextList());
        if (Config.getInstance().isDebugDumpShader()) {
            try (PrintWriter out = new PrintWriter(new FileOutputStream(
                    Path.of(
                            ShaderCache.CACHE_DIR.toString(),
                            "%s+FragShader.frag.glsl".formatted(shaderName)
                    ).toString()
            ))) {
                out.println(code);
            } catch (IOException e) {
                SuperResolution.LOGGER.error(e.toString());
            }
        }
        return code;
    }

    public String getVertShaderText() {
        String code = String.join("\n", getVertShaderTextList());
        if (Config.getInstance().isDebugDumpShader()) {
            try (PrintWriter out = new PrintWriter(new FileOutputStream(
                    Path.of(
                            ShaderCache.CACHE_DIR.toString(),
                            "%s+VertShader.vert.glsl".formatted(shaderName)
                    ).toString()
            ))) {
                out.println(code);
            } catch (IOException e) {
                SuperResolution.LOGGER.error(e.toString());
            }
        }
        return code;
    }

    public ArrayList<String> getFragShaderTextList() {
        return processShaderText(this.fragShaderTextList);
    }

    public ArrayList<String> getVertShaderTextList() {
        return processShaderText(this.vertShaderTextList);
    }

    public abstract AbstractGlShaderProgram compileShader();

    public void use() {
        RenderSystem.assertOnRenderThread();
        glUseProgram(this.shaderProgram);
    }

    public void clear() {
        RenderSystem.assertOnRenderThread();
        glUseProgram(0);
    }

    public int getUniformLocation(String name) {
        if (uniformLocationCache.containsKey(name)) return uniformLocationCache.get(name);
        int i = glGetUniformLocation(this.shaderProgram, name);
        uniformLocationCache.put(name, i);
        return i;
    }

    public void setVec2(String name, float x, float y) {
        glUniform2f(getUniformLocation(name), x, y);
    }

    public void setVec3(String name, float x, float y, float z) {
        glUniform3f(getUniformLocation(name), x, y, z);
    }

    public void setFloat(String name, float value) {
        glUniform1f(getUniformLocation(name), value);
    }

    public void setInt(String name, int value) {
        glUniform1i(getUniformLocation(name), value);
    }

    public void setStruct(String name, GlUniformBuffer<?> value, int bindingPoint) {
        value.bind(bindingPoint);
    }


    public void setMatrix4(String name, Matrix4f x) {
        float[] data = new float[16];
        x.get(data);
        glUniformMatrix4fv(getUniformLocation(name), false, data);
    }
}
