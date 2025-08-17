package io.homo.superresolution.core.graphics.impl.texture;

import java.util.Objects;

public class TextureDescription {
    private int width;
    private int height;
    private TextureFormat format;
    private TextureType type;
    private TextureUsages usages = TextureUsages.create();
    private TextureFilterMode filterMode = TextureFilterMode.NEAREST;
    private TextureWrapMode wrapMode = TextureWrapMode.CLAMP_TO_EDGE;
    private TextureMipmapSettings mipmapSettings = TextureMipmapSettings.disabled();

    private String label;


    private TextureDescription() {
    }

    public String getLabel() {
        return label;
    }

    public static TextureDescription.Builder create() {
        return new Builder();
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public TextureFormat getFormat() {
        return format;
    }

    public TextureType getType() {
        return type;
    }

    public TextureUsages getUsages() {
        return usages;
    }

    public TextureFilterMode getFilterMode() {
        return filterMode;
    }

    public TextureWrapMode getWrapMode() {
        return wrapMode;
    }

    public TextureMipmapSettings getMipmapSettings() {
        return mipmapSettings;
    }

    @Override
    public String toString() {
        return "TextureDescription{" +
                "width=" + width +
                ", height=" + height +
                ", format=" + format +
                ", type=" + type +
                ", usages=" + usages +
                ", filterMode=" + filterMode +
                ", wrapMode=" + wrapMode +
                ", mipmap=" + mipmapSettings +
                '}';
    }

    public static class Builder {
        private final TextureDescription description;

        public Builder() {
            this.description = new TextureDescription();
        }

        public Builder width(int width) {
            if (width <= 0) throw new IllegalArgumentException("Width must be positive");
            description.width = width;
            return this;
        }

        public Builder height(int height) {
            if (height <= 0) throw new IllegalArgumentException("Height must be positive");
            description.height = height;
            return this;
        }

        public Builder size(int width, int height) {
            return width(width).height(height);
        }

        public Builder format(TextureFormat format) {
            description.format = Objects.requireNonNull(format, "TextureFormat cannot be null");
            return this;
        }

        public Builder type(TextureType type) {
            description.type = Objects.requireNonNull(type, "TextureType cannot be null");
            return this;
        }

        public Builder usages(TextureUsages usages) {
            if (usages == null || usages.isEmpty()) {
                throw new IllegalArgumentException("At least one usage must be specified");
            }
            description.usages = usages.copy();
            return this;
        }

        public Builder filterMode(TextureFilterMode filterMode) {
            description.filterMode = Objects.requireNonNull(filterMode, "FilterMode cannot be null");
            return this;
        }

        public Builder wrapMode(TextureWrapMode wrapMode) {
            description.wrapMode = Objects.requireNonNull(wrapMode, "WrapMode cannot be null");
            return this;
        }

        public Builder mipmapSettings(TextureMipmapSettings mipmapSettings) {
            description.mipmapSettings = mipmapSettings;
            return this;
        }

        public Builder mipmapsDisabled() {
            description.mipmapSettings = TextureMipmapSettings.disabled();
            return this;
        }

        public Builder mipmapsAuto() {
            description.mipmapSettings = TextureMipmapSettings.auto();
            return this;
        }


        public Builder label(String label) {
            description.label = label;
            return this;
        }

        public Builder mipmapsManual(int levels) {
            description.mipmapSettings = TextureMipmapSettings.manual(levels);
            return this;
        }

        public TextureDescription build() {
            if (description.usages.getUsages().contains(TextureUsage.AttachmentDepth) &&
                    !description.format.name().toUpperCase().startsWith("DEPTH")) {
                throw new IllegalStateException("Depth attachment requires a depth texture format");
            }
            return description;
        }
    }
}