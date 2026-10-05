package io.homo.superresolution.common.upscale.interoplayer;

import io.homo.superresolution.api.InputResourceSet;
import io.homo.superresolution.api.InputResourceType;
import io.homo.superresolution.api.interop.*;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.impl.texture.TextureFormat;
import io.homo.superresolution.core.graphics.impl.texture.TextureType;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static io.homo.superresolution.api.interop.InteropResourceRequirement.FormatSource.*;
import static io.homo.superresolution.api.interop.InteropResourceRequirement.Presence.*;
import static io.homo.superresolution.api.interop.InteropResourceRequirement.SizeSource.*;
import static io.homo.superresolution.api.interop.InteropResourceType.*;
import static org.junit.Assert.*;

public class InteropResourceLayoutTest {
    static ITexture texture(TextureFormat format, int width, int height, long handle) {
        return (ITexture) Proxy.newProxyInstance(ITexture.class.getClassLoader(),
                new Class<?>[]{ITexture.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getTextureFormat" -> {
                        if (format == null) throw new AssertionError("Depth format must not be queried");
                        yield format;
                    }
                    case "getTextureType" -> TextureType.Texture2D;
                    case "getWidth" -> width;
                    case "getHeight" -> height;
                    case "handle" -> handle;
                    case "toString" -> "TestTexture-" + handle;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private static List<InteropResourceRequirement> requirements() {
        return List.of(
                InteropResourceRequirement.input(Color, Required, RenderSize, Context, TextureFormat.RGBA16F),
                InteropResourceRequirement.input(Depth, Required, RenderSize, Fixed, TextureFormat.R32F),
                InteropResourceRequirement.input(MotionVectors, Required, RenderSize, Context, TextureFormat.RG16F),
                InteropResourceRequirement.input(Exposure, Optional, OneByOne, Fixed, TextureFormat.R32F),
                InteropResourceRequirement.output(OutputColor, OutputSize, InternalColorConfig, null));
    }

    private static InteropResourceLayout resolve(InteropResourceContext context, InputResourceSet inputs) {
        return InteropResourceLayout.resolve(requirements(), context, inputs,
                TextureFormat.R11G11B10F, 1280, 720, 1920, 1080);
    }

    @Test
    public void contextControlsColorAndMotionButNotDepthOrOutput() {
        InteropResourceContext context = new InteropResourceContext(Map.of(
                Color, TextureFormat.RGBA32F, MotionVectors, TextureFormat.RG32F, Depth, TextureFormat.DEPTH24));
        InteropResourceLayout layout = resolve(context, null);
        assertEquals(TextureFormat.RGBA32F, layout.resources().get(Color).format());
        assertEquals(TextureFormat.RG32F, layout.resources().get(MotionVectors).format());
        assertEquals(TextureFormat.R32F, layout.resources().get(Depth).format());
        assertEquals(TextureFormat.R11G11B10F, layout.resources().get(OutputColor).format());
        assertEquals(1920, layout.resources().get(OutputColor).width());
    }

    @Test
    public void emptyContextUsesDeclaredFallbacks() {
        InteropResourceLayout layout = resolve(InteropResourceContext.empty(), null);
        assertEquals(TextureFormat.RGBA16F, layout.resources().get(Color).format());
        assertEquals(TextureFormat.RG16F, layout.resources().get(MotionVectors).format());
        assertFalse(layout.resources().containsKey(Exposure));
    }

    @Test
    public void finalInputSnapshotDoesNotReadOrRetainDepthFormat() {
        InputResourceSet inputs = InputResourceSet.create()
                .with(InputResourceType.Color, texture(TextureFormat.RGBA32F, 1280, 720, 1))
                .with(InputResourceType.Depth, texture(null, 1280, 720, 2));
        InteropResourceContext context = InteropResourceContext.fromInputs(inputs);
        assertEquals(Map.of(Color, TextureFormat.RGBA32F), context.formats());
        assertEquals(TextureFormat.R32F, resolve(context, inputs).resources().get(Depth).format());
        inputs.with(InputResourceType.Color, null);
        assertEquals(TextureFormat.RGBA32F, context.formats().get(Color));
    }

    @Test
    public void contextIsDefensivelyCopiedAndImmutable() {
        EnumMap<InteropResourceType, TextureFormat> mutable = new EnumMap<>(InteropResourceType.class);
        mutable.put(Color, TextureFormat.RGBA16F);
        InteropResourceContext context = new InteropResourceContext(mutable);
        mutable.clear();
        assertEquals(TextureFormat.RGBA16F, context.formats().get(Color));
        assertThrows(UnsupportedOperationException.class, () -> context.formats().clear());
    }

    @Test
    public void handlesDoNotChangeLayoutButFormatsDo() {
        InputResourceSet a = InputResourceSet.create().with(InputResourceType.Color,
                texture(TextureFormat.RGBA16F, 1280, 720, 1));
        InputResourceSet b = InputResourceSet.create().with(InputResourceType.Color,
                texture(TextureFormat.RGBA16F, 1280, 720, 2));
        assertEquals(resolve(InteropResourceContext.fromInputs(a), a),
                resolve(InteropResourceContext.fromInputs(b), b));
        b.with(InputResourceType.Color, texture(TextureFormat.RGBA32F, 1280, 720, 2));
        assertNotEquals(resolve(InteropResourceContext.fromInputs(a), a),
                resolve(InteropResourceContext.fromInputs(b), b));
    }

    @Test
    public void optionalResourcesAppearAndDisappearWithoutStaleEntries() {
        InputResourceSet inputs = InputResourceSet.create();
        InteropResourceLayout absent = resolve(InteropResourceContext.empty(), inputs);
        inputs.with(InputResourceType.Exposure, texture(TextureFormat.R32F, 1, 1, 1));
        InteropResourceLayout present = resolve(InteropResourceContext.empty(), inputs);
        assertEquals(1, present.resources().get(Exposure).width());
        assertNotEquals(absent, present);
        inputs.with(InputResourceType.Exposure, null);
        assertEquals(absent, resolve(InteropResourceContext.empty(), inputs));
    }

    @Test
    public void supplementalInputsUseSourceDimensionsAndFormats() {
        List<InteropResourceRequirement> requirements = new ArrayList<>(requirements());
        requirements.add(InteropResourceRequirement.input(NormalRoughness, Optional, SourceSize, SourceTexture, null));
        InputResourceSet inputs = InputResourceSet.create().with(InputResourceType.NormalRoughness,
                texture(TextureFormat.RGBA16F, 640, 360, 7));
        InteropResourceLayout layout = InteropResourceLayout.resolve(requirements, InteropResourceContext.empty(),
                inputs, TextureFormat.RGBA16F, 1280, 720, 1920, 1080);
        assertEquals(640, layout.resources().get(NormalRoughness).width());
        assertEquals(TextureFormat.RGBA16F, layout.resources().get(NormalRoughness).format());
    }

    @Test
    public void rejectsDuplicateMissingOutputAndUnsupportedFormat() {
        List<InteropResourceRequirement> duplicate = new ArrayList<>(requirements());
        duplicate.add(requirements().get(0));
        assertThrows(IllegalArgumentException.class, () -> InteropResourceLayout.resolve(
                duplicate, InteropResourceContext.empty(), null, TextureFormat.RGBA16F, 1, 1, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> InteropResourceLayout.resolve(
                List.of(), InteropResourceContext.empty(), null, TextureFormat.RGBA16F, 1, 1, 1, 1));
        for (TextureFormat format : List.of(TextureFormat.R32UI, TextureFormat.RGB16F, TextureFormat.DEPTH32F)) {
            assertThrows(IllegalArgumentException.class,
                    () -> resolve(new InteropResourceContext(Map.of(Color, format)), null));
        }
    }

    @Test
    public void depthCannotOptIntoSourceOrContextFormats() {
        assertThrows(IllegalArgumentException.class, () -> InteropResourceRequirement.input(
                Depth, Required, RenderSize, Context, TextureFormat.R32F));
        assertThrows(IllegalArgumentException.class, () -> InteropResourceRequirement.input(
                Depth, Required, RenderSize, Fixed, TextureFormat.DEPTH32F));
    }

    @Test
    public void everyInputEnumHasExactlyOneInteropMapping() {
        for (InputResourceType input : InputResourceType.values()) {
            assertEquals(input, InteropResourceType.fromInput(input).inputType());
        }
        assertFalse(OutputColor.isInput());
    }
}
