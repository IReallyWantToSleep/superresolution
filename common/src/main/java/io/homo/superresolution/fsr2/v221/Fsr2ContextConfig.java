package io.homo.superresolution.fsr2.v221;

public class Fsr2ContextConfig {
    public Fsr2ContextFlags flags;

    private Fsr2ContextConfig(Fsr2ContextFlags flags) {
        this.flags = flags;
    }

    public static Fsr2ContextConfig create(Fsr2ContextFlags flags) {
        return new Fsr2ContextConfig(flags);
    }
}
