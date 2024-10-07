package io.homo.superresolution.gui;

import io.homo.superresolution.SuperResolution;
import io.homo.superresolution.config.Config;
import io.homo.superresolution.upscale.AlgorithmManager;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.api.Requirement;
import me.shedaniel.clothconfig2.gui.entries.BooleanListEntry;
import me.shedaniel.clothconfig2.gui.entries.EnumListEntry;
import me.shedaniel.clothconfig2.gui.entries.IntegerSliderEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public class ConfigScreenBuilder {
    public static ConfigScreenBuilder create() {
        return new ConfigScreenBuilder();
    }

    public Screen build(Screen parentScreen) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parentScreen)
                .setTitle(Component.literal("超分辨率配置"))
                .setTransparentBackground(true)
                .setDefaultBackgroundTexture(new ResourceLocation("minecraft:textures/block/stone.png"));
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        ConfigCategory fsrCategory = builder.getOrCreateCategory(Component.literal("FSR2配置"));
        BooleanListEntry useUpscale = entryBuilder.startBooleanToggle(
                        Component.literal("启用超采样"),
                        !SuperResolution.notSupportFSR2
                )
                .setTooltip(Component.literal("开启后将使用当前所选算法对画面进行超采样以提高分辨率"))
                .build();
        IntegerSliderEntry upscaleRatio = entryBuilder.startIntSlider(
                        Component.literal("FSR2缩放比例"),
                        (int) (Config.getUpscaleRatio() * 1000),
                        300,
                        4000
                )
                .setTooltipSupplier((i) -> {
                    float f = (float) i / 1000;
                    return Optional.of(new Component[]{Component.literal(
                            "缩放比例\n" +
                                    "值越高渲染世界时的分辨率越低，画面越模糊，可以降低性能消耗以及石一般的画面\n" +
                                    "值越高渲染世界时的分辨率越高，画面就越清晰，可以获取更好的画面以及PPT般的流畅度\n" +
                                    "当前渲染分辨率：%sx%s\n".formatted(
                                            (int) (SuperResolution.getMinecraftWidth() / f),
                                            (int) (SuperResolution.getMinecraftHeight() / f)) +
                                    "当前渲染精度 " + (int) ((1 / f) * 100) + "%"
                    )});
                })
                .setTextGetter((i) -> Component.literal("缩放比例：" + String.format("%.2f", (float) i / 1000)))
                .setRequirement(Requirement.isTrue(useUpscale))
                .build();

        EnumListEntry<AlgorithmManager.AlgorithmType> algoType = entryBuilder.startEnumSelector(
                        Component.literal("超采样算法"),
                        AlgorithmManager.AlgorithmType.class,
                        AlgorithmManager.AlgorithmType.FSR1
                )
                .setEnumNameProvider((t)->{
                    if (t==AlgorithmManager.AlgorithmType.FSR2){
                        return Component.literal("FSR2 (正在开发)");
                    } else if (t==AlgorithmManager.AlgorithmType.FSR1) {
                        return Component.literal("FSR1");
                    } else if (t==AlgorithmManager.AlgorithmType.NONE) {
                        return Component.literal("无");
                    }
                    return Component.literal("???");
                })
                .setRequirement(Requirement.isTrue(useUpscale))
                .build();
        if (SuperResolution.notSupportFSR2) {
            Minecraft.getInstance().getToasts().addToast(
                    SystemToast.multiline(
                            Minecraft.getInstance(),
                            SystemToast.SystemToastIds.PERIODIC_NOTIFICATION,
                            Component.literal("警告"),
                            Component.literal("你的显卡不支持使用GL_KHR_shader_subgroup扩展， FSR2仅支持NVIDIA和AMD显卡，不支持核显，将自动禁用FSR2相关功能。")
                    )
            );
            fsrCategory.addEntry(
                    entryBuilder.startTextDescription(
                            Component.literal(
                                    "你的显卡不支持使用GL_KHR_shader_subgroup扩展， " +
                                            "FSR2仅支持NVIDIA和AMD显卡，" +
                                            "不支持核显，将自动禁用FSR2相关功" +
                                            "能。"
                            )
                    ).setColor(0xFF0000).build());
        }
        fsrCategory.addEntry(useUpscale);
        fsrCategory.addEntry(algoType);
        fsrCategory.addEntry(upscaleRatio);
        builder.setSavingRunnable(() -> {
            //Minecraft.getInstance().resizeDisplay();
            Config.setUpscaleRatio((float) upscaleRatio.getValue() / 1000);
            Config.setUpscaleAlgo(algoType.getValue());
        });
        return builder.build();
    }
}
