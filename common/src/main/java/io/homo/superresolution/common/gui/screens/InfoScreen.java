package io.homo.superresolution.common.gui.screens;

import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.gui.BaseScreen;
import io.homo.superresolution.common.gui.ConfigScreenBuilder;
import io.homo.superresolution.common.gui.Rectangle;
import io.homo.superresolution.common.gui.widgets.ButtonWidget;
import io.homo.superresolution.common.gui.widgets.Line;
import io.homo.superresolution.common.gui.widgets.TextWidget;
import io.homo.superresolution.common.platform.OS;
import io.homo.superresolution.common.platform.Platform;
import io.homo.superresolution.common.upscale.AlgorithmType;
import io.homo.superresolution.common.upscale.utils.AlgorithmHelper;
import io.homo.superresolution.common.upscale.utils.NativeLibManager;
import io.homo.superresolution.common.upscale.utils.Requirement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Arrays;

public class InfoScreen extends BaseScreen {
    private final Screen lastScreen;
    private final boolean openConfigScreen;
    private ButtonWidget closeButton;
    private Rectangle infoTextRect;
    private TextWidget infoText;
    private int showInfo = 0;
    private ButtonWidget extButton;

    public InfoScreen(Screen lastScreen, boolean openConfigScreen) {
        super(Component.literal(""));
        this.lastScreen = lastScreen;
        this.openConfigScreen = openConfigScreen;
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            if (this.openConfigScreen) {
                this.minecraft.setScreen(ConfigScreenBuilder.create().build(lastScreen));
            } else {
                this.minecraft.setScreen(lastScreen);
            }
        }
    }

    public void onClickInfo() {
        if (showInfo == 0) {
            this.infoText.lines.clear();
            this.showInfo = 1;
            this.addGlExt();
        } else {
            this.infoText.lines.clear();
            this.showInfo = 0;
            this.addAllInfo();
        }
    }

    @Override
    public void init() {
        this.closeButton = new ButtonWidget(this.width - 50 - 10, this.height - 20 - 10, 50, 20)
                .setAction(this::onClose)
                .setLabel(Component.translatable("superresolution.screen.button.label.close"));
        this.addWidget(this.closeButton);
        this.extButton = new ButtonWidget(10, this.height - 20 - 10, 60, 20)
                .setAction(this::onClickInfo)
                .setLabel(Component.translatable("superresolution.screen.info.button.label.opengl_ext_info"));
        this.addWidget(this.extButton);
        this.infoTextRect = new Rectangle(10, 18, this.width - 10, this.height - 40);
        this.infoText = new TextWidget(infoTextRect.x, infoTextRect.y, infoTextRect.width, infoTextRect.height);
        this.addWidget(this.infoText);
        this.infoText.border.x = 3;
        this.infoText.border.y = 3;
        if (showInfo == 0) {
            addAllInfo();
        } else if (showInfo == 1) {
            addGlExt();
        }
    }

    public void addGlExt() {
        this.infoText.addLine(
                new Line()
                        .text(Component.translatable("superresolution.screen.info.text.opengl_ext_count").getString()
                                .formatted(AlgorithmHelper.GLExtension.size())
                        )
                        .center(true)
                        .color(255, 255, 255, 255)
        );
        for (String ext : AlgorithmHelper.GLExtension) {
            this.infoText.addLine(
                    new Line()
                            .text(ext)
                            .color(255, 255, 255, 255)
            );
        }
    }

    public void addAllInfo() {
        addEnvInfo();
        this.infoText.addLine(
                new Line()
                        .text(Component.translatable("superresolution.screen.info.text.algo_support_status"))
                        .center(true)
                        .scale(1.15f)
                        .color(255, 255, 255, 255)
        );
        this.infoText.addLine(
                new Line()
                        .type(Line.LineType.Divider)
                        .color(255, 255, 255, 255)
        );
        for (AlgorithmType algorithmType : Arrays.stream(AlgorithmType.values()).toList()) {
            addAlgoInfo(algorithmType);
        }
    }

    public void addEnvInfo() {
        this.infoText.addLine(
                new Line()
                        .text(Component.translatable("superresolution.screen.info.title.env_info"))
                        .center(true)
                        .scale(1.15f)
                        .color(255, 255, 255, 255)
        );
        this.infoText.addLine(
                new Line()
                        .type(Line.LineType.Divider)
                        .color(255, 255, 255, 255)
        );
        this.infoText.addLine(
                new Line()
                        .text(
                                Component.translatable("superresolution.screen.info.text.mod_version").getString()
                                        .formatted(Platform.currentPlatform.getModVersionString("super_resolution"))
                        )
                        .color(255, 255, 255, 255)
        );
        this.infoText.addLine(
                new Line()
                        .text(
                                Component.translatable("superresolution.screen.info.text.native_lib_version").getString()
                                        .formatted(NativeLibManager.nativeApi.getVersionInfo())
                        )
                        .color(255, 255, 255, 255)
        );
        this.infoText.addLine(
                new Line()
                        .text(
                                Component.translatable("superresolution.screen.info.text.opengl_version").getString()
                                        .formatted(
                                                AlgorithmHelper.GLVersion[0],
                                                AlgorithmHelper.GLVersion[1]
                                        )
                        )
                        .color(255, 255, 255, 255)
        );
        this.infoText.addLine(
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
        this.infoText.addLine(
                new Line()
                        .text(
                                Component.translatable(
                                                "superresolution.screen.info.text.vulkan_version").getString()
                                        .formatted(
                                                AlgorithmHelper.VkVersion[0],
                                                AlgorithmHelper.VkVersion[1],
                                                AlgorithmHelper.VkVersion[2]
                                        )
                        )
                        .color(255, 255, 255, 255)
        );
        this.infoText.addLine(
                new Line()
                        .text(
                                Component.translatable("superresolution.screen.info.text.os_name").getString()
                                        .formatted(
                                                Platform.currentPlatform.getOS().type.getString()
                                        )
                        )
                        .color(255, 255, 255, 255)
        );
        this.infoText.addLine(
                new Line()
                        .text(
                                Component.translatable("superresolution.screen.info.text.os_arch").getString()
                                        .formatted(
                                                Platform.currentPlatform.getOS().arch.getString()
                                        )
                        )
                        .color(255, 255, 255, 255)
        );
    }

    public void addAlgoInfo(AlgorithmType algo) {
        if (algo == AlgorithmType.NONE) return;
        this.infoText.addLine(new Line().text(algo.getString()).center(true).color(255, 255, 255, 255));
        this.infoText.addLine(
                new Line()
                        .type(Line.LineType.Divider)
                        .color(255, 255, 255, 255)
        );
        Requirement req = algo.getValue();
        Requirement.Result result = req.check();
        ArrayList<String> missingGlExtension = req.getMissingExtension();
        this.infoText.addLine(
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
        this.infoText.addLine(
                new Line()
                        .text(Component.translatable("superresolution.screen.info.text_need_opengl_ext"))
                        .color(255, 255, 255, 255)
        );
        if (req.getIncludeExtension().isEmpty())
            this.infoText.addLine(
                    new Line()
                            .text(Component.translatable("superresolution.screen.text.none"))
                            .left(0.02f)
                            .color(255, 255, 255, 255)
            );
        for (String glExt : req.getIncludeExtension()) {
            this.infoText.addLine(
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
        this.infoText.addLine(
                new Line()
                        .text(Component.translatable("superresolution.screen.info.text.req_os_name_and_os_arch"))
                        .color(255, 255, 255, 255)
        );
        if (req.getIncludeOS().isEmpty())
            this.infoText.addLine(
                    new Line()
                            .text(Component.translatable("superresolution.screen.text.any"))
                            .left(0.02f)
                            .color(255, 255, 255, 255)
            );
        for (OS os : req.getIncludeOS()) {
            this.infoText.addLine(
                    new Line()
                            .text("%s %s".formatted(
                                    os.type.getString(),
                                    os.arch.getString()
                            ))
                            .left(0.02f)
                            .color(255, 255, 255, 255)
            );
        }
        this.infoText.addLine(
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
        this.infoText.addLine(
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
        this.infoText.addLine(
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

        this.infoText.addLine(
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
        this.infoText.addLine(
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
        this.infoText.addLine(
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
        this.infoText.addLine(
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
        this.infoText.addLine(
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
        this.infoText.addLine(
                new Line()
                        .type(Line.LineType.Divider)
                        .color(255, 255, 255, 255)
        );

    }

    @Override
    public void renderMain(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (showInfo == 0) {
            this.extButton.setLabel(Component.translatable("superresolution.screen.info.button.label.opengl_ext_info"));
        } else {
            this.extButton.setLabel(Component.translatable("superresolution.screen.button.label.return"));
        }
        this.extButton.getRect().width = this.font.width(this.extButton.getLabel()) + 20;
        this.infoText.render(guiGraphics, mouseX, mouseY, partialTick);
        this.extButton.render(guiGraphics, mouseX, mouseY, partialTick);
        this.closeButton.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected Rectangle getGuiRect() {
        return infoTextRect;
    }
}
