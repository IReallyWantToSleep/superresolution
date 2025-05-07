package io.homo.superresolution.common.gui;

import io.homo.superresolution.api.registry.AlgorithmDescription;
import io.homo.superresolution.common.gui.widgets.Line;
import io.homo.superresolution.common.platform.OS;
import io.homo.superresolution.common.platform.Platform;
import io.homo.superresolution.core.GraphicsCapabilities;
import io.homo.superresolution.core.SuperResolutionNative;
import io.homo.superresolution.core.interop.GlVkInteropManager;
import io.homo.superresolution.common.upscale.AlgorithmDescriptions;
import io.homo.superresolution.core.utils.ColorUtil;
import io.homo.superresolution.core.NativeLibManager;
import io.homo.superresolution.api.utils.Requirement;
import net.minecraft.network.chat.Component;

import java.util.List;

public class InfoBuilder {
    private final LineContainer lineContainer;

    InfoBuilder(LineContainer lineContainer) {
        this.lineContainer = lineContainer;
    }

    public static InfoBuilder of(LineContainer lineContainer) {
        return new InfoBuilder(lineContainer);
    }


    public InfoBuilder addGlExt() {
        this.lineContainer.addLine(
                new Line()
                        .text(Component.translatable("superresolution.screen.info.text.opengl_ext_count").getString()
                                .formatted(GraphicsCapabilities.getGLExtensions().size())
                        )
                        .center(true)
                        .color(255, 255, 255, 255)
        );
        for (String ext : GraphicsCapabilities.getGLExtensions()) {
            this.lineContainer.addLine(
                    new Line()
                            .text(ext)
                            .color(255, 255, 255, 255)
            );
        }
        return this;
    }

    public InfoBuilder addVkExt() {
        this.lineContainer.addLine(
                new Line()
                        .text(Component.translatable("superresolution.screen.info.text.vulkan_ext_count").getString()
                                .formatted(GraphicsCapabilities.getVulkanDeviceExtensions().size())
                        )
                        .center(true)
                        .color(255, 255, 255, 255)
        );
        for (String ext : GraphicsCapabilities.getVulkanDeviceExtensions()) {
            this.lineContainer.addLine(
                    new Line()
                            .text(ext)
                            .color(255, 255, 255, 255)
            );
        }
        return this;
    }

    public InfoBuilder addEnvInfo() {
        this.lineContainer.addLine(
                new Line()
                        .text(
                                Component.translatable("superresolution.screen.info.text.mod_version").getString()
                                        .formatted(Platform.currentPlatform.getModVersionString("super_resolution"))
                        )
                        .color(255, 255, 255, 255)
        );
        this.lineContainer.addLine(
                new Line()
                        .text(
                                Component.translatable("superresolution.screen.info.text.native_lib_version").getString()
                                        .formatted(NativeLibManager.nativeApiAvailable() ? SuperResolutionNative.getVersionInfo() : "???")
                        )
                        .color(255, 255, 255, 255)
        );
        this.lineContainer.addLine(
                new Line()
                        .text(
                                Component.translatable("superresolution.screen.info.text.opengl_version").getString()
                                        .formatted(
                                                GraphicsCapabilities.getGLVersion()[0],
                                                GraphicsCapabilities.getGLVersion()[1]
                                        )
                        )
                        .color(255, 255, 255, 255)
        );
        this.lineContainer.addLine(
                new Line()
                        .text(
                                Component.translatable("superresolution.screen.info.text.vulkan_available").getString()
                                        .formatted(
                                                GlVkInteropManager.isSupportVulkan() ?
                                                        Component.translatable("superresolution.screen.text.yes").getString() :
                                                        Component.translatable("superresolution.screen.text.no").getString()
                                        )
                        )
                        .color(255, 255, 255, 255)
        );
        this.lineContainer.addLine(
                new Line()
                        .text(
                                Component.translatable(
                                                "superresolution.screen.info.text.vulkan_version").getString()
                                        .formatted(
                                                GraphicsCapabilities.getVulkanVersion()[0],
                                                GraphicsCapabilities.getVulkanVersion()[1],
                                                GraphicsCapabilities.getVulkanVersion()[2]
                                        )
                        )
                        .color(255, 255, 255, 255)
        );
        this.lineContainer.addLine(
                new Line()
                        .text(
                                Component.translatable("superresolution.screen.info.text.os_name").getString()
                                        .formatted(
                                                Platform.currentPlatform.getOS().type.getString()
                                        )
                        )
                        .color(255, 255, 255, 255)
        );
        this.lineContainer.addLine(
                new Line()
                        .text(
                                Component.translatable("superresolution.screen.info.text.os_arch").getString()
                                        .formatted(
                                                Platform.currentPlatform.getOS().arch.getString()
                                        )
                        )
                        .color(255, 255, 255, 255)
        );
        return this;

    }

    public InfoBuilder addAlgoInfo(AlgorithmDescription<?> algo) {
        if (algo.equals(AlgorithmDescriptions.NONE)) return this;
        this.lineContainer.addLine(new Line().text(algo.getDisplayName()).center(true).color(255, 255, 255, 255));
        Requirement req = algo.getRequirement();
        Requirement.Result result = req.check();
        List<String> missingGlExtension = req.getMissingGlExtensions();
        List<String> missingVkExtension = req.getMissingVkExtensions();
        this.lineContainer.addLine(
                new Line()
                        .text(
                                Component.translatable("superresolution.screen.info.text.min_opengl_version").getString()
                                        .formatted(
                                                req.getGlMajorVersion() == -1 ? "*" : req.getGlMajorVersion(),
                                                req.getGlMinorVersion() == -1 ? "*" : req.getGlMinorVersion()
                                        )
                        )
                        .color(255, 255, 255, 255)
        );
        this.lineContainer.addLine(
                new Line()
                        .text(
                                Component.translatable("superresolution.screen.info.text.min_vulkan_version").getString()
                                        .formatted(
                                                req.getVulkanMajorVersion() == -1 ? "*" : req.getVulkanMajorVersion(),
                                                req.getVulkanMinorVersion() == -1 ? "*" : req.getVulkanMinorVersion(),
                                                req.getVulkanPatchVersion() == -1 ? "*" : req.getVulkanPatchVersion()

                                        )
                        )
                        .color(255, 255, 255, 255)
        );
        this.lineContainer.addLine(
                new Line()
                        .text(Component.translatable("superresolution.screen.info.text_need_opengl_ext"))
                        .color(255, 255, 255, 255)
        );
        if (req.getRequiredGlExtensions().isEmpty())
            this.lineContainer.addLine(
                    new Line()
                            .text(Component.translatable("superresolution.screen.text.none"))
                            .left(0.02f)
                            .color(255, 255, 255, 255)
            );
        for (String glExt : req.getRequiredGlExtensions()) {
            this.lineContainer.addLine(
                    new Line()
                            .text("%s (%s)".formatted(
                                    glExt,
                                    !missingGlExtension.contains(glExt) ?
                                            Component.translatable("superresolution.screen.text.exist").getString() :
                                            Component.translatable("superresolution.screen.text.missing").getString()
                            ))
                            .left(0.02f)
                            .color(
                                    missingGlExtension.contains(glExt) ?
                                            ColorUtil.color(255, 255, 0, 0) :
                                            ColorUtil.color(255, 255, 255, 255)
                            )
            );
        }

        this.lineContainer.addLine(
                new Line()
                        .text(Component.translatable("superresolution.screen.info.text_need_vulkan_ext"))
                        .color(255, 255, 255, 255)
        );

        if (req.getRequiredVulkanDeviceExtensions().isEmpty())
            this.lineContainer.addLine(
                    new Line()
                            .text(Component.translatable("superresolution.screen.text.none"))
                            .left(0.02f)
                            .color(255, 255, 255, 255)
            );
        for (String vkExt : req.getRequiredVulkanDeviceExtensions()) {
            this.lineContainer.addLine(
                    new Line()
                            .text("%s (%s)".formatted(
                                    vkExt,
                                    !missingVkExtension.contains(vkExt) ?
                                            Component.translatable("superresolution.screen.text.exist").getString() :
                                            Component.translatable("superresolution.screen.text.missing").getString()
                            ))
                            .left(0.02f)
                            .color(
                                    missingVkExtension.contains(vkExt) ?
                                            ColorUtil.color(255, 255, 0, 0) :
                                            ColorUtil.color(255, 255, 255, 255)
                            )
            );
        }

        this.lineContainer.addLine(
                new Line()
                        .text(Component.translatable("superresolution.screen.info.text.req_os_name_and_os_arch"))
                        .color(255, 255, 255, 255)
        );
        if (req.getSupportedOS().isEmpty())
            this.lineContainer.addLine(
                    new Line()
                            .text(Component.translatable("superresolution.screen.text.any"))
                            .left(0.02f)
                            .color(255, 255, 255, 255)
            );
        for (OS os : req.getSupportedOS()) {
            this.lineContainer.addLine(
                    new Line()
                            .text("%s %s".formatted(
                                    os.type.getString(),
                                    os.arch.getString()
                            ))
                            .left(0.02f)
                            .color(255, 255, 255, 255)
            );
        }
        this.lineContainer.addLine(
                new Line()
                        .text(
                                Component.translatable("superresolution.screen.info.text.only_in_dev_env").getString()
                                        .formatted(
                                                req.isRequiresDevEnv() ?
                                                        Component.translatable("superresolution.screen.text.yes").getString() :
                                                        Component.translatable("superresolution.screen.text.no").getString()
                                        )
                        )
                        .color(255, 255, 255, 255)
        );
        this.lineContainer.addLine(
                new Line()
                        .text(
                                Component.translatable("superresolution.screen.info.text.need_vulkan").getString()
                                        .formatted(
                                                req.isRequiresVulkan() ?
                                                        Component.translatable("superresolution.screen.text.yes").getString() :
                                                        Component.translatable("superresolution.screen.text.no").getString()
                                        )
                        )
                        .color(255, 255, 255, 255)
        );
        this.lineContainer.addLine(
                new Line()
                        .text(
                                Component.translatable("superresolution.screen.info.text.is_available").getString()
                                        .formatted(
                                                result.support() ?
                                                        Component.translatable("superresolution.screen.text.yes").getString() :
                                                        Component.translatable("superresolution.screen.text.no").getString()
                                        )
                        )
                        .color(
                                !result.support() ?
                                        ColorUtil.color(255, 255, 0, 0) :
                                        ColorUtil.color(255, 255, 255, 255)
                        )
        );

        this.lineContainer.addLine(
                new Line()
                        .text(
                                Component.translatable("superresolution.screen.info.text.requirement.os").getString()
                                        .formatted(
                                                result.osSupported() ?
                                                        Component.translatable("superresolution.screen.text.yes").getString() :
                                                        Component.translatable("superresolution.screen.text.no").getString()
                                        )
                        )
                        .left(0.02f)
                        .color(
                                !result.osSupported() ?
                                        ColorUtil.color(255, 255, 0, 0) :
                                        ColorUtil.color(255, 255, 255, 255)
                        )
        );
        this.lineContainer.addLine(
                new Line()
                        .text(
                                Component.translatable("superresolution.screen.info.text.requirement.env").getString()
                                        .formatted(
                                                result.environmentValid() ?
                                                        Component.translatable("superresolution.screen.text.yes").getString() :
                                                        Component.translatable("superresolution.screen.text.no").getString()
                                        )
                        )
                        .left(0.02f)
                        .color(
                                !result.environmentValid() ?
                                        ColorUtil.color(255, 255, 0, 0) :
                                        ColorUtil.color(255, 255, 255, 255)
                        )
        );
        this.lineContainer.addLine(
                new Line()
                        .text(
                                Component.translatable("superresolution.screen.info.text.requirement.opengl_ext").getString()
                                        .formatted(
                                                result.glExtensionsPresent() ?
                                                        Component.translatable("superresolution.screen.text.yes").getString() :
                                                        Component.translatable("superresolution.screen.text.no").getString()
                                        )
                        )
                        .left(0.02f)
                        .color(
                                !result.glExtensionsPresent() ?
                                        ColorUtil.color(255, 255, 0, 0) :
                                        ColorUtil.color(255, 255, 255, 255)
                        )
        );
        this.lineContainer.addLine(
                new Line()
                        .text(
                                Component.translatable("superresolution.screen.info.text.requirement.opengl_version").getString()
                                        .formatted(
                                                result.glVersionMet() ?
                                                        Component.translatable("superresolution.screen.text.yes").getString() :
                                                        Component.translatable("superresolution.screen.text.no").getString()
                                        )
                        )
                        .left(0.02f)
                        .color(
                                !result.glVersionMet() ?
                                        ColorUtil.color(255, 255, 0, 0) :
                                        ColorUtil.color(255, 255, 255, 255)
                        )
        );
        this.lineContainer.addLine(
                new Line()
                        .text(
                                Component.translatable("superresolution.screen.info.text.requirement.vulkan").getString()
                                        .formatted(
                                                result.vulkanAvailable() ?
                                                        Component.translatable("superresolution.screen.text.yes").getString() :
                                                        Component.translatable("superresolution.screen.text.no").getString()
                                        )
                        )
                        .left(0.02f)
                        .color(
                                !result.vulkanAvailable() ?
                                        ColorUtil.color(255, 255, 0, 0) :
                                        ColorUtil.color(255, 255, 255, 255)
                        )
        );

        this.lineContainer.addLine(
                new Line()
                        .text(
                                Component.translatable("superresolution.screen.info.text.requirement.vulkan_ext").getString()
                                        .formatted(
                                                result.vulkanDeviceExtensionsMet() ?
                                                        Component.translatable("superresolution.screen.text.yes").getString() :
                                                        Component.translatable("superresolution.screen.text.no").getString()
                                        )
                        )
                        .left(0.02f)
                        .color(
                                !result.vulkanDeviceExtensionsMet() ?
                                        ColorUtil.color(255, 255, 0, 0) :
                                        ColorUtil.color(255, 255, 255, 255)
                        )
        );
        this.lineContainer.addLine(
                new Line()
                        .text(
                                Component.translatable("superresolution.screen.info.text.requirement.vulkan_version").getString()
                                        .formatted(
                                                result.vulkanVersionMet() ?
                                                        Component.translatable("superresolution.screen.text.yes").getString() :
                                                        Component.translatable("superresolution.screen.text.no").getString()
                                        )
                        )
                        .left(0.02f)
                        .color(
                                !result.vulkanVersionMet() ?
                                        ColorUtil.color(255, 255, 0, 0) :
                                        ColorUtil.color(255, 255, 255, 255)
                        )
        );

        return this;
    }

    public interface LineContainer {
        void addLine(Line line);
    }
}
