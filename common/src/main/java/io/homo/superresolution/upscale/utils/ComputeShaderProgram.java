package io.homo.superresolution.upscale.utils;

import com.mojang.blaze3d.systems.RenderSystem;
import io.homo.superresolution.SuperResolution;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static io.homo.superresolution.render.gl.GlConst.*;
import static io.homo.superresolution.render.gl.Gl.*;

/**
 * PS:这是计算着色器
 */
public class ComputeShaderProgram {
    public String shaderName;
    public int shaderProgram;
    private ArrayList<String> shaderTextList;
    private Map<String, ShaderInclude> shaderIncludeList;
    private ArrayList<String> shaderDefineList;

    private ComputeShaderProgram() {
    }

    public static builder create() {
        return new builder();
    }

    protected ArrayList<String> processShaderText() {
        ArrayList<String> srcTextList = new ArrayList<>(this.shaderTextList);
        ArrayList<String> processTextList = new ArrayList<>();
        int index = 0;
        while (true) {
            String srcText = null;

            if (srcTextList.size() == index) break;
            srcText = srcTextList.get(index);
            String text = srcText;
            index++;
            try {
                if (text.startsWith("//") && text.endsWith("//")) { //开头结尾都有两个//
                    text = text.substring(2, text.length() - 2);
                    if (text.startsWith("--") && text.endsWith("--")) { //开头结尾都有两个--
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

    public String getShaderText() {
        return String.join("\n", getShaderTextList());
    }

    public ArrayList<String> getShaderTextList() {
        return processShaderText();
    }

    public ComputeShaderProgram compileShader() {
        RenderSystem.assertOnRenderThread();
        int FRAGMENT_SHADER = glCreateShader(GL_COMPUTE_SHADER);
        glShaderSource(FRAGMENT_SHADER, getShaderText());
        glCompileShader(FRAGMENT_SHADER);
        if (glGetShaderi(FRAGMENT_SHADER, GL_COMPILE_STATUS) == 0) {
            try (PrintWriter out = new PrintWriter(new FileOutputStream("ERROR_FRAGMENT_SHADER_SRC.glsl"))) {
                out.println(this.getShaderText());
            } catch (IOException e) {
                SuperResolution.LOGGER.error(e.toString());
            }
            throw new RuntimeException("FRAGMENT_SHADER " + this.shaderName + " 无法编译着色器：" + glGetShaderInfoLog(FRAGMENT_SHADER, 32768));
        }

        this.shaderProgram = glCreateProgram();
        glAttachShader(shaderProgram, FRAGMENT_SHADER);
        glLinkProgram(shaderProgram);
        glDeleteShader(FRAGMENT_SHADER);
        return this;
    }

    public void use() {
        RenderSystem.assertOnRenderThread();
        glUseProgram(this.shaderProgram);
    }

    public void clear() {
        RenderSystem.assertOnRenderThread();
        glUseProgram(0);
    }

    public static class ShaderInclude {
        public String name;
        public ArrayList<String> textList;

        private ShaderInclude() {
        }

        public static ShaderInclude create(Collection<String> textList, String name) {
            ShaderInclude i = new ShaderInclude();
            i.textList = new ArrayList<>();
            i.textList.addAll(textList);
            i.name = name;
            return i;
        }
    }

    public static class builder {
        private final ArrayList<String> shaderTextList = new ArrayList<>();
        private final Map<String, ShaderInclude> shaderIncludeList = new HashMap<>();
        private final ArrayList<String> shaderDefineList = new ArrayList<>();
        private String shaderName = "";

        public builder() {
        }

        public builder addShaderInclude(ShaderInclude include) {
            this.shaderIncludeList.put(include.name, include);
            return this;
        }

        public builder setShaderName(String shaderName) {
            this.shaderName = shaderName;
            return this;
        }

        public builder addShaderText(String text) {
            this.shaderTextList.add(text);
            return this;
        }

        public builder addDefineText(String name, String value) {
            this.shaderDefineList.add("#define %s %s".formatted(name, value));
            return this;
        }

        public builder addShaderText(String text, int index) {
            this.shaderTextList.add(index, text);
            return this;
        }

        public builder addAllShaderTextList(Collection<String> text, int index) {
            this.shaderTextList.addAll(index, text);
            return this;
        }

        public builder addAllShaderTextList(Collection<String> text) {
            this.shaderTextList.addAll(text);
            return this;
        }

        public ComputeShaderProgram build() {
            ComputeShaderProgram shader = new ComputeShaderProgram();
            shader.shaderIncludeList = this.shaderIncludeList;
            shader.shaderTextList = this.shaderTextList;
            shader.shaderDefineList = this.shaderDefineList;
            shader.shaderName = this.shaderName;
            return shader;
        }
    }
}
