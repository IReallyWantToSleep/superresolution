package io.homo.superresolution.common.render.gl.shader;

import com.mojang.blaze3d.systems.RenderSystem;
import io.homo.superresolution.common.SuperResolution;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Map;

import static io.homo.superresolution.common.render.gl.Gl.*;

public abstract class AbstractShaderProgram {

    public String shaderName;
    public int shaderProgram;
    protected ArrayList<String> fragShaderTextList;
    protected ArrayList<String> vertShaderTextList;
    protected Map<String, GeneralShaderProgram.ShaderInclude> shaderIncludeList;
    protected ArrayList<String> shaderDefineList;

    protected AbstractShaderProgram() {
    }

    protected ArrayList<String> processShaderText() {
        ArrayList<String> srcTextList = new ArrayList<>(this.fragShaderTextList);
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
        return String.join("\n", getFragShaderTextList());
    }

    public String getVertShaderText() {
        return String.join("\n", getVertShaderTextList());
    }

    public ArrayList<String> getFragShaderTextList() {
        return processShaderText();
    }

    public ArrayList<String> getVertShaderTextList() {
        return this.vertShaderTextList;
    }

    public abstract AbstractShaderProgram compileShader();

    public void use() {
        RenderSystem.assertOnRenderThread();
        glUseProgram(this.shaderProgram);
    }

    public void clear() {
        RenderSystem.assertOnRenderThread();
        glUseProgram(0);
    }

    public int getUniformLocation(String name) {
        return glGetUniformLocation(this.shaderProgram, name);
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


    public void setMatrix4(String name, Matrix4f x) {
        float[] data = new float[16];
        x.get(data);
        glUniformMatrix4fv(getUniformLocation(name), false, data);
    }
}
