package io.homo.superresolution.common.gui;

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
import net.minecraft.util.FastColor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;

public class InfoScreen extends Screen {
    private final Screen lastScreen;
    private final boolean openConfigScreen;
    private ButtonWidget closeButton;
    private Rect infoTextRect;
    private TextWidget infoText;
    private int showInfo = 0;
    private ButtonWidget extButton;

    protected InfoScreen(Screen lastScreen, boolean openConfigScreen) {
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
                .setLabel(Component.literal("OpenGL扩展信息"));
        this.addWidget(this.extButton);
        this.infoTextRect = new Rect(10, 18, this.width - 10, this.height - 40);
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
                        .text("共有 %s 个扩展".formatted(AlgorithmHelper.GLExtension.size()))
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
                        .text("各算法支持情况")
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
                        .text("环境信息")
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
                        .text("模组版本: %s".formatted(Platform.currentPlatform.getModVersionString("super_resolution")))
                        .color(255, 255, 255, 255)
        );
        this.infoText.addLine(
                new Line()
                        .text("依赖库版本: %s".formatted(NativeLibManager.nativeApi.getVersionInfo()))
                        .color(255, 255, 255, 255)
        );
        this.infoText.addLine(
                new Line()
                        .text("OpenGL版本: %s.%s".formatted(AlgorithmHelper.GLVersion[0], AlgorithmHelper.GLVersion[1]))
                        .color(255, 255, 255, 255)
        );
        this.infoText.addLine(
                new Line()
                        .text("Vulkan版本: %s.%s.%s".formatted(AlgorithmHelper.VkVersion[0], AlgorithmHelper.VkVersion[1], AlgorithmHelper.VkVersion[2]))
                        .color(255, 255, 255, 255)
        );
        this.infoText.addLine(
                new Line()
                        .text("系统: %s".formatted(Platform.currentPlatform.getOS().type.getString()))
                        .color(255, 255, 255, 255)
        );
        this.infoText.addLine(
                new Line()
                        .text("系统架构: %s".formatted(Platform.currentPlatform.getOS().arch.getString()))
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
                        .text("最低OpenGL版本: %s.%s".formatted(
                                req.getGlMajorVersion() == -1 ? "*" : req.getGlMajorVersion(),
                                req.getGlMinorVersion() == -1 ? "*" : req.getGlMinorVersion()
                        ))
                        .color(255, 255, 255, 255)
        );
        this.infoText.addLine(
                new Line()
                        .text("需要的OpenGL扩展")
                        .color(255, 255, 255, 255)
        );
        if (req.getIncludeExtension().isEmpty())
            this.infoText.addLine(
                    new Line()
                            .text("无")
                            .left(0.02f)
                            .color(255, 255, 255, 255)
            );
        for (String glExt : req.getIncludeExtension()) {
            this.infoText.addLine(
                    new Line()
                            .text("%s (%s)".formatted(
                                    glExt,
                                    !missingGlExtension.contains(glExt) ? "存在" : "缺少"
                            ))
                            .left(0.02f)
                            .color(255, 255, 255, 255)
            );
        }
        this.infoText.addLine(
                new Line()
                        .text("要求的系统与架构")
                        .color(255, 255, 255, 255)
        );
        if (req.getIncludeOS().isEmpty())
            this.infoText.addLine(
                    new Line()
                            .text("任意")
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
                        .text("仅在开发环境中可用? %s".formatted(req.isDevelopmentEnvironment() ? "是" : "否"))
                        .color(255, 255, 255, 255)
        );
        this.infoText.addLine(
                new Line()
                        .text("当前是否支持 %s".formatted(result.support() ? "是" : "否"))
                        .color(255, 255, 255, 255)
        );

        this.infoText.addLine(
                new Line()
                        .text("系统 %s".formatted(result.os() ? "是" : "否"))
                        .left(0.02f)
                        .color(255, 255, 255, 255)
        );
        this.infoText.addLine(
                new Line()
                        .text("环境 %s".formatted(result.env() ? "是" : "否"))
                        .left(0.02f)
                        .color(255, 255, 255, 255)
        );
        this.infoText.addLine(
                new Line()
                        .text("OpenGL扩展 %s".formatted(result.glExtension() ? "是" : "否"))
                        .left(0.02f)
                        .color(255, 255, 255, 255)
        );
        this.infoText.addLine(
                new Line()
                        .text("OpenGL版本 %s".formatted(result.glVersion() ? "是" : "否"))
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
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        if (showInfo == 0) {
            this.extButton.setLabel("OpenGL扩展信息");
        } else {
            this.extButton.setLabel("关闭");
        }
        this.infoText.render(guiGraphics, mouseX, mouseY, partialTick);
        this.extButton.render(guiGraphics, mouseX, mouseY, partialTick);
        this.closeButton.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (this.minecraft != null) {
            if (this.minecraft.level == null) {
                this.renderPanorama(guiGraphics, partialTick);
                this.renderBlurredBackground(partialTick);
                this.renderMenuBackground(guiGraphics);
            }
        }
        guiGraphics.fill(infoTextRect.x, infoTextRect.y, infoTextRect.width, infoTextRect.height, FastColor.ARGB32.color(100, 0, 0, 0));
    }
}
