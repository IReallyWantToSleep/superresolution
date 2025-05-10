package io.homo.superresolution.common.gui;

import io.homo.superresolution.api.registry.AlgorithmDescription;
import io.homo.superresolution.api.registry.AlgorithmRegistry;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.ConfigFile;
import io.homo.superresolution.common.config.enums.CaptureMode;
import io.homo.superresolution.common.config.special.SpecialConfig;
import io.homo.superresolution.common.config.special.SpecialConfigDescription;
import io.homo.superresolution.common.config.Config;
import io.homo.superresolution.common.debug.PerformanceInfo;
import io.homo.superresolution.common.gui.entries.ClothTextListListEntry;
import io.homo.superresolution.common.gui.entries.ClothButtonEntry;
import io.homo.superresolution.common.gui.entries.ClothTextListEntry;
import io.homo.superresolution.common.gui.widgets.Line;
import io.homo.superresolution.core.impl.Pair;
import io.homo.superresolution.common.platform.OSType;
import io.homo.superresolution.common.platform.Platform;
import io.homo.superresolution.core.GraphicsCapabilities;
import io.homo.superresolution.core.interop.GlVkInteropManager;
import io.homo.superresolution.common.upscale.AlgorithmDescriptions;
import io.homo.superresolution.common.upscale.AlgorithmManager;
import io.homo.superresolution.core.utils.ColorUtil;
import me.shedaniel.clothconfig2.api.*;
import me.shedaniel.clothconfig2.gui.entries.EnumListEntry;
import me.shedaniel.clothconfig2.gui.entries.SelectionListEntry;
import me.shedaniel.clothconfig2.impl.ConfigEntryBuilderImpl;
import me.shedaniel.clothconfig2.impl.builders.AbstractFieldBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
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
        Pair<SpecialConfig, String> specialConfigDescription = Config.getInstance().getSpecial().description.get(key);
        Map<String, SpecialConfigDescription<?>> configDescriptions = specialConfigDescription.left().getDescriptions();
        Set<String> keys = configDescriptions.keySet();
        if (keys.isEmpty()) return;

        ConfigCategory category = builder.getOrCreateCategory(Component.literal(specialConfigDescription.right()));
        for (String configKey : keys) {
            SpecialConfigDescription<?> configDescription = configDescriptions.get(configKey);
            AbstractFieldBuilder<?, ?, ?> fieldBuilder =
                    switch (configDescription.getType()) {
                        case ENUM -> entryBuilder.startEnumSelector(
                                        configDescription.getName(),
                                        (Class) configDescription.getClazz(),
                                        (Enum<?>) configDescription.getValue()
                                )
                                .setDefaultValue((Enum<?>) configDescription.getDefaultValue())
                                .setSaveConsumer(configDescription.getSaveConsumer_())
                                .setEnumNameProvider(configDescription.isNameIsSupplier() ? (anEnum -> configDescription.getNameSupplier().apply(anEnum).orElse(Component.empty())) : null);
                        case FLOAT -> entryBuilder.startIntSlider(
                                        configDescription.getName(),
                                        getInt((Float) configDescription.getValue()),
                                        getInt(configDescription.getValueRange().left()),
                                        getInt(configDescription.getValueRange().right())
                                )
                                .setTextGetter(configDescription.isNameIsSupplier() ? (integer -> configDescription.getNameSupplier().apply(integer).orElse(Component.empty())) : (integer -> Component.literal(String.format("%.2f", getFloat(integer)))))
                                .setDefaultValue(getInt((Float) configDescription.getDefaultValue()))
                                .setSaveConsumer((integer -> configDescription.getSaveConsumer().accept(integer)));
                        case STRING -> entryBuilder.startStrField(
                                        configDescription.getName(),
                                        (String) configDescription.getValue()
                                )
                                .setDefaultValue((String) configDescription.getDefaultValue())
                                .setSaveConsumer((Consumer<String>) configDescription.getSaveConsumer_());
                        case BOOLEAN -> entryBuilder.startBooleanToggle(
                                        configDescription.getName(),
                                        (Boolean) configDescription.getValue()
                                )
                                .setYesNoTextSupplier(configDescription.isNameIsSupplier() ? (aBoolean -> configDescription.getNameSupplier().apply(aBoolean).orElse(Component.empty())) : null)
                                .setDefaultValue((Boolean) configDescription.getDefaultValue())
                                .setSaveConsumer((Consumer<Boolean>) configDescription.getSaveConsumer_());
                        case OBJECT -> null;
                    };
            if (configDescription.getTooltip() != null) if (fieldBuilder != null) {
                fieldBuilder.setTooltip(configDescription.getTooltip());
            }
            if (fieldBuilder != null) {
                category.addEntry(fieldBuilder.build());
            }
        }
    }

    public static void addDebug(ConfigBuilder builder, ConfigEntryBuilder entryBuilder) {
        ConfigCategory debugCategory = builder.getOrCreateCategory(Component.translatable("superresolution.screen.config.category.debug"));
        debugCategory.addEntry(entryBuilder.startBooleanToggle(
                        Component.translatable("superresolution.screen.config.options.label.debug_dump_shader"),
                        Config.getInstance().isDebugDumpShader())
                .setDefaultValue(false)
                .setTooltip(Component.translatable("superresolution.screen.config.options.tooltip.debug_dump_shader"))
                .setSaveConsumer(Config.getInstance()::setDebugDumpShader)
                .build());

        debugCategory.addEntry(entryBuilder.startBooleanToggle(
                        Component.translatable("superresolution.screen.config.options.label.enable_renderdoc"),
                        Config.getInstance().isEnableRenderDoc())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("superresolution.screen.config.options.tooltip.enable_renderdoc"))
                .setSaveConsumer(Config.getInstance()::setEnableRenderDoc)
                .build());

        debugCategory.addEntry(entryBuilder.startBooleanToggle(
                        Component.translatable("superresolution.screen.config.options.label.enable_imgui"),
                        Config.getInstance().isEnableImgui())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("superresolution.screen.config.options.tooltip.enable_imgui"))
                .setSaveConsumer(Config.getInstance()::setEnableImgui)
                .build());

        ClothTextListEntry debugInfo = new ClothTextListEntry(
                Component.translatable("superresolution.screen.debug.performance_info"),
                () -> Component.translatable(
                        "superresolution.screen.debug.performance_data",
                        BigDecimal.valueOf(PerformanceInfo.getAsMillis("world") - PerformanceInfo.getAsMillis("upscale"))
                                .setScale(3, RoundingMode.HALF_UP),
                        BigDecimal.valueOf(PerformanceInfo.getAsMillis("upscale"))
                                .setScale(3, RoundingMode.HALF_UP)
                ),
                ColorUtil.color(255, 255, 255, 255),
                null
        );
        debugInfo.setDisplayRequirement(Requirement.isTrue(() -> Minecraft.getInstance().level != null));
        debugCategory.addEntry(debugInfo);
    }

    public static void add(ConfigBuilder builder) {
        ConfigCategory commonCategory = builder.getOrCreateCategory(Component.translatable("superresolution.screen.config.category.general"));
        ConfigEntryBuilder entryBuilder = ConfigEntryBuilderImpl.create();
        if (Platform.currentPlatform.getOS().type == OSType.ANDROID) {
            commonCategory.addEntry(entryBuilder.startTextDescription(
                            Component.translatable("superresolution.screen.config.warn.mobile_device"))
                    .setColor(ColorUtil.color(255, 255, 0, 0)).build());
        }
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

        SelectionListEntry<Object> algorithmSelector = entryBuilder.startSelector(
                        Component.translatable("superresolution.screen.config.options.label.algo_type"),
                        AlgorithmRegistry.getAlgorithmMap().values().toArray(),
                        Config.getUpscaleAlgo()
                )
                .setDefaultValue(AlgorithmDescriptions.FSR1)
                .setNameProvider(((anEnum) -> Component.literal(((AlgorithmDescription<?>) anEnum).getBriefName())))
                .setErrorSupplier((algorithmType -> {
                    if (Platform.currentPlatform.isDevelopmentEnvironment() || Platform.currentPlatform.getModVersionString(SuperResolution.MOD_ID).contains("dev")) {
                        return Optional.empty();
                    }
                    if (List.of(AlgorithmDescriptions.NIS, AlgorithmDescriptions.FSR2).contains(algorithmType)) {
                        return Optional.of(Component.literal("当前环境不支持该算法"));
                    } else if (Objects.equals(AlgorithmDescriptions.FSR2, algorithmType) && Platform.currentPlatform.getOS().type == OSType.ANDROID) {
                        return Optional.of(Component.literal("当前环境不支持该算法"));
                    }
                    return Optional.empty();
                }))
                .setSaveConsumer((o -> {
                    Config.setUpscaleAlgo((AlgorithmDescription<?>) o);
                })).build();
        commonCategory.addEntry(algorithmSelector);
        commonCategory.addEntry(
                entryBuilder.startTextDescription(
                                Component.translatable("superresolution.algo.description.header"))
                        .build()
        );
        commonCategory.addEntry(
                entryBuilder.startTextDescription(
                                Component.translatable("superresolution.screen.config.warn.algorithm_unstable")
                        ).setColor(ColorUtil.color(255, 255, 128, 0))
                        .setDisplayRequirement(Requirement.isValue(algorithmSelector, AlgorithmDescriptions.FSR2, AlgorithmDescriptions.NIS, AlgorithmDescriptions.SGSR2))
                        .build()
        );
        commonCategory.addEntry(
                entryBuilder.startTextDescription(
                                Component.translatable("superresolution.screen.config.warn.algorithm_incomplete")
                        ).setColor(ColorUtil.color(255, 255, 0, 0))
                        .setDisplayRequirement(Requirement.isValue(algorithmSelector, AlgorithmDescriptions.FSR2, AlgorithmDescriptions.NIS))
                        .build()
        );
        EnumListEntry<CaptureMode> captureModeEnumSelector = entryBuilder.startEnumSelector(
                        Component.translatable("superresolution.screen.config.options.label.capture_mode"),
                        CaptureMode.class,
                        Config.getCaptureMode()
                )
                .setDefaultValue(CaptureMode.A)
                .setErrorSupplier((captureMode -> {
                    if (
                            (Platform.currentPlatform.getMinecraftVersion().equals("1.21.4") && captureMode == CaptureMode.B) ||
                                    (Platform.currentPlatform.getMinecraftVersion().equals("1.21.5") && captureMode == CaptureMode.C)
                    ) {
                        return Optional.of(Component.literal("当前的捕获方式在 %s 无法使用".formatted(Platform.currentPlatform.getMinecraftVersion())));
                    } else {
                        return Optional.empty();
                    }
                }))
                .setTooltipSupplier((captureMode) -> Optional.of(new Component[]{captureMode.get()}))
                .setSaveConsumer(Config::setCaptureMode).build();
        commonCategory.addEntry(entryBuilder.startBooleanToggle(
                        Component.translatable("superresolution.screen.config.options.label.generate_motion_vectors"),
                        Config.isGenerateMotionVectors()
                )
                .setTooltip(Component.translatable("superresolution.screen.config.options.tooltip.generate_motion_vectors"))
                .setDefaultValue(false)
                .setSaveConsumer(Config::setGenerateMotionVectors)
                .build());
        commonCategory.addEntry(captureModeEnumSelector);
        commonCategory.addEntry(entryBuilder.startBooleanToggle(
                        Component.translatable("superresolution.screen.config.options.label.skip_init_vulkan"),
                        Config.isSkipInitVulkan())
                .setTooltip(Component.translatable("superresolution.screen.config.options.tooltip.skip_init_vulkan"))
                .setSaveConsumer((Config::setSkipInitVulkan))
                .requireRestart()
                .build());
        commonCategory.addEntry(new ClothButtonEntry(
                Component.translatable("superresolution.screen.config.button.label.info"),
                (button) -> Minecraft.getInstance().setScreen(ConfigScreenBuilder.create().buildInfoScreen(Minecraft.getInstance().screen)),
                true
        ));
        for (String key : Config.getInstance().getSpecial().description.keySet()) {
            addSpecialConfig(builder, entryBuilder, key);
        }
        addDebug(builder, entryBuilder);
        builder.setSavingRunnable(ConfigFile::write);
    }

    public static void addInfos(ConfigBuilder builder) {
        ConfigCategory envInfoCategory = builder.getOrCreateCategory(Component.translatable("superresolution.screen.info.title.env_info"));
        ClothTextListListEntry envInfoEntry = new ClothTextListListEntry(
                Component.empty(),
                null,
                false
        ).setTop(4).setBottom(7);
        InfoBuilder.of(envInfoEntry).addEnvInfo();
        ClothTextListListEntry glExtInfoEntry = new ClothTextListListEntry(
                Component.translatable("superresolution.screen.info.button.label.opengl_ext_info").append(" ").append(
                        Component.translatable("superresolution.screen.info.text.opengl_ext_count").getString()
                                .formatted(GraphicsCapabilities.getGLExtensions().size())
                ),
                null,
                true
        ).setTop(4).setBottom(7);
        InfoBuilder.of(glExtInfoEntry).addGlExt();
        ClothTextListListEntry vkExtInfoEntry = null;
        if (GlVkInteropManager.isSupportVulkan()) {
            vkExtInfoEntry = new ClothTextListListEntry(
                    Component.translatable("superresolution.screen.info.button.label.vulkan_ext_info").append(" ").append(
                            Component.translatable("superresolution.screen.info.text.vulkan_ext_count").getString()
                                    .formatted(GraphicsCapabilities.getVulkanDeviceExtensions().size())
                    ),
                    null,
                    true
            ).setTop(4).setBottom(7);
            InfoBuilder.of(vkExtInfoEntry).addVkExt();
        }

        envInfoCategory.addEntry(envInfoEntry);
        envInfoCategory.addEntry(glExtInfoEntry);
        if (GlVkInteropManager.isSupportVulkan()) envInfoCategory.addEntry(vkExtInfoEntry);
        ConfigCategory algoInfoCategory = builder.getOrCreateCategory(Component.translatable("superresolution.screen.info.text.algo_support_status"));
        for (AlgorithmDescription<?> algorithmDescription : AlgorithmRegistry.getAlgorithmMap().values()) {
            if (algorithmDescription.equals(AlgorithmDescriptions.NONE)) continue;
            ClothTextListListEntry algoInfoEntry = new ClothTextListListEntry(
                    MutableComponent.create(Component.literal(algorithmDescription.getDisplayName()).getContents()).withStyle(Style.EMPTY.withColor(
                            AlgorithmManager.isSupportAlgorithm(algorithmDescription) ?
                                    ColorUtil.color(255, 255, 255, 255) :
                                    ColorUtil.color(255, 255, 0, 0)
                    )),
                    null,
                    true
            ).setTop(4).setBottom(7);
            InfoBuilder.of(algoInfoEntry).addAlgoInfo(algorithmDescription);
            algoInfoCategory.addEntry(algoInfoEntry);
        }
        ConfigCategory projectInfoCategory = builder.getOrCreateCategory(
                Component.translatable("superresolution.screen.info.title.project_info")
        );
        ClothTextListListEntry contributorsEntry = new ClothTextListListEntry(
                Component.translatable("superresolution.screen.info.text.contributors"),
                null,
                true
        ).setTop(4).setBottom(7);
        String[] contributors = {
                "187J3X1 - 核心开发+造饼大王",
                "异世界美西螈 - 测试反馈+吉祥物+大饼规划者",
                "yu - 绘制图标"
        };
        Map<String, String> libraries = new LinkedHashMap<>() {{
            put("Cloth Config", "https://github.com/shedaniel/cloth-config");
            put("Architectury API", "https://github.com/architectury/architectury-api");
            put("SpongePowered Mixin", "https://github.com/SpongePowered/Mixin");
            put("Dear ImGui", "https://github.com/ocornut/imgui");
            put("Snapdragon™ Game Super Resolution 2(1)", "https://github.com/SnapdragonStudios/snapdragon-gsr");
            put("FidelityFX Super Resolution 1.0", "https://github.com/GPUOpen-Effects/FidelityFX-FSR");
            put("FidelityFX Super Resolution 2.2", "https://github.com/GPUOpen-Effects/FidelityFX-FSR2");
            put("NVIDIA Image Scaling SDK v1.0.3", "https://github.com/NVIDIAGameWorks/NVIDIAImageScaling");
            put("Java OpenGL Math Library(JOML)", "https://github.com/JOML-CI/JOML");
            put("RenderDoc", "https://github.com/baldurk/renderdoc");
            put("Lightweight Java Game Library 3(LWJGL3)", "https://github.com/LWJGL/lwjgl3");
            put("Glslang", "https://github.com/KhronosGroup/glslang");
        }};
        Map<String, String> officialLinks = new LinkedHashMap<>() {{
            put(
                    Component.translatable("superresolution.screen.info.link.official_website").getString(),
                    "https://minecraft-superresolution.netlify.app/"
            );
            put(
                    Component.translatable("superresolution.screen.info.link.github_repo").getString(),
                    "https://github.com/187J3X1-114514/superresolution"
            );
            put(
                    Component.translatable("superresolution.screen.info.link.issue_tracker").getString(),
                    "https://github.com/187J3X1-114514/superresolution/issues"
            );
            put(
                    Component.translatable("superresolution.screen.info.link.mcmod_homepage").getString(),
                    "https://www.mcmod.cn/class/17888.html"
            );
        }};
        ClothTextListListEntry webLinksEntry = new ClothTextListListEntry(
                Component.translatable("superresolution.screen.info.text.website_links"),
                null,
                true
        ).setTop(4).setBottom(7);

        webLinksEntry.addLine(new Line()
                .text(Component.translatable("superresolution.screen.info.text.website_links"))
                .color(0, 122, 204, 255)
                .center(true)
        );

        officialLinks.forEach((title, url) -> {
            webLinksEntry.addLine(new Line()
                    .text(Component.literal(title)
                            .withStyle(Style.EMPTY
                                    .withColor(0xFF42A5F5)
                                    .withClickEvent(createURLClickEvent(url))
                            )
                            .append(Component.literal(" - "))
                            .append(
                                    Component.literal(url)
                                            .withStyle(Style.EMPTY
                                                    .withColor(ColorUtil.color(255, 150, 150, 150))
                                                    .withUnderlined(true)
                                                    .withClickEvent(createURLClickEvent(url))
                                            )
                            )
                    ));
        });
        contributorsEntry.addLine(new Line()
                .text(Component.translatable("superresolution.screen.info.text.contributors"))
                .color(255, 200, 100, 255)
                .center(true)
        );
        for (String contributor : contributors) {
            contributorsEntry.addLine(new Line()
                    .text("• " + contributor)
                    .color(255, 255, 255, 255)
            );
        }
        ClothTextListListEntry librariesEntry = new ClothTextListListEntry(
                Component.translatable("superresolution.screen.info.text.open_source"),
                null,
                true
        ).setTop(4).setBottom(7);
        librariesEntry.addLine(new Line()
                .text(Component.translatable("superresolution.screen.info.text.open_source"))
                .color(100, 200, 255, 255)
                .center(true)
        );
        librariesEntry.addLine(new Line()
                .text(Component.translatable("superresolution.screen.info.text.open_source_thank"))
                .color(100, 200, 255, 255)
                .center(true)
        );
        libraries.forEach((name, url) -> {
            librariesEntry.addLine(new Line()
                    .text(Component.literal(name)
                            .withStyle(Style.EMPTY
                                    .withUnderlined(true)
                                    .withColor(0xFF00FF00)
                                    .withClickEvent(createURLClickEvent(url))
                            )
                    )
            );
            librariesEntry.addLine(new Line()
                    .text(Component.literal(url)
                            .withStyle(Style.EMPTY
                                    .withColor(ColorUtil.color(255, 150, 150, 150))
                                    .withClickEvent(createURLClickEvent(url))
                            ))
            );
        });
        projectInfoCategory.addEntry(webLinksEntry);
        projectInfoCategory.addEntry(contributorsEntry);
        projectInfoCategory.addEntry(librariesEntry);
    }

    private static ClickEvent createURLClickEvent(String url) {
        #if MC_VER > MC_1_21_4
        return new ClickEvent.OpenUrl(URI.create(url));
        #else
        return new ClickEvent(ClickEvent.Action.OPEN_URL, url);
        #endif
    }
}
