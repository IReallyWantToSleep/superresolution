package io.homo.superresolution.common.gui;

import io.homo.superresolution.api.registry.AlgorithmDescription;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.gui.widgets.Line;
import io.homo.superresolution.common.platform.OS;
import io.homo.superresolution.common.platform.Platform;
import io.homo.superresolution.common.render.GraphicsCapabilities;
import io.homo.superresolution.common.upscale.AlgorithmDescriptions;
import oiiaio.fsr.NativeLibManager;
import io.homo.superresolution.api.utils.Requirement;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;

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
                                        .formatted(NativeLibManager.nativeApiAvailable() ? NativeLibManager.getNativeApi().getVersionInfo() : "???")
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
                                                SuperResolution.interopManager.supportVulkan ?
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
        ArrayList<String> missingGlExtension = req.getMissingExtension();
        this.lineContainer.addLine(
                new Line()
                        .text(
                                Component.translatable("superresolution.screen.info.text.mix_opengl_version").getString()
                                        .formatted(
                                                req.getGlMajorVersion() == -1 ? "*" : req.getGlMajorVersion(),
                                                req.getGlMinorVersion() == -1 ? "*" : req.getGlMinorVersion()
                                        )
                        )
                        .color(255, 255, 255, 255)
        );
        this.lineContainer.addLine(
                new Line()
                        .text(Component.translatable("superresolution.screen.info.text_need_opengl_ext"))
                        .color(255, 255, 255, 255)
        );
        if (req.getIncludeExtension().isEmpty())
            this.lineContainer.addLine(
                    new Line()
                            .text(Component.translatable("superresolution.screen.text.none"))
                            .left(0.02f)
                            .color(255, 255, 255, 255)
            );
        for (String glExt : req.getIncludeExtension()) {
            this.lineContainer.addLine(
                    new Line()
                            .text("%s (%s)".formatted(
                                    glExt,
                                    !missingGlExtension.contains(glExt) ?
                                            Component.translatable("superresolution.screen.text.exist").getString() :
                                            Component.translatable("superresolution.screen.text.missing").getString()
                            ))
                            .left(0.02f)
                            .color(255, 255, 255, 255)
            );
        }
        this.lineContainer.addLine(
                new Line()
                        .text(Component.translatable("superresolution.screen.info.text.req_os_name_and_os_arch"))
                        .color(255, 255, 255, 255)
        );
        if (req.getIncludeOS().isEmpty())
            this.lineContainer.addLine(
                    new Line()
                            .text(Component.translatable("superresolution.screen.text.any"))
                            .left(0.02f)
                            .color(255, 255, 255, 255)
            );
        for (OS os : req.getIncludeOS()) {
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
                                                req.isDevelopmentEnvironment() ?
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
                                                req.isRequireVulkan() ?
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
                        .color(255, 255, 255, 255)
        );

        this.lineContainer.addLine(
                new Line()
                        .text(
                                Component.translatable("superresolution.screen.info.text.requirement.os").getString()
                                        .formatted(
                                                result.os() ?
                                                        Component.translatable("superresolution.screen.text.yes").getString() :
                                                        Component.translatable("superresolution.screen.text.no").getString()
                                        )
                        )
                        .left(0.02f)
                        .color(255, 255, 255, 255)
        );
        this.lineContainer.addLine(
                new Line()
                        .text(
                                Component.translatable("superresolution.screen.info.text.requirement.env").getString()
                                        .formatted(
                                                result.env() ?
                                                        Component.translatable("superresolution.screen.text.yes").getString() :
                                                        Component.translatable("superresolution.screen.text.no").getString()
                                        )
                        )
                        .left(0.02f)
                        .color(255, 255, 255, 255)
        );
        this.lineContainer.addLine(
                new Line()
                        .text(
                                Component.translatable("superresolution.screen.info.text.requirement.opengl_ext").getString()
                                        .formatted(
                                                result.glExtension() ?
                                                        Component.translatable("superresolution.screen.text.yes").getString() :
                                                        Component.translatable("superresolution.screen.text.no").getString()
                                        )
                        )
                        .left(0.02f)
                        .color(255, 255, 255, 255)
        );
        this.lineContainer.addLine(
                new Line()
                        .text(
                                Component.translatable("superresolution.screen.info.text.requirement.opengl_version").getString()
                                        .formatted(
                                                result.glVersion() ?
                                                        Component.translatable("superresolution.screen.text.yes").getString() :
                                                        Component.translatable("superresolution.screen.text.no").getString()
                                        )
                        )
                        .left(0.02f)
                        .color(255, 255, 255, 255)
        );
        this.lineContainer.addLine(
                new Line()
                        .text(
                                Component.translatable("superresolution.screen.info.text.requirement.vulkan").getString()
                                        .formatted(
                                                result.vulkan() ?
                                                        Component.translatable("superresolution.screen.text.yes").getString() :
                                                        Component.translatable("superresolution.screen.text.no").getString()
                                        )
                        )
                        .left(0.02f)
                        .color(255, 255, 255, 255)
        );
        return this;
    }

    public interface LineContainer {
        void addLine(Line line);
    }
}
