package io.homo.superresolution.core.graphics.impl.texture;

public class TextureMipmapSettings {
    private final boolean enabled;
    private final int levels;
    private final boolean autoGenerate;
    private final boolean autoResolve;

    private TextureMipmapSettings(boolean enabled, int levels, boolean autoGenerate, boolean autoResolve) {
        this.enabled = enabled;
        this.levels = levels;
        this.autoGenerate = autoGenerate;
        this.autoResolve = autoResolve;
    }

    public static TextureMipmapSettings disabled() {
        return new TextureMipmapSettings(false, 1, false, false);
    }

    public static TextureMipmapSettings auto() {
        return new TextureMipmapSettings(true, -1, true, true);
    }

    public static TextureMipmapSettings manual(int levels, boolean autoGenerate) {
        if (levels < 1) {
            throw new IllegalArgumentException("Mipmap levels must be at least 1");
        }
        return new TextureMipmapSettings(true, levels, autoGenerate, false);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getLevels() {
        return enabled ? levels : 0;
    }

    public boolean isAutoGenerate() {
        return autoGenerate;
    }

    public boolean isAutoResolve() {
        return autoResolve;
    }

    public int resolveLevels(int width, int height) {
        if (!enabled) return 1;
        if (levels > 0) return levels;

        int maxDim = Math.max(width, height);
        return (int) (Math.log(maxDim) / Math.log(2)) + 1;
    }

    @Override
    public String toString() {
        if (!enabled) return "MipmapDisabled";
        if (levels < 0) return "MipmapAuto";
        return "MipmapLevels=" + levels +
                ", Generate=" + (autoGenerate ? "Auto" : "Manual");
    }
}
