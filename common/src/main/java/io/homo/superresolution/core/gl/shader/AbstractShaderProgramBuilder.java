package io.homo.superresolution.core.gl.shader;

import io.homo.superresolution.core.utils.ShaderCache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractShaderProgramBuilder<T extends AbstractGlShaderProgram> {
    private final ArrayList<String> fragShaderTextList = new ArrayList<>();
    private final ArrayList<String> vertShaderTextList = new ArrayList<>();
    private final Map<String, GlGeneralShaderProgram.ShaderInclude> shaderIncludeList = new HashMap<>();
    private final ArrayList<String> shaderDefineList = new ArrayList<>();
    private boolean enableCache = false;
    private String shaderName = "";

    public AbstractShaderProgramBuilder() {
    }

    public AbstractShaderProgramBuilder<T> enableCache(boolean cache) {
        enableCache = cache;
        return this;
    }

    public AbstractShaderProgramBuilder<T> addShaderInclude(GlGeneralShaderProgram.ShaderInclude include) {
        this.shaderIncludeList.put(include.name, include);
        return this;
    }

    public AbstractShaderProgramBuilder<T> setShaderName(String shaderName) {
        this.shaderName = shaderName;
        return this;
    }

    public AbstractShaderProgramBuilder<T> addFragShaderText(String text) {
        this.fragShaderTextList.add(text);
        return this;
    }

    public AbstractShaderProgramBuilder<T> addDefineText(String name, String value) {
        this.shaderDefineList.add("#define %s %s".formatted(name, value));
        return this;
    }

    public AbstractShaderProgramBuilder<T> addFragShaderText(String text, int index) {
        this.fragShaderTextList.add(index, text);
        return this;
    }

    public AbstractShaderProgramBuilder<T> addAllFragShaderTextList(Collection<String> text, int index) {
        this.fragShaderTextList.addAll(index, text);
        return this;
    }

    public AbstractShaderProgramBuilder<T> addAllFragShaderTextList(Collection<String> text) {
        this.fragShaderTextList.addAll(text);
        return this;
    }

    public AbstractShaderProgramBuilder<T> addVertShaderText(String text, int index) {
        this.vertShaderTextList.add(index, text);
        return this;
    }

    public AbstractShaderProgramBuilder<T> addAllVertShaderTextList(Collection<String> text, int index) {
        this.vertShaderTextList.addAll(index, text);
        return this;
    }

    public AbstractShaderProgramBuilder<T> addAllVertShaderTextList(Collection<String> text) {
        this.vertShaderTextList.addAll(text);
        return this;
    }

    protected ShaderCache.ShaderBinary getShaderCache() {
        if (enableCache) {
            return ShaderCache.getProgramBinary(updateShader(new GlGeneralShaderProgram()));
        }
        return null;
    }

    protected boolean checkShaderCache() {
        if (enableCache) {
            return ShaderCache.checkProgramBinary(updateShader(new GlGeneralShaderProgram()));
        }
        return false;
    }

    public abstract T build();

    @SuppressWarnings("unchecked")
    protected T updateShader(AbstractGlShaderProgram shader) {
        shader.shaderIncludeList = this.shaderIncludeList;
        shader.fragShaderTextList = this.fragShaderTextList;
        shader.vertShaderTextList = this.vertShaderTextList;
        shader.shaderDefineList = this.shaderDefineList;
        shader.shaderName = this.shaderName;
        shader.enableCache = enableCache;
        return (T) shader;
    }
}
