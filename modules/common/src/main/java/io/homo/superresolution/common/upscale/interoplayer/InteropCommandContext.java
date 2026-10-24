package io.homo.superresolution.common.upscale.interoplayer;

import io.homo.superresolution.core.graphics.impl.command.ICommandBuffer;
import io.homo.superresolution.srapi.SRRenderApiType;

public interface InteropCommandContext extends AutoCloseable {
    SRRenderApiType api();

    ICommandBuffer commandBuffer();

    long nativeHandle();

    boolean borrowed();

    @Override
    void close();
}
