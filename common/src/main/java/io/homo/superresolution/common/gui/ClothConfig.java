package io.homo.superresolution.common.gui;

import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.ConfigFile;
import io.homo.superresolution.common.config.enums.CaptureMode;
import io.homo.superresolution.common.config.special.SpecialConfig;
import io.homo.superresolution.common.config.special.SpecialConfigDescription;
import io.homo.superresolution.common.config.Config;
import io.homo.superresolution.common.debug.PerformanceInfo;
import io.homo.superresolution.common.gui.screens.InfoScreen;
import io.homo.superresolution.common.gui.widgets.ClothButtonEntry;
import io.homo.superresolution.common.gui.widgets.ClothTextListEntry;
import io.homo.superresolution.common.impl.Pair;
import io.homo.superresolution.common.platform.Platform;
import io.homo.superresolution.common.upscale.AlgorithmType;
import io.homo.superresolution.common.utils.ColorUtil;
import me.shedaniel.clothconfig2.api.*;
import me.shedaniel.clothconfig2.gui.entries.EnumListEntry;
import me.shedaniel.clothconfig2.impl.ConfigEntryBuilderImpl;
import me.shedaniel.clothconfig2.impl.builders.AbstractFieldBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

public class ClothConfig {
    private static int getInt(float v) {
        return (int) (v * 1e4);
    }

    private static int getInt(double v) {
        return (int) (v * 1e4);
    }


    private static float getFloat(int v) {
        return (float) (v / 1e4);
    }

    @SuppressWarnings("unchecked")
    public static void addSpecialConfig(ConfigBuilder builder, ConfigEntryBuilder entryBuilder, String key) {
        Pair<SpecialConfig, Component> specialConfigDescription = Config.getInstance().getSpecial().description.get(key);
        Map<String, SpecialConfigDescription<?>> configDescriptions = specialConfigDescription.left().getDescriptions();
        Set<String> keys = configDescriptions.keySet();
        if (keys.isEmpty()) return;

        ConfigCategory category = builder.getOrCreateCategory(specialConfigDescription.right());
        for (String configKey : keys) {
            SpecialConfigDescription<?> configDescription = configDescriptions.get(configKey);
            AbstractFieldBuilder<?, ?, ?> fieldBuilder =
                    switch (configDescription.getType()) {
                        case ENUM -> entryBuilder.startEnumSelector(
                                        configDescription.getName(),
                                        (Class) configDescription.getClazz(),
                                        (Enum<?>) configDescription.getValue()
                                ).setDefaultValue((Enum<?>) configDescription.getDefaultValue())
                                .setSaveConsumer(configDescription.getSaveConsumer_());
                        case FLOAT -> entryBuilder.startIntSlider(
                                        configDescription.getName(),
                                        getInt((Float) configDescription.getValue()),
                                        getInt(configDescription.getValueRange().left()),
                                        getInt(configDescription.getValueRange().right())
                                )
                                .setTextGetter((integer -> Component.literal(String.format("%.2f", getFloat(integer)))))
                                .setDefaultValue(getInt((Float) configDescription.getDefaultValue()))
                                .setSaveConsumer((integer -> configDescription.getSaveConsumer().accept(integer)));
                        case STRING -> entryBuilder.startStrField(
                                        configDescription.getName(),
                                        (String) configDescription.getValue()
                                ).setDefaultValue((String) configDescription.getDefaultValue())
                                .setSaveConsumer((Consumer<String>) configDescription.getSaveConsumer_());
                        case BOOLEAN -> entryBuilder.startBooleanToggle(
                                        configDescription.getName(),
                                        (Boolean) configDescription.getValue()
                                ).setDefaultValue((Boolean) configDescription.getDefaultValue())
                                .setSaveConsumer((Consumer<Boolean>) configDescription.getSaveConsumer_());
                    };
            if (configDescription.getTooltip() != null) fieldBuilder.setTooltip(configDescription.getTooltip());
            category.addEntry(fieldBuilder.build());
        }
    }

    public static void addDebug(ConfigBuilder builder, ConfigEntryBuilder entryBuilder) {
        ConfigCategory debugCategory = builder.getOrCreateCategory(Component.literal("DEBUG"));
        debugCategory.addEntry(entryBuilder.startBooleanToggle(Component.literal("转储着色器"), Config.getInstance().isDebugDumpShader())
                .setDefaultValue(false)
                .setSaveConsumer(Config.getInstance()::setDebugDumpShader)
                .build());
        ClothTextListEntry debugInfo =
                new ClothTextListEntry(
                        Component.literal("debugInfo"),
                        () -> Component.literal(
                                """
                                        世界渲染用时 %sms
                                        升采样算法用时 %sms"""
                                        .formatted(
                                                BigDecimal.valueOf(PerformanceInfo.getAsMillis("world") - PerformanceInfo.getAsMillis("upscale"))
                                                        .setScale(3, RoundingMode.HALF_UP)
                                                        .toString(),
                                                BigDecimal.valueOf(PerformanceInfo.getAsMillis("upscale"))
                                                        .setScale(3, RoundingMode.HALF_UP)
                                                        .toString()
                                        )
                        ),
                        ColorUtil.color(255, 255, 255, 255),
                        null
                );
        debugInfo.setDisplayRequirement(Requirement.isTrue(() -> Minecraft.getInstance().level != null));
        debugCategory.addEntry(debugInfo);
    }

    public static void add(ConfigBuilder builder) {
        ConfigCategory commonCategory = builder.getOrCreateCategory(Component.literal("通用"));
        ConfigEntryBuilder entryBuilder = ConfigEntryBuilderImpl.create();
        commonCategory.addEntry(entryBuilder.startBooleanToggle(Component.translatable("superresolution.screen.config.options.label.enable_upscale"), Config.isEnableUpscale())
                .setTooltip(Component.translatable("superresolution.screen.config.options.tooltip.enable_upscale"))
                .setDefaultValue(true)
                .setSaveConsumer(Config::setEnableUpscale)
                .build());
        commonCategory.addEntry(entryBuilder.startIntSlider(
                        Component.translatable("superresolution.screen.config.options.label.upscale_ratio"),
                        getInt(Config.getUpscaleRatio()),
                        getInt(Config.getMinUpscaleRatio()),
                        getInt(4.0f)
                )
                .setDefaultValue(getInt(1.7))
                .setTextGetter((integer -> Component.literal(String.format("%.2f", getFloat(integer)))))
                .setTooltipSupplier((integer -> {
                    float value = getFloat(integer);
                    return Optional.of(new Component[]{Component.literal(Component.translatable("superresolution.screen.config.options.tooltip.upscale_ratio").getString().formatted(
                            (int) (SuperResolution.getMinecraftWidth() / value),
                            (int) (SuperResolution.getMinecraftHeight() / value),
                            (int) ((1 / value) * 100) + "%"
                    ))});
                }))
                .setSaveConsumer((i) -> Config.setUpscaleRatio(getFloat(i)))
                .build());
        commonCategory.addEntry(entryBuilder.startIntSlider(
                        Component.translatable("superresolution.screen.config.options.label.sharpness"),
                        getInt(Config.getSharpness()),
                        getInt(0.0),
                        getInt(2.0)
                )
                .setTooltip(Component.translatable("superresolution.screen.config.options.tooltip.sharpness"))
                .setDefaultValue(getInt(0.55))
                .setTextGetter((integer -> Component.literal(String.format("%.2f", getFloat(integer)))))
                .setSaveConsumer((i) -> Config.setSharpness(getFloat(i)))
                .build());
        EnumListEntry<AlgorithmType> algorithmTypeEnumSelector = entryBuilder.startEnumSelector(
                        Component.translatable("superresolution.screen.config.options.label.algo_type"),
                        AlgorithmType.class,
                        Config.getUpscaleAlgo()
                )
                .setDefaultValue(AlgorithmType.FSR1)
                .setEnumNameProvider(((anEnum) -> Component.literal(((AlgorithmType) anEnum).getString())))
                .setSaveConsumer(Config::setUpscaleAlgo).build();
        commonCategory.addEntry(algorithmTypeEnumSelector);
        commonCategory.addEntry(
                entryBuilder.startTextDescription(
                                Component.literal(
                                        """
                                                FSR1 --- AMD FidelityFX Super Resolution 1
                                                FSR2 --- AMD FidelityFX Super Resolution 2
                                                NIS --- NVIDIA Image Scaling
                                                SGSR --- Snapdragon™ Game Super Resolution"""
                                )
                        )
                        .build()
        );
        commonCategory.addEntry(
                entryBuilder.startTextDescription(
                                Component.literal("警告：当前所选算法不稳定")
                        ).setColor(ColorUtil.color(255, 255, 128, 0))
                        .setDisplayRequirement(Requirement.isValue(algorithmTypeEnumSelector, AlgorithmType.FSR2, AlgorithmType.NIS, AlgorithmType.SGSR))
                        .build()
        );
        commonCategory.addEntry(
                entryBuilder.startTextDescription(
                                Component.literal("警告：当前所选算法未完成，无法正常使用")
                        ).setColor(ColorUtil.color(255, 255, 0, 0))
                        .setDisplayRequirement(Requirement.isValue(algorithmTypeEnumSelector, AlgorithmType.FSR2, AlgorithmType.NIS, AlgorithmType.SGSR))
                        .build()
        );
        EnumListEntry<CaptureMode> captureModeEnumSelector = entryBuilder.startEnumSelector(
                        Component.literal("捕获方式"),
                        CaptureMode.class,
                        Config.getCaptureMode()
                )
                .setDefaultValue(CaptureMode.A)
                .setErrorSupplier((captureMode -> {
                    if (Platform.currentPlatform.getMinecraftVersion().equals("1.21.4") && captureMode == CaptureMode.B) {
                        return Optional.of(Component.literal("当前的捕获方式在 %s 无法使用".formatted(Platform.currentPlatform.getMinecraftVersion())));
                    } else {
                        return Optional.empty();
                    }
                }))
                .setTooltipSupplier((captureMode) -> Optional.of(new Component[]{captureMode.get()}))
                .setSaveConsumer(Config::setCaptureMode).build();
        commonCategory.addEntry(captureModeEnumSelector);
        commonCategory.addEntry(new ClothButtonEntry(
                Component.translatable("superresolution.screen.config.button.label.info"),
                (button) -> Minecraft.getInstance().setScreen(new InfoScreen(Minecraft.getInstance().screen, false)),
                true
        ));
        for (String key : Config.getInstance().getSpecial().description.keySet()) {
            addSpecialConfig(builder, entryBuilder, key);
        }
        addDebug(builder, entryBuilder);

        builder.setSavingRunnable(ConfigFile::write);
    }
}
