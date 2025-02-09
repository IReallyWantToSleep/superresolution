package io.homo.superresolution.common.render.vulkan;

import java.util.stream.IntStream;

public class QueueFamilyIndices {
    public Integer graphicsFamily;
    public Integer presentFamily;

    protected boolean isComplete() {
        return graphicsFamily != null;
    }

    public int[] unique() {
        return IntStream.of(graphicsFamily).toArray();
    }

    public int[] array() {
        return new int[] {graphicsFamily, presentFamily};
    }
}
