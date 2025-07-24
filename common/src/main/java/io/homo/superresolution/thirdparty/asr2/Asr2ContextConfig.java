package io.homo.superresolution.thirdparty.asr2;

public class Asr2ContextConfig {
    protected Asr2ContextFlags flags;

    public Asr2ContextFlags getFlags() {
        return flags;
    }

    public Asr2ContextConfig flags(Asr2ContextFlags flags) {
        this.flags = flags;
        return this;
    }


    public static Asr2ContextConfig create() {
        return new Asr2ContextConfig();
    }
}
