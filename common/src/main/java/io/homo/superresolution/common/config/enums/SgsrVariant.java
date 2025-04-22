package io.homo.superresolution.common.config.enums;

import net.minecraft.network.chat.Component;

public enum SgsrVariant {

    CS_2(Component.translatable("superresolution.enum.sgsrvariant.cs_2")),
    CS_3(Component.translatable("superresolution.enum.sgsrvariant.cs_3")),
    FS_2(Component.translatable("superresolution.enum.sgsrvariant.fs_2"));
    private final Component tooltip;

    SgsrVariant(Component tooltip) {
        this.tooltip = tooltip;
    }

    public Component getComponent() {
        return tooltip;
    }
}
