package io.homo.superresolution.common.upscale.interoplayer;

import io.homo.superresolution.api.InputResourceSet;
import io.homo.superresolution.api.InputResourceType;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.impl.texture.TextureFormat;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static io.homo.superresolution.api.interop.InteropResourceType.*;
import static org.junit.Assert.*;

public class InteropInputWriteSessionTest {
    private final ITexture source = InteropResourceLayoutTest.texture(TextureFormat.RGBA16F, 10, 10, 1);
    private final ITexture destination = InteropResourceLayoutTest.texture(TextureFormat.RGBA16F, 10, 10, 2);

    @Test
    public void successfulWriteSuppressesOnlyThatDefaultTransfer() {
        InputResourceSet inputs = InputResourceSet.create()
                .with(InputResourceType.Color, source).with(InputResourceType.Depth, source);
        try (InteropInputWriteSession session = new InteropInputWriteSession(
                Map.of(Color, destination, Depth, destination), (type, texture) -> fail("Unexpected copy"))) {
            session.write(Color, texture -> assertSame(destination, texture));
            assertNull(session.pendingSource(Color, inputs));
            assertSame(source, session.pendingSource(Depth, inputs));
        }
    }

    @Test
    public void copyUsesRegisteredTransferAndMarksInputProvided() {
        AtomicReference<ITexture> copied = new AtomicReference<>();
        try (InteropInputWriteSession session = new InteropInputWriteSession(
                Map.of(Color, destination), (type, texture) -> {
                    assertEquals(Color, type);
                    copied.set(texture);
                })) {
            session.copyFrom(Color, source);
            assertSame(source, copied.get());
            assertNull(session.pendingSource(Color, InputResourceSet.create().with(InputResourceType.Color, source)));
        }
    }

    @Test
    public void failedWritePoisonsSessionEvenWhenCallerCatchesException() {
        try (InteropInputWriteSession session = new InteropInputWriteSession(
                Map.of(Color, destination), (type, texture) -> {})) {
            assertThrows(IllegalArgumentException.class, () -> session.write(Color, texture -> {
                throw new IllegalArgumentException("Partial external write");
            }));
            assertThrows(IllegalStateException.class, session::requireHealthy);
        }
    }

    @Test
    public void rejectsMissingOutputsDuplicateAndClosedAccess() {
        InteropInputWriteSession session = new InteropInputWriteSession(
                Map.of(Color, destination, OutputColor, destination), (type, texture) -> {});
        assertFalse(session.has(OutputColor));
        assertThrows(IllegalArgumentException.class, () -> session.write(OutputColor, texture -> {}));
        assertThrows(IllegalArgumentException.class, () -> session.write(Exposure, texture -> {}));
        session.write(Color, texture -> {});
        assertThrows(IllegalArgumentException.class, () -> session.write(Color, texture -> {}));
        session.close();
        assertThrows(IllegalStateException.class, () -> session.has(Color));
        assertThrows(IllegalStateException.class, () -> session.copyFrom(Color, source));
    }

    @Test
    public void rejectsNestedAndCrossThreadAccess() throws InterruptedException {
        try (InteropInputWriteSession session = new InteropInputWriteSession(
                Map.of(Color, destination), (type, texture) -> {})) {
            AtomicReference<Throwable> error = new AtomicReference<>();
            Thread thread = new Thread(() -> {
                try {
                    session.has(Color);
                } catch (Throwable failure) {
                    error.set(failure);
                }
            });
            thread.start();
            thread.join();
            assertTrue(error.get() instanceof IllegalStateException);
            assertThrows(IllegalStateException.class,
                    () -> session.write(Color, texture -> session.write(Color, nested -> {})));
        }
    }
}
