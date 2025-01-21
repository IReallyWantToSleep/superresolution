package io.homo.superresolution.gui;

import io.homo.superresolution.SuperResolution;
import io.homo.superresolution.config.Config;
import io.homo.superresolution.config.ConfigFile;
import io.homo.superresolution.gui.options.OptionsList;
import io.homo.superresolution.gui.options.option.BooleanOption;
import io.homo.superresolution.gui.options.option.EnumData;
import io.homo.superresolution.gui.options.option.EnumOption;
import io.homo.superresolution.gui.options.option.SliderOption;
import io.homo.superresolution.gui.widgets.ButtonWidget;
import io.homo.superresolution.render.MinecraftRenderingStates;
import io.homo.superresolution.upscale.AlgorithmManager;
import io.homo.superresolution.upscale.AlgorithmType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FastColor;
import org.jetbrains.annotations.NotNull;


public class ConfigScreen extends Screen {
    private final OptionsList optionsList;
    private final Screen lastScreen;
    private ButtonWidget closeButton;
    private ButtonWidget okButton;
    private ButtonWidget saveButton;
    private Rect guiRect;

    protected ConfigScreen(Screen lastScreen) {
        super(Component.literal("dsfgdfsfsdf"));
        this.lastScreen = lastScreen;
        this.optionsList = new OptionsList(0, 0, 100, 100);
        this.addTestOption();
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.optionsList.render(guiGraphics, mouseX, mouseY, partialTick);
        this.closeButton.render(guiGraphics, mouseX, mouseY, partialTick);
        this.okButton.render(guiGraphics, mouseX, mouseY, partialTick);
        this.saveButton.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, "超分辨率配置", this.width / 2, 6, FastColor.ARGB32.color(255, 255, 255, 255));
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
        guiGraphics.fill(guiRect.x, guiRect.y, guiRect.width, guiRect.height, FastColor.ARGB32.color(100, 0, 0, 0));
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(lastScreen);
        }
    }

    public void onClickOK() {
        this.optionsList.save();
        this.onClose();
    }

    public void onClickClose() {
        this.onClose();
    }

    public void onClickSave() {
        this.optionsList.save();
    }

    @Override
    public void init() {
        this.okButton = new ButtonWidget(this.width - 50 - 10, this.height - 20 - 10, 50, 20)
                .setAction(this::onClickOK)
                .setLabel("完成");
        this.addWidget(this.okButton);
        this.closeButton = new ButtonWidget(this.width - 50 - 10 - 50 - 10, this.height - 20 - 10, 50, 20)
                .setAction(this::onClickClose)
                .setLabel("关闭");
        this.addWidget(this.closeButton);
        this.saveButton = new ButtonWidget(this.width - 50 - 10 - 50 - 10 - 50 - 10, this.height - 20 - 10, 50, 20)
                .setAction(this::onClickSave)
                .setLabel("应用");
        this.addWidget(this.saveButton);
        this.guiRect = new Rect(10, 18, this.width - 10, this.height - 40);
        this.optionsList.setPosition(this.guiRect.x, this.guiRect.y);
        this.optionsList.resize(this.guiRect.width, this.guiRect.height);
        this.addWidget(this.optionsList);
        ((SliderOption) this.optionsList.getOption("upscaleRatio")).setMin(Config.getMinUpscaleRatio());
    }

    private void addTestOption() {
        Config.fromData(ConfigFile.read());
        BooleanOption useUpscale = (BooleanOption) new BooleanOption()
                .setLabel("启用超分辨率")
                .setKey("useUpscale")
                .setTooltip(Tooltip.create(Component.literal("开启后将使用当前所选算法对画面进行超分辨率")));

        SliderOption upscaleRatio = (SliderOption) new SliderOption()
                .setMin(Config.getMinUpscaleRatio())
                .setMax(4)
                .setStep(0.01)
                .setKey("upscaleRatio")
                .setLabel("超分辨率比例");
        upscaleRatio.setOnChange((value) -> {
            upscaleRatio.setTooltip(Tooltip.create(Component.literal(
                    "缩放比例\n" +
                            "值越高渲染世界时的分辨率越低，画面越模糊，可以降低性能消耗以及屎一般的画面\n" +
                            "值越高渲染世界时的分辨率越高，画面就越清晰，可以获取更好的画面以及PPT般的流畅度\n" +
                            "当前渲染分辨率：%sx%s\n".formatted(
                                    (int) (SuperResolution.getMinecraftWidth() / value),
                                    (int) (SuperResolution.getMinecraftHeight() / value)) +
                            "当前渲染精度 " + (int) ((1 / value) * 100) + "%\n" +
                            "建议值：1.4~1.8"
            )));
        });
        upscaleRatio.setValue((double) Config.getUpscaleRatio());
        SliderOption sharpnessRatio = (SliderOption) new SliderOption()
                .setMin(0)
                .setMax(2)
                .setStep(0.01)
                .setKey("sharpnessRatio")
                .setLabel("锐度")
                .setTooltip(Tooltip.create(Component.literal("值越低画面越物体边缘越明显\n值越高远处物体越模糊\n建议值：0.55左右")));
        EnumOption algoType = (EnumOption) new EnumOption(
                new EnumData()
                        .addEnum(new EnumData.EnumInfo<>()
                                .setDisplayName("无")
                                .setValue(AlgorithmType.NONE)
                                .setKey("none")
                        )
                        .addEnum(AlgorithmManager.isSupportAlgorithm(AlgorithmType.FSR1) ? new EnumData.EnumInfo<>()
                                .setDisplayName("FSR1")
                                .setValue(AlgorithmType.FSR1)
                                .setKey("fsr1") : null
                        )
                        .addEnum(AlgorithmManager.isSupportAlgorithm(AlgorithmType.FSR2) ? new EnumData.EnumInfo<>()
                                .setDisplayName("FSR2 (正在开发) (不稳定)")
                                .setValue(AlgorithmType.FSR2)
                                .setKey("fsr2") : null
                        )
        ).setKey("algoType").setLabel("超分辨率算法");
        if (!AlgorithmManager.isSupportAlgorithm(AlgorithmType.FSR2)) {
            Minecraft.getInstance().getToasts().addToast(
                    SystemToast.multiline(
                            Minecraft.getInstance(),
                            SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                            Component.literal("警告"),
                            Component.literal("你的显卡不支持使用GL_KHR_shader_subgroup扩展，将自动禁用FSR2相关功能。")
                    )
            );
        }
        this.optionsList.setSavingRunnable((data) -> {
            Config.setEnableUpscale(useUpscale.getValue());
            Config.setUpscaleRatio(upscaleRatio.getValue().floatValue());
            Config.setUpscaleAlgo((AlgorithmType) algoType.getValue().value);
            Config.setSharpness(sharpnessRatio.getValue().floatValue());
            MinecraftRenderingStates.onResolutionChanged();
            MinecraftRenderingStates.resizeMinecraftRenderTarget();
            ConfigFile.write();
        });
        useUpscale.setValue(Config.enableUpscale);
        algoType.setValue(algoType.getEnumData().getEnum(Config.getUpscaleAlgo()));
        upscaleRatio.setValue((double) Config.getUpscaleRatio());
        sharpnessRatio.setValue((double) Config.getSharpness());
        this.optionsList.addOption(useUpscale);
        this.optionsList.addOption(algoType);
        this.optionsList.addOption(upscaleRatio);
        this.optionsList.addOption(sharpnessRatio);
    }
}
