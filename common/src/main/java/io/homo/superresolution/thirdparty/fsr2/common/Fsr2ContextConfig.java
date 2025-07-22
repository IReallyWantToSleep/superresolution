package io.homo.superresolution.thirdparty.fsr2.common;

public class Fsr2ContextConfig {
    protected Fsr2ContextFlags flags;
    protected Fsr2Version version;

    public Fsr2ContextFlags getFlags() {
        return flags;
    }

    public Fsr2ContextConfig flags(Fsr2ContextFlags flags) {
        this.flags = flags;
        return this;
    }

    public Fsr2Version getVersion() {
        return version;
    }

    public Fsr2ContextConfig version(Fsr2Version version) {
        this.version = version;
        return this;
    }

    public static Fsr2ContextConfig create() {
        return new Fsr2ContextConfig();
    }
}
