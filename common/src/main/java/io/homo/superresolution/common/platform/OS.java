package io.homo.superresolution.common.platform;

import net.minecraft.network.chat.Component;

public class OS {
    public static OS any = new OS(Arch.ANY, OSType.ANY);
    public Arch arch;
    public OSType type;

    public OS() {
        type = OSType.get();
        arch = Arch.get();
    }

    public OS(Arch arch, OSType type) {
        this.type = type;
        this.arch = arch;
    }

    public String getString() {
        if (any.equals(this)) return Component.translatable("superresolution.requirement.os.any").getString();
        return "%s %s:%s".formatted(
                type.getString(),
                Component.translatable("superresolution.requirement.os.arch").getString(),
                arch.getString()
        );
    }
}
