package io.homo.superresolution.common.gui;

import io.homo.superresolution.api.event.ConfigChangedEvent;
import io.homo.superresolution.api.registry.AlgorithmDescription;
import io.homo.superresolution.api.registry.AlgorithmRegistry;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.config.enums.CaptureMode;
import io.homo.superresolution.common.config.special.SpecialConfig;
import io.homo.superresolution.common.config.special.SpecialConfigDescription;
import io.homo.superresolution.common.debug.PerformanceInfo;
import io.homo.superresolution.common.gui.entries.ClothChartEntry;
import io.homo.superresolution.common.gui.entries.ClothTextListListEntry;
import io.homo.superresolution.common.gui.entries.ClothButtonEntry;
import io.homo.superresolution.common.gui.entries.ClothTextListEntry;
import io.homo.superresolution.common.gui.widgets.Line;
import io.homo.superresolution.common.minecraft.MinecraftRenderHandle;
import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.impl.Pair;
import io.homo.superresolution.common.platform.OSType;
import io.homo.superresolution.common.platform.Platform;
import io.homo.superresolution.core.graphics.GraphicsCapabilities;
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
    public static void add(ConfigBuilder builder) {
        ConfigCategory commonCategory = builder.getOrCreateCategory(Component.translatable("superresolution.screen.config.category.general"));
        ConfigEntryBuilder entryBuilder = ConfigEntryBuilderImpl.create();
        if (Platform.currentPlatform.getOS().type == OSType.ANDROID) {
            commonCategory.addEntry(entryBuilder.startTextDescription(
                            Component.translatable("superresolution.screen.config.warn.mobile_device"))
                    .setColor(ColorUtil.color(255, 255, 0, 0)).build());
        }
        commonCategory.addEntry(entryBuilder.startBooleanToggle(Component.translatable("superresolution.screen.config.options.label.enable_upscale"), SuperResolutionConfig.isEnableUpscaleOriginal())
                .setTooltip(Component.translatable("superresolution.screen.config.options.tooltip.enable_upscale"))
                .setDefaultValue(true)
                .setSaveConsumer((newValue) -> {
                    boolean oldValue = SuperResolutionConfig.isEnableUpscale();
                    SuperResolutionConfig.setEnableUpscale(newValue);
                    if (!SuperResolution.isShaderPackCompatSuperResolution()) return;
                    if (oldValue != newValue) {
                        SuperResolution.irisApiReloadShader();
                    }
                })
                .build());
        commonCategory.addEntry(entryBuilder.startBooleanToggle(Component.translatable("superresolution.screen.config.options.label.disable_upscale_on_vanilla"), SuperResolutionConfig.isDisableUpscaleOnVanilla())
                .setTooltip(Component.translatable("superresolution.screen.config.options.tooltip.disable_upscale_on_vanilla"))
                .setDefaultValue(false)
                .setSaveConsumer(SuperResolutionConfig::setDisableUpscaleOnVanilla)
                .build());
        commonCategory.addEntry(entryBuilder.startIntSlider(
                        Component.translatable("superresolution.screen.config.options.label.upscale_ratio"),
                        getInt(SuperResolutionConfig.getUpscaleRatio()),
                        getInt(SuperResolutionConfig.getMinUpscaleRatio()),
                        getInt(4.0f)
                )
                .setDefaultValue(getInt(1.7))
                .setTextGetter((integer -> Component.literal(String.format("%.2f", getFloat(integer)))))
                .setTooltipSupplier((integer -> {
                    float value = getFloat(integer);
                    return Optional.of(new Component[]{Component.literal(Component.translatable("superresolution.screen.config.options.tooltip.upscale_ratio").getString().formatted(
                            (int) (MinecraftRenderHandle.getScreenWidth() / value),
                            (int) (MinecraftRenderHandle.getScreenHeight() / value),
                            (int) ((1 / value) * 100) + "%"
                    ))});
                }))
                .setSaveConsumer((i) -> SuperResolutionConfig.setUpscaleRatio(getFloat(i)))
                .build());
        commonCategory.addEntry(entryBuilder.startIntSlider(
                        Component.translatable("superresolution.screen.config.options.label.sharpness"),
                        getInt(SuperResolutionConfig.getSharpness()),
                        getInt(0.0),
                        getInt(1.0)
                )
                .setTooltip(Component.translatable("superresolution.screen.config.options.tooltip.sharpness"))
                .setDefaultValue(getInt(0.55))
                .setTextGetter((integer -> Component.literal(String.format("%.2f", getFloat(integer)))))
                .setSaveConsumer((i) -> SuperResolutionConfig.setSharpness(getFloat(i)))
                .build());

        SelectionListEntry<Object> algorithmSelector = entryBuilder.startSelector(
                        Component.translatable("superresolution.screen.config.options.label.algo_type"),
                        AlgorithmRegistry.getAlgorithmMap().values().toArray(),
                        SuperResolutionConfig.getUpscaleAlgorithm()
                )
                .setDefaultValue(AlgorithmDescriptions.FSR1)
                .setNameProvider(((anEnum) -> Component.literal(((AlgorithmDescription<?>) anEnum).getBriefName())))
                .setErrorSupplier((algorithmType -> {
                    if (Platform.currentPlatform.isDevelopmentEnvironment() || Platform.currentPlatform.getModVersionString(SuperResolution.MOD_ID).contains("dev")) {
                        return Optional.empty();
                    }
                    if (!((AlgorithmDescription<?>) algorithmType).getRequirement().check().support()) {
                        return Optional.of(Component.translatable("superresolution.screen.config.error.unsupported_algorithm"));
                    }
                    return Optional.empty();
                }))
                .setSaveConsumer((o -> {
                    SuperResolutionConfig.setUpscaleAlgorithm((AlgorithmDescription<?>) o);
                })).build();
        commonCategory.addEntry(algorithmSelector);
        commonCategory.addEntry(
                entryBuilder.startTextDescription(
                                Component.translatable("superresolution.screen.config.warn.algorithm_unstable")
                        ).setColor(ColorUtil.color(255, 255, 128, 0))
                        .setDisplayRequirement(Requirement.isValue(algorithmSelector, AlgorithmDescriptions.FSR2 /*AlgorithmDescriptions.NIS*/, AlgorithmDescriptions.SGSR2))
                        .build()
        );
        commonCategory.addEntry(
                entryBuilder.startTextDescription(
                                Component.translatable("superresolution.screen.config.warn.algorithm_incomplete")
                        ).setColor(ColorUtil.color(255, 255, 0, 0))
                        .setDisplayRequirement(Requirement.isTrue(() -> false))
                        .build()
        );
        EnumListEntry<CaptureMode> captureModeEnumSelector = entryBuilder.startEnumSelector(
                        Component.translatable("superresolution.screen.config.options.label.capture_mode"),
                        CaptureMode.class,
                        SuperResolutionConfig.getCaptureMode()
                )
                .setDefaultValue(CaptureMode.A)
                .setErrorSupplier((captureMode -> {
                    if (
                            (Platform.currentPlatform.getMinecraftVersion().equals("1.21.4") && captureMode == CaptureMode.B) ||
                                    (Platform.currentPlatform.getMinecraftVersion().equals("1.21.5") && captureMode == CaptureMode.C) ||
                                    (Platform.currentPlatform.getMinecraftVersion().equals("1.21.6") && captureMode == CaptureMode.C) ||
                                    (Platform.currentPlatform.getMinecraftVersion().equals("1.21.7") && captureMode == CaptureMode.C) ||
                                    (Platform.currentPlatform.getMinecraftVersion().equals("1.21.8") && captureMode == CaptureMode.C)
                    ) {
                        return Optional.of(
                                Component.translatable(
                                        "superresolution.screen.config.error.capture_mode_unsupported",
                                        Platform.currentPlatform.getMinecraftVersion()
                                )
                        );
                    } else {
                        return Optional.empty();
                    }
                }))
                .setTooltipSupplier((captureMode) -> Optional.of(new Component[]{captureMode.get()}))
                .setSaveConsumer(SuperResolutionConfig::setCaptureMode).build();
        commonCategory.addEntry(entryBuilder.startBooleanToggle(
                        Component.translatable("superresolution.screen.config.options.label.generate_motion_vectors"),
                        SuperResolutionConfig.isGenerateMotionVectors()
                )
                .setTooltip(Component.translatable("superresolution.screen.config.options.tooltip.generate_motion_vectors"))
                .setDefaultValue(false)
                .setSaveConsumer(SuperResolutionConfig::setGenerateMotionVectors)
                .build());
        commonCategory.addEntry(captureModeEnumSelector);
        commonCategory.addEntry(entryBuilder.startBooleanToggle(
                        Component.translatable("superresolution.screen.config.options.label.skip_init_vulkan"),
                        SuperResolutionConfig.isSkipInitVulkan())
                .setTooltip(Component.translatable("superresolution.screen.config.options.tooltip.skip_init_vulkan"))
                .setSaveConsumer((SuperResolutionConfig::setSkipInitVulkan))
                .requireRestart()
                .build());
        commonCategory.addEntry(entryBuilder.startBooleanToggle(
                        Component.translatable("superresolution.screen.config.options.label.enable_compat_shader_compiler"),
                        SuperResolutionConfig.isEnableCompatShaderCompiler())
                .setTooltip(Component.translatable("superresolution.screen.config.options.tooltip.enable_compat_shader_compiler"))
                .setSaveConsumer((SuperResolutionConfig::setEnableCompatShaderCompiler))
                .requireRestart()
                .build());
        commonCategory.addEntry(entryBuilder.startBooleanToggle(
                        Component.translatable("superresolution.screen.config.options.label.pause_game_on_gui"),
                        SuperResolutionConfig.isPauseGameOnGui())
                .setSaveConsumer((SuperResolutionConfig::setPauseGameOnGui))
                .build());
        commonCategory.addEntry(entryBuilder.startBooleanToggle(
                        Component.translatable("superresolution.screen.config.options.label.enable_detailed_profiling"),
                        SuperResolutionConfig.isEnableDetailedProfiling()
                )
                .setTooltip(Component.translatable("superresolution.screen.config.options.tooltip.enable_detailed_profiling"))
                .setDefaultValue(false)
                .setSaveConsumer(SuperResolutionConfig::setEnableDetailedProfiling)
                .build());
        commonCategory.addEntry(entryBuilder.startBooleanToggle(
                        Component.translatable("superresolution.screen.config.options.label.force_disable_shader_compat"),
                        SuperResolutionConfig.isForceDisableShaderCompat()
                )
                .setTooltip(Component.translatable("superresolution.screen.config.options.tooltip.force_disable_shader_compat"))
                .setDefaultValue(false)
                .setSaveConsumer((newValue) -> {
                    SuperResolutionConfig.setForceDisableShaderCompat(newValue);
                    if (!SuperResolution.isShaderPackCompatSuperResolution()) return;
                    SuperResolution.irisApiReloadShader();
                })
                .build());

        List<String> injectPostChainBlackList = new ArrayList<>();
        commonCategory.addEntry(entryBuilder.startStrList(
                        Component.translatable("superresolution.screen.config.options.label.inject_postChain_black_list"),
                        SuperResolutionConfig.getInjectPostChainBlackList())
                .setDefaultValue(injectPostChainBlackList)
                .requireRestart()
                .setTooltip(Component.translatable("superresolution.screen.config.options.tooltip.inject_postChain_black_list"))
                .setSaveConsumer((SuperResolutionConfig::setInjectPostChainBlackList))
                .build());
        commonCategory.addEntry(new ClothButtonEntry(
                Component.translatable("superresolution.screen.config.button.label.info"),
                (button) -> Minecraft.getInstance().setScreen(ConfigScreenBuilder.create().buildInfoScreen(Minecraft.getInstance().screen)),
                true
        ));
        for (String key : SuperResolutionConfig.SPECIAL.description.keySet()) {
            addSpecialConfig(builder, entryBuilder, key);
        }
        addDebug(builder, entryBuilder);
        builder.setSavingRunnable(() -> {
            SuperResolutionConfig.SPEC.save();
            ConfigChangedEvent.EVENT.invoker().onConfigReload();
        });
    }

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
        Pair<SpecialConfig, String> specialConfigDescription = SuperResolutionConfig.SPECIAL.description.get(key);
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
                                .setSaveConsumer(configDescription.getSaveConsumer())
                                .setEnumNameProvider(configDescription.isValueNameIsSupplier() ? (anEnum -> configDescription.getValueNameSupplierAsObject().apply(anEnum).orElse(Component.empty())) : null);
                        case FLOAT -> entryBuilder.startIntSlider(
                                        configDescription.getName(),
                                        getInt((Float) configDescription.getValue()),
                                        getInt(configDescription.getValueRange().left()),
                                        getInt(configDescription.getValueRange().right())
                                )
                                .setTextGetter(configDescription.isValueNameIsSupplier() ? (integer -> configDescription.getValueNameSupplierAsObject().apply(integer).orElse(Component.empty())) : (integer -> Component.literal(String.format("%.2f", getFloat(integer)))))
                                .setDefaultValue(getInt((Float) configDescription.getDefaultValue()))
                                .setSaveConsumer((integer -> configDescription.getSaveConsumerAsObject().accept(integer)));
                        case STRING -> entryBuilder.startStrField(
                                        configDescription.getName(),
                                        (String) configDescription.getValue()
                                )
                                .setDefaultValue((String) configDescription.getDefaultValue())
                                .setSaveConsumer((Consumer<String>) configDescription.getSaveConsumer());
                        case BOOLEAN -> entryBuilder.startBooleanToggle(
                                        configDescription.getName(),
                                        (Boolean) configDescription.getValue()
                                )
                                .setYesNoTextSupplier(configDescription.isValueNameIsSupplier() ? (aBoolean -> configDescription.getValueNameSupplierAsObject().apply(aBoolean).orElse(Component.empty())) : null)
                                .setDefaultValue((Boolean) configDescription.getDefaultValue())
                                .setSaveConsumer((Consumer<Boolean>) configDescription.getSaveConsumer());
                        case OBJECT -> null;
                    };
            if (configDescription.getTooltip().isPresent()) if (fieldBuilder != null) {
                fieldBuilder.setTooltip(configDescription.getTooltip().orElse(Component.empty()));
            }
            if (fieldBuilder != null) {
                category.addEntry(fieldBuilder.build());
            }
        }
    }

    public static void addDebug(ConfigBuilder builder, ConfigEntryBuilder entryBuilder) {
        ConfigCategory debugCategory = builder.getOrCreateCategory(Component.translatable("superresolution.screen.config.category.debug"));
        debugCategory.addEntry(entryBuilder.startBooleanToggle(
                        Component.translatable("superresolution.screen.config.options.label.enable_debug"),
                        SuperResolutionConfig.isEnableDebug()
                )
                .setDefaultValue(false)
                .setTooltip(Component.translatable("superresolution.screen.config.options.tooltip.enable_debug"))
                .setSaveConsumer(SuperResolutionConfig::setEnableDebug)
                .build());
        debugCategory.addEntry(entryBuilder.startBooleanToggle(
                        Component.translatable("superresolution.screen.config.options.label.debug_dump_shader"),
                        SuperResolutionConfig.isDebugDumpShader())
                .setDefaultValue(false)
                .setTooltip(Component.translatable("superresolution.screen.config.options.tooltip.debug_dump_shader"))
                .setSaveConsumer(SuperResolutionConfig::setDebugDumpShader)
                .build());

        debugCategory.addEntry(entryBuilder.startBooleanToggle(
                        Component.translatable("superresolution.screen.config.options.label.enable_renderdoc"),
                        SuperResolutionConfig.isEnableRenderDoc())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("superresolution.screen.config.options.tooltip.enable_renderdoc"))
                .setSaveConsumer(SuperResolutionConfig::setEnableRenderDoc)
                .build());

        debugCategory.addEntry(entryBuilder.startBooleanToggle(
                        Component.translatable("superresolution.screen.config.options.label.enable_imgui"),
                        SuperResolutionConfig.isEnableImgui())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("superresolution.screen.config.options.tooltip.enable_imgui"))
                .setSaveConsumer(SuperResolutionConfig::setEnableImgui)
                .build());
    }

    public static void addInfos(ConfigBuilder builder) {
        ConfigCategory performanceInfoCategory = builder.getOrCreateCategory(Component.translatable("superresolution.screen.info.title.performance_info"));
        ConfigCategory algoInfoCategory = builder.getOrCreateCategory(Component.translatable("superresolution.screen.info.text.algo_support_status"));
        ConfigCategory envInfoCategory = builder.getOrCreateCategory(Component.translatable("superresolution.screen.info.title.env_info"));
        ConfigCategory projectInfoCategory = builder.getOrCreateCategory(Component.translatable("superresolution.screen.info.title.project_info"));

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
        if (RenderSystems.isSupportVulkan()) {
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
        if (RenderSystems.isSupportVulkan()) envInfoCategory.addEntry(vkExtInfoEntry);

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

        ClothTextListListEntry contributorsEntry = new ClothTextListListEntry(
                Component.translatable("superresolution.screen.info.text.contributors"),
                null,
                true
        ).setTop(4).setBottom(7);
        String[] contributors = {
                "187J3X1",
                "异世界美西螈",
                "yu",
                "Enaium",
                "rrtt217",
                "qwertyuiop"
        };
        Map<String, String> libraries = new LinkedHashMap<>() {{
            put("Cloth Config", "https://github.com/shedaniel/cloth-config");
            put("Architectury API", "https://github.com/architectury/architectury-api");
            put("Night Config", "https://github.com/TheElectronWill/night-config");
            put("SpongePowered Mixin", "https://github.com/SpongePowered/Mixin");
            put("Manifold", "https://github.com/manifold-systems/manifold");
            put("Dear ImGui", "https://github.com/ocornut/imgui");
            put("Snapdragon™ Game Super Resolution 2(1)", "https://github.com/SnapdragonStudios/snapdragon-gsr");
            put("FidelityFX Super Resolution 1.0", "https://github.com/GPUOpen-Effects/FidelityFX-FSR");
            put("FidelityFX Super Resolution 2.2", "https://github.com/GPUOpen-Effects/FidelityFX-FSR2");
            put("AMD FidelityFX™ SDK", "https://github.com/GPUOpen-LibrariesAndSDKs/FidelityFX-SDK");
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
            put(
                    "Modrinth",
                    "https://modrinth.com/mod/superresolution"
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
                            )
                    )
            );
        });
        projectInfoCategory.addEntry(webLinksEntry);
        projectInfoCategory.addEntry(contributorsEntry);
        projectInfoCategory.addEntry(librariesEntry);

        ClothTextListEntry debugInfo = new ClothTextListEntry(
                Component.translatable("superresolution.screen.debug.performance_info"),
                () -> {

                    String stringBuilder =
                            Component.translatable("superresolution.screen.info.performance_info.frame_time",
                                    BigDecimal.valueOf(PerformanceInfo.getAsMillis("runTick"))
                                            .setScale(3, RoundingMode.HALF_UP)
                            ).getString() + "\n" +
                                    Component.translatable("superresolution.screen.info.performance_info.world_time",
                                            Minecraft.getInstance().level != null && SuperResolutionConfig.isEnableDetailedProfiling() ? BigDecimal.valueOf(PerformanceInfo.getAsMillis("world"))
                                                    .setScale(3, RoundingMode.HALF_UP) : "?"
                                    ).getString() + "\n" +
                                    Component.translatable("superresolution.screen.info.performance_info.upscale_time",
                                            Minecraft.getInstance().level != null && SuperResolutionConfig.isEnableDetailedProfiling() ? BigDecimal.valueOf(PerformanceInfo.getAsMillis("upscale"))
                                                    .setScale(3, RoundingMode.HALF_UP) : "?"
                                    ).getString();
                    return Component.literal(stringBuilder);
                },
                ColorUtil.color(255, 255, 255, 255),
                null
        );

        ClothChartEntry frameTimeChart = new ClothChartEntry(
                Component.translatable("superresolution.screen.info.performance_info.frame_time",
                        BigDecimal.valueOf(PerformanceInfo.getAsMillis("runTick"))
                                .setScale(3, RoundingMode.HALF_UP)
                ).getString(),
                Component.translatable("superresolution.screen.info.performance_info.frame_time",
                        BigDecimal.valueOf(PerformanceInfo.getAsMillis("runTick"))
                                .setScale(3, RoundingMode.HALF_UP)
                ),
                null
        );
        frameTimeChart.setRenderCallback((chart) -> {
            chart.setName(
                    Component.translatable("superresolution.screen.info.performance_info.frame_time",
                            BigDecimal.valueOf(PerformanceInfo.getAsMillis("runTick"))
                                    .setScale(3, RoundingMode.HALF_UP)
                    ).getString()
            );
            chart.push(PerformanceInfo.getAsMillis("runTick"), 1000);
        });
        frameTimeChart.setDisplayRange(0, 100);
        performanceInfoCategory.addEntry(debugInfo);
        performanceInfoCategory.addEntry(frameTimeChart);
        if (SuperResolutionConfig.isEnableDetailedProfiling()) {
            ClothChartEntry worldTimeChart = new ClothChartEntry(
                    Component.translatable("superresolution.screen.info.performance_info.world_time",
                            BigDecimal.valueOf(PerformanceInfo.getAsMillis("world"))
                                    .setScale(3, RoundingMode.HALF_UP)
                    ).getString(),
                    Component.translatable("superresolution.screen.info.performance_info.world_time",
                            BigDecimal.valueOf(PerformanceInfo.getAsMillis("world"))
                                    .setScale(3, RoundingMode.HALF_UP)
                    ),
                    null
            );
            worldTimeChart.setRenderCallback((chart) -> {
                if (Minecraft.getInstance().level != null) {
                    chart.setName(
                            Component.translatable("superresolution.screen.info.performance_info.world_time",
                                    BigDecimal.valueOf(PerformanceInfo.getAsMillis("world"))
                                            .setScale(3, RoundingMode.HALF_UP)
                            ).getString()
                    );
                    chart.push(PerformanceInfo.getAsMillis("world"), 1000);
                }
            });
            worldTimeChart.setDisplayRange(0, 80);

            ClothChartEntry upscaleTimeChart = new ClothChartEntry(
                    Component.translatable("superresolution.screen.info.performance_info.upscale_time",
                            BigDecimal.valueOf(PerformanceInfo.getAsMillis("upscale"))
                                    .setScale(3, RoundingMode.HALF_UP)
                    ).getString(),
                    Component.translatable("superresolution.screen.info.performance_info.upscale_time",
                            BigDecimal.valueOf(PerformanceInfo.getAsMillis("upscale"))
                                    .setScale(3, RoundingMode.HALF_UP)
                    ),
                    null
            );
            upscaleTimeChart.setRenderCallback((chart) -> {
                if (Minecraft.getInstance().level != null) {
                    chart.setName(
                            Component.translatable("superresolution.screen.info.performance_info.upscale_time",
                                    BigDecimal.valueOf(PerformanceInfo.getAsMillis("upscale"))
                                            .setScale(3, RoundingMode.HALF_UP)
                            ).getString()
                    );
                    chart.push(PerformanceInfo.getAsMillis("upscale"), 1000);
                }
            });
            upscaleTimeChart.setDisplayRange(0, 40);
            performanceInfoCategory.addEntry(worldTimeChart);
            performanceInfoCategory.addEntry(upscaleTimeChart);
        }
    }

    private static ClickEvent createURLClickEvent(String url) {
        #if MC_VER > MC_1_21_4
        return new ClickEvent.OpenUrl(java.net.URI.create(url));
        #else
        return new ClickEvent(ClickEvent.Action.OPEN_URL, url);
        #endif
    }
}
