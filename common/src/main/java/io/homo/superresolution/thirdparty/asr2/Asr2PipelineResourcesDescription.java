package io.homo.superresolution.thirdparty.asr2;

import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.opengl.buffer.GlBuffer;

public class Asr2PipelineResourcesDescription {
    private String name;
    private String shaderName;
    private ITexture texture;
    private GlBuffer ubo;
    private boolean isWritable = false;
    private boolean isUBO = false;

    public GlBuffer ubo() {
        return ubo;
    }

    public Asr2PipelineResourcesDescription ubo(GlBuffer ubo) {
        this.ubo = ubo;
        return this;
    }

    public boolean isUBO() {
        return isUBO;
    }

    public Asr2PipelineResourcesDescription ubo(boolean UBO) {
        isUBO = UBO;
        return this;
    }

    public boolean isWritable() {
        return isWritable;
    }

    public Asr2PipelineResourcesDescription writable(boolean writable) {
        isWritable = writable;
        return this;
    }

    public Asr2PipelineResourcesDescription name(String name) {
        this.name = name;
        return this;
    }

    public Asr2PipelineResourcesDescription shaderName(String shaderName) {
        this.shaderName = shaderName;
        return this;
    }

    public Asr2PipelineResourcesDescription texture(ITexture texture) {
        this.texture = texture;
        return this;
    }

    public String name() {
        return name;
    }

    public String shaderName() {
        return shaderName;
    }

    public ITexture texture() {
        return texture;
    }
}
