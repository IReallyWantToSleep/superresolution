package io.homo.superresolution.thirdparty.fsr2.common;

import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.opengl.buffer.GlBuffer;

public class Fsr2PipelineResourcesDescription {
    private String name;
    private String shaderName;
    private ITexture texture;
    private GlBuffer ubo;
    private boolean isWritable = false;
    private boolean isUBO = false;

    public GlBuffer ubo() {
        return ubo;
    }

    public Fsr2PipelineResourcesDescription ubo(GlBuffer ubo) {
        this.ubo = ubo;
        return this;
    }

    public boolean isUBO() {
        return isUBO;
    }

    public Fsr2PipelineResourcesDescription ubo(boolean UBO) {
        isUBO = UBO;
        return this;
    }

    public boolean isWritable() {
        return isWritable;
    }

    public Fsr2PipelineResourcesDescription writable(boolean writable) {
        isWritable = writable;
        return this;
    }

    public Fsr2PipelineResourcesDescription name(String name) {
        this.name = name;
        return this;
    }

    public Fsr2PipelineResourcesDescription shaderName(String shaderName) {
        this.shaderName = shaderName;
        return this;
    }

    public Fsr2PipelineResourcesDescription texture(ITexture texture) {
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
