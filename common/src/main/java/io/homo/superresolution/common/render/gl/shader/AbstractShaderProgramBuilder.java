package io.homo.superresolution.common.render.gl.shader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractShaderProgramBuilder {
    private final ArrayList<String> fragShaderTextList = new ArrayList<>();
    private final ArrayList<String> vertShaderTextList = new ArrayList<>();
    private final Map<String, GlGeneralShaderProgram.ShaderInclude> shaderIncludeList = new HashMap<>();
    private final ArrayList<String> shaderDefineList = new ArrayList<>();
    private String shaderName = "";

    public AbstractShaderProgramBuilder() {
    }

    public AbstractShaderProgramBuilder addShaderInclude(GlGeneralShaderProgram.ShaderInclude include) {
        this.shaderIncludeList.put(include.name, include);
        return this;
    }

    public AbstractShaderProgramBuilder setShaderName(String shaderName) {
        this.shaderName = shaderName;
        return this;
    }

    public AbstractShaderProgramBuilder addFragShaderText(String text) {
        this.fragShaderTextList.add(text);
        return this;
    }

    public AbstractShaderProgramBuilder addDefineText(String name, String value) {
        this.shaderDefineList.add("#define %s %s".formatted(name, value));
        return this;
    }

    public AbstractShaderProgramBuilder addFragShaderText(String text, int index) {
        this.fragShaderTextList.add(index, text);
        return this;
    }

    public AbstractShaderProgramBuilder addAllFragShaderTextList(Collection<String> text, int index) {
        this.fragShaderTextList.addAll(index, text);
        return this;
    }

    public AbstractShaderProgramBuilder addAllFragShaderTextList(Collection<String> text) {
        this.fragShaderTextList.addAll(text);
        return this;
    }

    public AbstractShaderProgramBuilder addVertShaderText(String text, int index) {
        this.vertShaderTextList.add(index, text);
        return this;
    }

    public AbstractShaderProgramBuilder addAllVertShaderTextList(Collection<String> text, int index) {
        this.vertShaderTextList.addAll(index, text);
        return this;
    }

    public AbstractShaderProgramBuilder addAllVertShaderTextList(Collection<String> text) {
        this.vertShaderTextList.addAll(text);
        return this;
    }

    public abstract AbstractGlShaderProgram build();

    protected AbstractGlShaderProgram setShaderText(AbstractGlShaderProgram shader) {
        shader.shaderIncludeList = this.shaderIncludeList;
        shader.fragShaderTextList = this.fragShaderTextList;
        shader.vertShaderTextList = this.vertShaderTextList;
        shader.shaderDefineList = this.shaderDefineList;
        shader.shaderName = this.shaderName;
        return shader;
    }
}
