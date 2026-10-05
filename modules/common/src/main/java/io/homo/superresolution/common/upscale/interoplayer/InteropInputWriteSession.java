package io.homo.superresolution.common.upscale.interoplayer;

import io.homo.superresolution.api.InputResourceSet;
import io.homo.superresolution.api.interop.InteropInputWriter;
import io.homo.superresolution.api.interop.InteropResourceType;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;

import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Scope and failure tracking shared by the external writer and the default input transfer. */
final class InteropInputWriteSession implements InteropInputWriter, AutoCloseable {
    private final Thread thread = Thread.currentThread();
    private final Map<InteropResourceType, ? extends ITexture> destinations;
    private final BiConsumer<InteropResourceType, ITexture> transfer;
    private final EnumSet<InteropResourceType> written = EnumSet.noneOf(InteropResourceType.class);
    private boolean active = true;
    private boolean failed;
    private boolean writing;

    InteropInputWriteSession(Map<InteropResourceType, ? extends ITexture> destinations,
                             BiConsumer<InteropResourceType, ITexture> transfer) {
        this.destinations = destinations;
        this.transfer = transfer;
    }

    void requireHealthy() {
        if (Thread.currentThread() != thread || !active || failed || writing) {
            throw new IllegalStateException("Input writer is closed, failed, reentrant or on another thread");
        }
    }

    @Override
    public boolean has(InteropResourceType type) {
        requireHealthy();
        return type.isInput() && destinations.containsKey(type);
    }

    @Override
    public void write(InteropResourceType type, Consumer<ITexture> writer) {
        requireHealthy();
        if (!type.isInput() || !destinations.containsKey(type) || written.contains(type)) {
            throw new IllegalArgumentException("Input is absent, not writable or already written: " + type);
        }
        writing = true;
        try {
            Objects.requireNonNull(writer).accept(destinations.get(type));
            written.add(type);
        } catch (RuntimeException | Error error) {
            failed = true;
            throw error;
        } finally {
            writing = false;
        }
    }

    @Override
    public void copyFrom(InteropResourceType type, ITexture source) {
        write(type, destination -> transfer.accept(type, Objects.requireNonNull(source)));
    }

    ITexture pendingSource(InteropResourceType type, InputResourceSet inputs) {
        requireHealthy();
        return written.contains(type) || !destinations.containsKey(type) ? null : inputs.get(type.inputType());
    }

    @Override
    public void close() {
        active = false;
    }
}
