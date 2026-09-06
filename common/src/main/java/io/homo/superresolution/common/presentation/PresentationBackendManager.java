package io.homo.superresolution.common.presentation;

import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.api.platform.OperatingSystemType;
import io.homo.superresolution.common.framegeneration.FrameGenerationDescriptions;
import io.homo.superresolution.common.presentation.api.PresentationBackend;
import io.homo.superresolution.common.presentation.api.PresentationBackendType;
import io.homo.superresolution.common.presentation.opengl.OpenGLPresentationBackend;
import io.homo.superresolution.common.presentation.vulkan.VulkanPresentationBackend;

public final class PresentationBackendManager {
    private static final PresentationBackend OPENGL = new OpenGLPresentationBackend();
    private static final PresentationBackend VULKAN = new VulkanPresentationBackend();
    /** The configured value captured for this process. */
    private static volatile PresentationBackendType configuredBackend;
    /** The effective backend used by all presentation-aware callers. */
    private static volatile PresentationBackend backend;

    private PresentationBackendManager() {
    }

    public static synchronized PresentationBackend backend() {
        if (backend == null) {
            SuperResolutionConfig.SPEC.load();
            configuredBackend = SuperResolutionConfig.getPresentationBackend();
            backend = resolve(configuredBackend);
        }
        return backend;
    }

    public static PresentationBackendType type() {
        return backend().type();
    }

    public static boolean isVulkanPresentationRequested() {
        return type() == PresentationBackendType.VULKAN;
    }

    public static boolean isVulkanPresentationAvailable() {
        PresentationBackend current = backend();
        return current.type() == PresentationBackendType.VULKAN && current.isAvailable();
    }

    public static boolean isInitialized() {
        return backend().isInitialized();
    }

    public static void endMinecraftFrame() {
        backend().endMinecraftFrame();
    }

    public static void flushCapturedFrame() {
        backend().flushCapturedFrame();
    }

    public static void setVsync(boolean enabled) {
        backend().setVsync(enabled);
    }

    public static boolean shutdownApplicationManagedProvider(String providerId, Runnable teardown) {
        return backend().shutdownApplicationManagedProvider(providerId, teardown);
    }

    public static synchronized void shutdown() {
        backend().shutdown();
    }

    public static void collectGpuTimestamps() {
        if (backend() instanceof VulkanPresentationBackend vulkan) {
            vulkan.collectGpuTimestamps();
        }
    }

    /**
     * Returns whether the startup configuration requires the Streamline interposer.
     * Presentation selection is intentionally part of this decision so callers do not
     * inspect the presentation config directly.
     */
    public static boolean shouldInitializeStreamline() {
        return isVulkanPresentationRequested()
                && SuperResolutionConfig.CURRENT_OS_TYPE == OperatingSystemType.WINDOWS
                && FrameGenerationDescriptions.mayUseStreamline(
                        SuperResolutionConfig.getFrameGenerationProvider());
    }

    public static synchronized void disableAfterFailure(Throwable failure) {
        if (backend == null) {
            backend();
        }
        if (configuredBackend != PresentationBackendType.VULKAN) {
            return;
        }
        SuperResolution.LOGGER.error("Vulkan presentation initialization failed; disabling it for the next launch", failure);
        try {
            // Keep the Vulkan adapter responsible for releasing a partially-created
            // surface/context before the effective backend is replaced with OpenGL.
            VULKAN.shutdown();
        } catch (Throwable cleanupFailure) {
            failure.addSuppressed(cleanupFailure);
        }
        backend = OPENGL;
        configuredBackend = PresentationBackendType.OPENGL;
        try {
            SuperResolutionConfig.setPresentationBackend(PresentationBackendType.OPENGL);
            SuperResolutionConfig.SPEC.save();
        } catch (Throwable saveFailure) {
            failure.addSuppressed(saveFailure);
        }
    }

    private static PresentationBackend resolve(PresentationBackendType requested) {
        return switch (requested) {
            case OPENGL -> OPENGL;
            case VULKAN -> {
                if (!isVulkanSupported()) {
                    SuperResolution.LOGGER.warn("Vulkan presentation is not supported by this Minecraft version; using OpenGL");
                    yield OPENGL;
                }
                if (SuperResolutionConfig.isSkipInitVulkan()) {
                    SuperResolution.LOGGER.warn("Vulkan presentation was selected but Vulkan initialization is skipped; using OpenGL");
                    yield OPENGL;
                }
                yield VULKAN;
            }
            case D3D12 -> {
                SuperResolution.LOGGER.warn("D3D12 presentation is not implemented; using OpenGL");
                yield OPENGL;
            }
        };
    }

    private static boolean isVulkanSupported() {
        #if (MC_VER >= MC_1_21_11 && MC_VER < MC_26_2) || (MC_VER >= MC_1_21 && MC_VER < MC_1_21_2) || MC_VER == MC_1_20_1 || MC_VER == MC_26_2
        return true;
        #else
        return false;
        #endif
    }
}
