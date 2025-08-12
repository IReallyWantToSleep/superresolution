package io.homo.superresolution.core.graphics.impl;

import io.homo.superresolution.core.graphics.impl.texture.ITexture;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CopyOperation {

    public enum TextureChancel {
        R, G, B, A
    }

    public static class ChannelMapping {
        public final TextureChancel src;
        public final TextureChancel dst;

        public ChannelMapping(TextureChancel src, TextureChancel dst) {
            this.src = Objects.requireNonNull(src);
            this.dst = Objects.requireNonNull(dst);
        }
    }

    private ITexture srcTexture;
    private ITexture dstTexture;
    private final List<ChannelMapping> mappings = new ArrayList<>();

    private CopyOperation() {
    }

    public static CopyOperation create() {
        return new CopyOperation();
    }

    public CopyOperation src(ITexture texture) {
        this.srcTexture = Objects.requireNonNull(texture);
        return this;
    }

    public CopyOperation dst(ITexture texture) {
        this.dstTexture = Objects.requireNonNull(texture);
        return this;
    }

    public CopyOperation fromTo(TextureChancel src, TextureChancel dst) {
        mappings.add(new ChannelMapping(src, dst));
        return this;
    }

    public ITexture getSrcTexture() {
        return srcTexture;
    }

    public ITexture getDstTexture() {
        return dstTexture;
    }

    public List<ChannelMapping> getMappings() {
        return List.copyOf(mappings);
    }

}
