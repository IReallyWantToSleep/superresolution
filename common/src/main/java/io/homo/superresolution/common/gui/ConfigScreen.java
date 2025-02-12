package io.homo.superresolution.common.gui;

import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.Config;
import io.homo.superresolution.common.config.ConfigFile;
import io.homo.superresolution.common.gui.options.OptionsList;
import io.homo.superresolution.common.gui.options.option.BooleanOption;
import io.homo.superresolution.common.gui.options.option.EnumData;
import io.homo.superresolution.common.gui.options.option.EnumOption;
import io.homo.superresolution.common.gui.options.option.SliderOption;
import io.homo.superresolution.common.gui.widgets.ButtonWidget;
import io.homo.superresolution.common.upscale.AlgorithmManager;
import io.homo.superresolution.common.upscale.AlgorithmType;
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
    private ButtonWidget infoButton;
    private Rect guiRect;

    protected ConfigScreen(Screen lastScreen) {
        super(Component.translatable("superresolution.screen.config.name"));
        this.lastScreen = lastScreen;
        this.optionsList = new OptionsList(0, 0, 100, 100);
        this.addOptions();
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.optionsList.render(guiGraphics, mouseX, mouseY, partialTick);
        this.closeButton.render(guiGraphics, mouseX, mouseY, partialTick);
        this.okButton.render(guiGraphics, mouseX, mouseY, partialTick);
        this.saveButton.render(guiGraphics, mouseX, mouseY, partialTick);
        this.infoButton.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, Component.translatable("superresolution.screen.config.name"), this.width / 2, 6, FastColor.ARGB32.color(255, 255, 255, 255));
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

    public void onClickInfo() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(new InfoScreen(this.lastScreen, true));
        }
    }

    @Override
    public void init() {
        this.okButton = (ButtonWidget) new ButtonWidget(this.width - 50 - 10, this.height - 20 - 10, 50, 20)
                .setAction(this::onClickOK)
                .setLabel(Component.translatable("superresolution.screen.button.label.done"))
                .setTooltip(Tooltip.create(Component.translatable("superresolution.screen.button.tooltip.done")));
        this.addWidget(this.okButton);
        this.closeButton = (ButtonWidget) new ButtonWidget(this.width - 50 - 10 - 50 - 10, this.height - 20 - 10, 50, 20)
                .setAction(this::onClickClose)
                .setLabel(Component.translatable("superresolution.screen.button.label.close"))
                .setTooltip(Tooltip.create(Component.translatable("superresolution.screen.button.tooltip.close")));
        this.addWidget(this.closeButton);
        this.saveButton = (ButtonWidget) new ButtonWidget(this.width - 50 - 10 - 50 - 10 - 50 - 10, this.height - 20 - 10, 50, 20)
                .setAction(this::onClickSave)
                .setLabel(Component.translatable("superresolution.screen.button.label.apply"))
                .setTooltip(Tooltip.create(Component.translatable("superresolution.screen.button.tooltip.apply")));
        this.addWidget(this.saveButton);
        this.infoButton = new ButtonWidget(10, this.height - 20 - 10, 30, 20)
                .setAction(this::onClickInfo)
                .setLabel(Component.literal("信息"));
        this.addWidget(this.infoButton);
        this.guiRect = new Rect(10, 18, this.width - 10, this.height - 40);
        this.optionsList.setPosition(this.guiRect.x, this.guiRect.y);
        this.optionsList.resize(this.guiRect.width, this.guiRect.height);
        this.addWidget(this.optionsList);
        ((SliderOption) this.optionsList.getOption("upscaleRatio")).setMin(Config.getMinUpscaleRatio());
    }

    private void addOptions() {
        Config.fromData(ConfigFile.read());
        BooleanOption useUpscale = (BooleanOption) new BooleanOption()
                .setLabel(Component.translatable("superresolution.screen.config.options.label.enable_upscale"))
                .setKey("useUpscale")
                .setTooltip(Tooltip.create(Component.translatable("superresolution.screen.config.options.tooltip.enable_upscale")));

        SliderOption upscaleRatio = (SliderOption) new SliderOption()
                .setMin(Config.getMinUpscaleRatio())
                .setMax(4)
                .setStep(0.01)
                .setKey("upscaleRatio")
                .setLabel(Component.translatable("superresolution.screen.config.options.label.upscale_ratio"));
        upscaleRatio.setOnChange((value) -> {
            upscaleRatio.setTooltip(Tooltip.create(Component.literal(Component.translatable("superresolution.screen.config.options.tooltip.upscale_ratio").getString().formatted(
                    (int) (SuperResolution.getMinecraftWidth() / value),
                    (int) (SuperResolution.getMinecraftHeight() / value),
                    (int) ((1 / value) * 100) + "%"
            ))));
        });
        upscaleRatio.setValue((double) Config.getUpscaleRatio());
        SliderOption sharpnessRatio = (SliderOption) new SliderOption()
                .setMin(0)
                .setMax(2)
                .setStep(0.01)
                .setKey("sharpnessRatio")
                .setLabel(Component.translatable("superresolution.screen.config.options.label.sharpness"))
                .setTooltip(Tooltip.create(Component.translatable("superresolution.screen.config.options.tooltip.sharpness")));
        EnumOption algoType = (EnumOption) new EnumOption(
                new EnumData()
                        .addEnum(new EnumData.EnumInfo<>()
                                .setDisplayName(Component.translatable("superresolution.algo.display_name.none"))
                                .setValue(AlgorithmType.NONE)
                                .setKey("none")
                        )
                        .addEnum(AlgorithmManager.isSupportAlgorithm(AlgorithmType.FSR1) ? new EnumData.EnumInfo<>()
                                .setDisplayName(Component.translatable("superresolution.algo.display_name.fsr1"))
                                .setValue(AlgorithmType.FSR1)
                                .setKey("fsr1") : null
                        )
                        .addEnum(AlgorithmManager.isSupportAlgorithm(AlgorithmType.NIS) ? new EnumData.EnumInfo<>()
                                .setDisplayName(Component.translatable("superresolution.algo.display_name.nis"))
                                .setValue(AlgorithmType.NIS)
                                .setKey("nis") : null
                        )
                        .addEnum(AlgorithmManager.isSupportAlgorithm(AlgorithmType.FSR2) ? new EnumData.EnumInfo<>()
                                .setDisplayName(Component.translatable("superresolution.algo.display_name.fsr2"))
                                .setValue(AlgorithmType.FSR2)
                                .setKey("fsr2") : null
                        )
        ).setKey("algoType").setLabel(Component.translatable("superresolution.screen.config.options.label.algo_type"));

        if (!AlgorithmManager.isSupportAlgorithm(AlgorithmType.FSR2)) {
            Minecraft.getInstance().getToasts().addToast(
                    SystemToast.multiline(
                            Minecraft.getInstance(),
                            SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                            Component.translatable("superresolution.screen.warn"),
                            Component.translatable("superresolution.algo.not_support.fsr2")
                    )
            );
        }
        if (!AlgorithmManager.isSupportAlgorithm(AlgorithmType.FSR1)) {
            Minecraft.getInstance().getToasts().addToast(
                    SystemToast.multiline(
                            Minecraft.getInstance(),
                            SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                            Component.translatable("superresolution.screen.warn"),
                            Component.translatable("superresolution.algo.not_support.fsr1")
                    )
            );
        }
        if (!AlgorithmManager.isSupportAlgorithm(AlgorithmType.NIS)) {
            Minecraft.getInstance().getToasts().addToast(
                    SystemToast.multiline(
                            Minecraft.getInstance(),
                            SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                            Component.translatable("superresolution.screen.warn"),
                            Component.translatable("superresolution.algo.not_support.nis")
                    )
            );
        }
        if (!AlgorithmManager.isSupportAlgorithm(AlgorithmType.NONE)) {
            Minecraft.getInstance().getToasts().addToast(
                    SystemToast.multiline(
                            Minecraft.getInstance(),
                            SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                            Component.translatable("superresolution.screen.warn"),
                            Component.translatable("superresolution.algo.not_support.none")
                    )
            );
        }

        this.optionsList.setSavingRunnable((data) -> {
            Config.setEnableUpscale(useUpscale.getValue());
            Config.setUpscaleRatio(upscaleRatio.getValue().floatValue());
            Config.setUpscaleAlgo((AlgorithmType) algoType.getValue().value);
            Config.setSharpness(sharpnessRatio.getValue().floatValue());
            Minecraft.getInstance().resizeDisplay();
            ConfigFile.write();
        });
        useUpscale.setValue(Config.isEnableUpscale());
        algoType.setValue(algoType.getEnumData().getEnum(Config.getUpscaleAlgo()));
        upscaleRatio.setValue((double) Config.getUpscaleRatio());
        sharpnessRatio.setValue((double) Config.getSharpness());
        this.optionsList.addOption(useUpscale);
        this.optionsList.addOption(algoType);
        this.optionsList.addOption(upscaleRatio);
        this.optionsList.addOption(sharpnessRatio);
    }
}
