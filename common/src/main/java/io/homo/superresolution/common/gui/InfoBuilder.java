package io.homo.superresolution.common.gui;

import io.homo.superresolution.api.registry.AlgorithmDescription;
import io.homo.superresolution.api.utils.Requirement;
import io.homo.superresolution.common.gui.widgets.Line;
import io.homo.superresolution.common.platform.OS;
import io.homo.superresolution.common.platform.Platform;
import io.homo.superresolution.common.upscale.AlgorithmDescriptions;
import io.homo.superresolution.core.NativeLibManager;
import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.SuperResolutionNative;
import io.homo.superresolution.core.graphics.GraphicsCapabilities;
import io.homo.superresolution.core.utils.ColorUtil;
import net.minecraft.network.chat.Component;

import java.util.List;

public class InfoBuilder {

    private static final int COLOR_WHITE = ColorUtil.color(255, 255, 255, 255);
    private static final int COLOR_RED = ColorUtil.color(255, 255, 0, 0);

    private final LineContainer lineContainer;

    InfoBuilder(LineContainer lineContainer) {
        this.lineContainer = lineContainer;
    }

    public static InfoBuilder of(LineContainer lineContainer) {
        return new InfoBuilder(lineContainer);
    }

    public InfoBuilder addGlExt() {
        addCenteredLine("superresolution.screen.info.text.opengl_ext_count", GraphicsCapabilities.getGLExtensions().size());
        for (String ext : GraphicsCapabilities.getGLExtensions()) {
            addLine(ext);
        }
        return this;
    }

    public InfoBuilder addVkExt() {
        addCenteredLine("superresolution.screen.info.text.vulkan_ext_count", GraphicsCapabilities.getVulkanDeviceExtensions().size());
        for (String ext : GraphicsCapabilities.getVulkanDeviceExtensions()) {
            addLine(ext);
        }
        return this;
    }

    public InfoBuilder addEnvInfo() {
        addLine("superresolution.screen.info.text.mod_version", Platform.currentPlatform.getModVersionString("super_resolution"));
        addLine("superresolution.screen.info.text.native_lib_version",
                NativeLibManager.nativeApiAvailable() ? SuperResolutionNative.getVersionInfo() : "???");
        addLine("superresolution.screen.info.text.opengl_version",
                GraphicsCapabilities.getGLVersion()[0], GraphicsCapabilities.getGLVersion()[1]);
        addLine("superresolution.screen.info.text.vulkan_available",
                getYesNo(RenderSystems.isSupportVulkan()));
        addLine("superresolution.screen.info.text.vulkan_version",
                GraphicsCapabilities.getVulkanVersion()[0],
                GraphicsCapabilities.getVulkanVersion()[1],
                GraphicsCapabilities.getVulkanVersion()[2]);
        addLine("superresolution.screen.info.text.os_name",
                Platform.currentPlatform.getOS().type.getString());
        addLine("superresolution.screen.info.text.os_arch",
                Platform.currentPlatform.getOS().arch.getString());
        return this;
    }

    public InfoBuilder addAlgoInfo(AlgorithmDescription<?> algo) {
        if (algo.equals(AlgorithmDescriptions.NONE)) return this;

        lineContainer.addLine(new Line().text(algo.getDisplayName()).center(true).color(COLOR_WHITE));

        Requirement req = algo.getRequirement();
        Requirement.Result result = req.check();
        List<String> missingGlExt = req.getMissingGlExtensions();
        List<String> missingVkExt = req.getMissingVkExtensions();

        addLine("superresolution.screen.info.text.min_opengl_version",
                formatVersion(req.getGlMajorVersion()),
                formatVersion(req.getGlMinorVersion())
        );
        addLine("superresolution.screen.info.text.min_vulkan_version",
                formatVersion(req.getVulkanMajorVersion()),
                formatVersion(req.getVulkanMinorVersion()),
                formatVersion(req.getVulkanPatchVersion())
        );

        addRequirementList("superresolution.screen.info.text_need_opengl_ext", req.getRequiredGlExtensions(), missingGlExt);
        addRequirementList("superresolution.screen.info.text_need_vulkan_ext", req.getRequiredVulkanDeviceExtensions(), missingVkExt);

        addOsRequirement(req);

        addLine("superresolution.screen.info.text.only_in_dev_env", getYesNo(req.isRequiresDevEnv()));
        addLine("superresolution.screen.info.text.need_vulkan", getYesNo(req.isRequiresVulkan()));

        addAvailabilityStatus("superresolution.screen.info.text.is_available", result.support());

        addCheckLine("superresolution.screen.info.text.requirement.os", result.osSupported());
        addCheckLine("superresolution.screen.info.text.requirement.env", result.environmentValid());
        addCheckLine("superresolution.screen.info.text.requirement.opengl_ext", result.glExtensionsPresent());
        addCheckLine("superresolution.screen.info.text.requirement.opengl_version", result.glVersionMet());
        addCheckLine("superresolution.screen.info.text.requirement.vulkan", result.vulkanAvailable());
        addCheckLine("superresolution.screen.info.text.requirement.vulkan_ext", result.vulkanDeviceExtensionsMet());
        addCheckLine("superresolution.screen.info.text.requirement.vulkan_version", result.vulkanVersionMet());
        addCheckLine("superresolution.screen.info.text.requirement.additional_conditions", result.additionalConditionsMet());

        return this;
    }

    private void addOsRequirement(Requirement req) {
        addLine(Component.translatable("superresolution.screen.info.text.req_os_name_and_os_arch").getString());
        if (req.getSupportedOS().isEmpty()) {
            addIndentedLine(Component.translatable("superresolution.screen.text.any").getString());
        } else {
            for (OS os : req.getSupportedOS()) {
                addIndentedLine("%s %s".formatted(os.type.getString(), os.arch.getString()));
            }
        }
    }

    private void addRequirementList(String headerKey, Iterable<String> extensions, List<String> missing) {
        addLine(Component.translatable(headerKey).getString());
        if (!extensions.iterator().hasNext()) {
            addIndentedLine(Component.translatable("superresolution.screen.text.none").getString());
            return;
        }
        for (String ext : extensions) {
            boolean exists = !missing.contains(ext);
            addIndentedLine("%s (%s)".formatted(
                    ext,
                    exists ? Component.translatable("superresolution.screen.text.exist").getString()
                            : Component.translatable("superresolution.screen.text.missing").getString()
            ), exists ? COLOR_WHITE : COLOR_RED);
        }
    }

    private void addCheckLine(String key, boolean condition) {
        addIndentedLine(
                Component.translatable(key).getString().formatted(getYesNo(condition)),
                condition ? COLOR_WHITE : COLOR_RED
        );
    }

    private void addAvailabilityStatus(String key, boolean condition) {
        addLine(
                Component.translatable(key).getString().formatted(getYesNo(condition)),
                condition ? COLOR_WHITE : COLOR_RED
        );
    }

    private void addCenteredLine(String key, Object... args) {
        lineContainer.addLine(new Line()
                .text(Component.translatable(key).getString().formatted(args))
                .center(true)
                .color(COLOR_WHITE));
    }

    private void addLine(String key, Object... args) {
        lineContainer.addLine(new Line()
                .text(Component.translatable(key).getString().formatted(args))
                .color(COLOR_WHITE));
    }

    private void addLine(String text) {
        lineContainer.addLine(new Line().text(text).color(COLOR_WHITE));
    }

    private void addLine(String text, int color) {
        lineContainer.addLine(new Line().text(text).color(color));
    }

    private void addIndentedLine(String text) {
        addIndentedLine(text, COLOR_WHITE);
    }

    private void addIndentedLine(String text, int color) {
        lineContainer.addLine(new Line().text(text).left(0.02f).color(color));
    }

    private String getYesNo(boolean condition) {
        return Component.translatable(condition ?
                "superresolution.screen.text.yes" :
                "superresolution.screen.text.no").getString();
    }

    private String formatVersion(int ver) {
        return ver == -1 ? "*" : String.valueOf(ver);
    }

    private String formatVersion(int major, int minor, int patch) {
        return "%s.%s.%s".formatted(
                major == -1 ? "*" : major,
                minor == -1 ? "*" : minor,
                patch == -1 ? "*" : patch
        );
    }

    public interface LineContainer {
        void addLine(Line line);
    }
}
