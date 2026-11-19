package io.homo.superresolution.common.upscale.algo.legacy.fsr1;

#if MC_VER == MC_26_3 && IS_VULKAN == 1
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.ShaderSource;
import com.mojang.renderpearl.api.pipeline.ShaderType;
import com.mojang.renderpearl.api.pipeline.UniformType;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.mojang.renderpearl.backend.vulkan.VulkanCommandEncoder;
import com.mojang.renderpearl.backend.vulkan.VulkanConst;
import com.mojang.renderpearl.backend.vulkan.VulkanGpuTextureView;
import io.homo.superresolution.api.InitializationDescription;
import io.homo.superresolution.api.InputResourceType;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.minecraft.B3DVulkanBridge;
import io.homo.superresolution.common.mixin.core.accessor.VulkanCommandEncoderAccessor;
import io.homo.superresolution.common.upscale.DispatchResource;
import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.impl.texture.TextureFormat;
import io.homo.superresolution.core.graphics.impl.texture.TextureType;
import io.homo.superresolution.core.graphics.impl.texture.TextureUsages;
import io.homo.superresolution.core.graphics.vulkan.VulkanExternalTexture;
import net.minecraft.resources.Identifier;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;

import static org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_GENERAL;

public final class IrisVulkanFsr1 extends FSR1 {
    private Target input;
    private Target intermediate;
    private Target output;
    private CompiledRenderPipeline easu;
    private CompiledRenderPipeline rcas;

    @Override
    public void initialize(InitializationDescription description) {
        initDesc = description;
    }

    @Override
    public boolean dispatch(DispatchResource resource) {
        ITexture color = resource.resources().get(InputResourceType.Color);
        if (color == null || color.getTextureFormat().isInteger() || color.getTextureFormat().isDepth() || color.getTextureFormat().isStencil()) return false;
        int width = resource.renderWidth(), height = resource.renderHeight();
        int outputWidth = resource.screenWidth(), outputHeight = resource.screenHeight();
        if (width < 1 || height < 1 || color.getWidth() != width || color.getHeight() != height || outputWidth < 1 || outputHeight < 1) return false;
        if (input == null || !input.matches(width, height, color.getTextureFormat())) {
            GpuFormat format = null;
            for (GpuFormat candidate : GpuFormat.values()) {
                if (candidate.hasColorAspect() && VulkanConst.toVk(candidate) == color.getTextureFormat().vk()) {
                    format = candidate;
                    break;
                }
            }
            if (format == null) return false;
            if (input != null) input.close();
            input = new Target("SR FSR1 input", width, height, format);
        }
        if (output == null || !output.matches(outputWidth, outputHeight, TextureFormat.RGBA16F)) {
            releaseOutput();
            intermediate = new Target("SR FSR1 EASU", outputWidth, outputHeight, GpuFormat.RGBA16_FLOAT);
            output = new Target("SR FSR1 RCAS", outputWidth, outputHeight, GpuFormat.RGBA16_FLOAT);
            SuperResolution.LOGGER.info("Iris Vulkan FSR 1: {}x{} -> {}x{}", width, height, outputWidth, outputHeight);
        }
        if (easu == null) easu = compile("easu", GpuFormat.RGBA16_FLOAT);
        if (rcas == null) rcas = compile("rcas", GpuFormat.RGBA16_FLOAT);
        var encoder = RenderSystem.getDevice().createCommandEncoder();
        var commands = ((VulkanCommandEncoderAccessor) B3DVulkanBridge.createCommandEncoder()).currentCommandBuffer();
        try (var stack = MemoryStack.stackPush()) {
            B3DVulkanBridge.recordCommands(buffer -> buffer.copyTexture(color, input.texture, 0, 0, width, height, 0, 0, 0, width, height, 0));
            VulkanCommandEncoder.memoryBarrier(commands, stack);
            draw(encoder, stack, easu, input.target.getColorTextureView(), intermediate.target.getColorTextureView(), 0, 0, outputWidth, outputHeight);
            draw(encoder, stack, rcas, intermediate.target.getColorTextureView(), output.target.getColorTextureView(), 0, 0, outputWidth, outputHeight);
            VulkanCommandEncoder.memoryBarrier(commands, stack);
        }
        return true;
    }

    private static final class Target implements AutoCloseable {
        final TextureTarget target;
        final VulkanExternalTexture texture;

        Target(String name, int width, int height, GpuFormat format) {
            target = new TextureTarget(name, width, height, format, null);
            var view = (VulkanGpuTextureView) target.getColorTextureView();
            texture = VulkanExternalTexture.fixedLayout(RenderSystems.vulkan().device(), view.texture().vkImage(), view.vkImageView(),
                    TextureFormat.fromVk(VulkanConst.toVk(format)), TextureType.Texture2D, width, height, 1, VK_IMAGE_LAYOUT_GENERAL,
                    TextureUsages.create().sampler().transferSource().transferDestination());
        }

        boolean matches(int width, int height, TextureFormat format) {
            return target.width == width && target.height == height && texture.getTextureFormat() == format;
        }

        @Override public void close() {
            texture.destroy();
            target.destroyBuffers();
        }
    }

    @Override public ITexture getOutputTexture() { return output == null ? null : output.texture; }

    @Override public void resize(int width, int height) { releaseOutput(); }

    private void releaseOutput() {
        if (output != null) { output.close(); output = null; }
        if (intermediate != null) { intermediate.close(); intermediate = null; }
    }

    @Override public void destroy() {
        releaseOutput();
        if (input != null) { input.close(); input = null; }
        if (easu != null) { easu.close(); easu = null; }
        if (rcas != null) { rcas.close(); rcas = null; }
    }

    private static void draw(CommandEncoder encoder, MemoryStack stack, CompiledRenderPipeline pipeline, GpuTextureView source,
                             GpuTextureView target, int left, int top, int width, int height) {
        var data = stack.calloc(32).putInt(left).putInt(top).putInt(width).putInt(height).putFloat(SuperResolutionConfig.getSharpness());
        data.rewind();
        var params = encoder.transientMemory().uploadStaging(data, RenderSystem.getDevice().getDeviceInfo().limits().minUniformOffsetAlignment(), GpuBuffer.USAGE_UNIFORM);
        try (var renderPass = encoder.createRenderPass(() -> "SR Iris Vulkan FSR1", target, Optional.empty())) {
            renderPass.setPipeline(pipeline);
            renderPass.setUniform("InSampler", source, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            renderPass.setUniform("FsrParams", params);
            renderPass.enableScissor(left, top, width, height);
            renderPass.draw(3, 1, 0, 0);
        }
    }

    private static CompiledRenderPipeline compile(String stage, GpuFormat format) {
        Identifier name = Identifier.fromNamespaceAndPath("super_resolution", "fsr1/" + stage);
        var pipeline = RenderPipeline.builder().withLocation(name).withVertexShader("core/screenquad").withFragmentShader(name)
                .withPrimitiveTopology(PrimitiveTopology.TRIANGLES).withCull(false)
                .withColorTargetState(new ColorTargetState(Optional.empty(), format, ColorTargetState.WRITE_ALL))
                .withBindGroupLayout(BindGroupLayout.builder().withUniform("InSampler", UniformType.COMBINED_IMAGE_SAMPLER)
                        .withUniform("FsrParams", UniformType.UNIFORM_BUFFER).build()).build();
        return Objects.requireNonNull(RenderSystem.getDevice().compilePipeline(pipeline, Sources.INSTANCE, Runnable::run)
                .join().finishCompile(), "Could not compile " + name);
    }

    private static final class Sources implements ShaderSource {
        static final Sources INSTANCE = new Sources();
        final String vertex = read("/assets/minecraft/shaders/core/screenquad.vsh");
        final String fragment = read("/shader/fsr1/iris_vulkan.frag.glsl")
                .replace("#include \"ffx_a.h\"", read("/shader/fsr1/ffx_a.h"))
                .replace("#include \"ffx_fsr1.h\"", read("/shader/fsr1/ffx_fsr1.h"));

        @Override
        public String getShader(Identifier id, ShaderType type) {
            return type == ShaderType.VERTEX ? vertex : fragment.replace("FSR_STAGE", id.getPath().endsWith("easu") ? "FSR_EASU_F" : "FSR_RCAS_F");
        }

        @Override public CachedIncludeSource getInclude(Identifier id) { return null; }
        @Override public void close() { }

        private static String read(String path) {
            try (var stream = Objects.requireNonNull(Sources.class.getResourceAsStream(path), path)) {
                return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException failure) {
                throw new UncheckedIOException(failure);
            }
        }
    }

}
#endif
