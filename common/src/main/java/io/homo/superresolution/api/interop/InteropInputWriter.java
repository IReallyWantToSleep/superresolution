package io.homo.superresolution.api.interop;

import io.homo.superresolution.core.graphics.impl.texture.ITexture;

import java.util.function.Consumer;

/**
 * Borrowed, render-thread-only access during dispatchWithInputWriter.
 * Do not retain, resize or destroy a destination. Writes must be issued on the current GL context
 * before the callback returns. The interop layer owns the subsequent cross-API handoff.
 */
public interface InteropInputWriter {
    /** Whether this input was allocated for this dispatch. */
    boolean has(InteropResourceType type);

    /**
     * Write final algorithm-ready data, including any required orientation and value transforms.
     * A successful callback suppresses the default transfer for this input.
     */
    void write(InteropResourceType type, Consumer<ITexture> writer);

    /** Copy/convert an external GL source using the normal interop preprocessing semantics. */
    void copyFrom(InteropResourceType type, ITexture source);
}
