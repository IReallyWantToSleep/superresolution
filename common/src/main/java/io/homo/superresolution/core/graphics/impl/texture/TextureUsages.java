package io.homo.superresolution.core.graphics.impl.texture;

import java.util.ArrayList;
import java.util.List;

public class TextureUsages {
    private List<TextureUsage> usages = new ArrayList<>();

    private TextureUsages(List<TextureUsage> usages) {
        this.usages = usages;
    }

    private TextureUsages() {
    }

    public static TextureUsages create() {
        return new TextureUsages();
    }

    public List<TextureUsage> getUsages() {
        return usages;
    }

    public TextureUsages copy() {
        return new TextureUsages(new ArrayList<>(usages));
    }

    public boolean isEmpty() {
        return usages.isEmpty();
    }

    public TextureUsages sampler() {
        usages.add(TextureUsage.Sampler);
        return this;
    }

    public TextureUsages storage() {
        usages.add(TextureUsage.Storage);
        return this;
    }

    public TextureUsages attachmentColor() {
        usages.add(TextureUsage.AttachmentColor);
        return this;
    }

    public TextureUsages attachmentDepth() {
        usages.add(TextureUsage.AttachmentDepth);
        return this;
    }

    public TextureUsages transferSource() {
        usages.add(TextureUsage.TransferSource);
        return this;
    }

    public TextureUsages transferDestination() {
        usages.add(TextureUsage.TransferDestination);
        return this;
    }
}
