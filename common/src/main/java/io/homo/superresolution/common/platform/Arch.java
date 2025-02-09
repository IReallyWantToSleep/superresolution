package io.homo.superresolution.common.platform;

import com.sun.jna.Platform;

public enum Arch {
    AARCH64("aarch64"),
    ARM32("arm"),
    X86_64("x86-64"),
    X86("x86"),
    ANY("&^*");

    private final String platformArch;

    Arch(String platformArch) {
        this.platformArch = platformArch;
    }

    public static Arch get() {
        String arch = Platform.ARCH;
        for (Arch a : values()) {
            if (a != ANY && a.platformArch.equals(arch)) {
                return a;
            }
        }
        return ANY;
    }

    public boolean equals(Arch arch) {
        return arch == ANY || Arch.get() == arch;
    }
}