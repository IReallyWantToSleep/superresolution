package io.homo.superresolution.srapi;

import java.util.EnumSet;

public enum SRResourceUsage {
    READ_ONLY(0),
    RENDERTARGET(1 << 0),
    UAV(1 << 1),
    DEPTHTARGET(1 << 2),
    INDIRECT(1 << 3),
    ARRAYVIEW(1 << 4),
    STENCILTARGET(1 << 5),
    DCC_RENDERTARGET(1 << 15);

    public final int value;

    SRResourceUsage(int value) {
        this.value = value;
    }

    public static int toBitmask(EnumSet<SRResourceUsage> usages) {
        int mask = 0;
        for (SRResourceUsage usage : usages) {
            mask |= usage.value;
        }
        return mask;
    }

    public static EnumSet<SRResourceUsage> fromBitmask(int mask) {
        EnumSet<SRResourceUsage> set = EnumSet.noneOf(SRResourceUsage.class);
        for (SRResourceUsage usage : values()) {
            if ((mask & usage.value) != 0) {
                set.add(usage);
            }
        }
        return set;
    }
}
