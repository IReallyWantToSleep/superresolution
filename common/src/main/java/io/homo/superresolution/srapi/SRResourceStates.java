package io.homo.superresolution.srapi;

import java.util.EnumSet;

public enum SRResourceStates {
    COMMON(1 << 0),
    UNORDERED_ACCESS(1 << 1),
    COMPUTE_READ(1 << 2),
    PIXEL_READ(1 << 3),
    PIXEL_COMPUTE_READ(COMPUTE_READ.value | PIXEL_READ.value),
    COPY_SRC(1 << 4),
    COPY_DEST(1 << 5),
    GENERIC_READ(COPY_SRC.value | COMPUTE_READ.value),
    INDIRECT_ARGUMENT(1 << 6),
    PRESENT(1 << 7),
    RENDER_TARGET(1 << 8),
    DEPTH_ATTACHMENT(1 << 9);

    public final int value;

    SRResourceStates(int value) {
        this.value = value;
    }

    public static int toBitmask(EnumSet<SRResourceStates> states) {
        int mask = 0;
        for (SRResourceStates state : states) {
            mask |= state.value;
        }
        return mask;
    }

    public static EnumSet<SRResourceStates> fromBitmask(int mask) {
        EnumSet<SRResourceStates> set = EnumSet.noneOf(SRResourceStates.class);
        for (SRResourceStates state : values()) {
            if ((mask & state.value) != 0) {
                set.add(state);
            }
        }
        return set;
    }
}
