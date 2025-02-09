package io.homo.superresolution.common.platform;

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
}
