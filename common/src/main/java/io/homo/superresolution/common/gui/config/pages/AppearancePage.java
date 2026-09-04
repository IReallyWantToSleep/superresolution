package io.homo.superresolution.common.gui.config.pages;

import io.homo.superresolution.api.QualityPreset;
import io.homo.superresolution.api.platform.OperatingSystemType;
import io.homo.superresolution.api.platform.Platform;
import io.homo.superresolution.api.registry.AlgorithmDescription;
import io.homo.superresolution.api.registry.AlgorithmRegistry;
import io.homo.superresolution.api.registry.BackendGroup;
import io.homo.superresolution.api.registry.LowLatencyGroups;
import io.homo.superresolution.api.registry.ExtraResource;
import io.homo.superresolution.api.registry.ExtraResources;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.config.enums.CaptureMode;
import io.homo.superresolution.common.config.enums.InternalTextureFormat;
import io.homo.superresolution.common.config.enums.InteropSyncMode;
import io.homo.superresolution.common.config.special.SpecialConfig;
import io.homo.superresolution.common.framegeneration.FrameGeneration;
import io.homo.superresolution.common.framegeneration.FrameGenerationDescriptions;
import io.homo.superresolution.api.registry.FrameGenerationDescription;
import io.homo.superresolution.api.registry.FrameGenerationRegistry;
import io.homo.superresolution.common.framegeneration.FrameGenerationMode;
import io.homo.superresolution.common.lowlatency.LowLatency;
import io.homo.superresolution.common.lowlatency.nv.NVIDIAReflexMode;
import io.homo.superresolution.api.registry.LowLatencyDescription;
import io.homo.superresolution.api.registry.LowLatencyRegistry;
import io.homo.superresolution.common.config.special.SpecialConfigDescription;
import io.homo.superresolution.common.gui.download.MaterialResourcesList;
import io.homo.superresolution.common.gui.impl.OptionRequirement;
import io.homo.superresolution.common.gui.impl.Text;
import io.homo.superresolution.common.gui.options.*;
import io.homo.superresolution.common.minecraft.B3DVulkanBridge;
import io.homo.superresolution.common.minecraft.MinecraftUtils;
import io.homo.superresolution.common.minecraft.MinecraftWindow;
import io.homo.superresolution.common.minecraft.handler.RenderHandlerManager;
import io.homo.superresolution.common.perf.PerformanceTracker;
import io.homo.superresolution.common.upscale.AlgorithmDescriptions;
import io.homo.superresolution.common.upscale.interoplayer.GlVulkanInteropAlgorithm;
import io.homo.superresolution.common.workmode.SRWorkModeManager;
import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.SuperResolutionConstants;
import io.homo.superresolution.core.SuperResolutionNative;
import io.homo.superresolution.core.graphics.GraphicsCapabilities;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.gui.*;
import io.homo.superresolution.core.gui.core.ContainerWidget;
import io.homo.superresolution.core.gui.core.UIInputState;
import io.homo.superresolution.core.gui.core.animator.TimeInterpolator;
import io.homo.superresolution.core.gui.core.backends.interfaces.IImage;
import io.homo.superresolution.core.gui.core.backends.interfaces.IPaint;
import io.homo.superresolution.core.gui.core.backends.interfaces.TextAlign;
import io.homo.superresolution.core.gui.core.backends.interfaces.TextAlignType;
import io.homo.superresolution.core.gui.core.backends.render.RenderContext;
import io.homo.superresolution.core.gui.core.frame.Frame;
import io.homo.superresolution.core.gui.core.frame.ScrollableFrame;
import io.homo.superresolution.core.gui.core.impl.Rectangle;
import io.homo.superresolution.core.gui.core.impl.Tooltip;
import io.homo.superresolution.core.gui.widgets.MaterialContainerWidget;
import io.homo.superresolution.core.gui.widgets.MaterialWidget;
import io.homo.superresolution.core.gui.widgets.SpacerWidget;
import io.homo.superresolution.core.gui.widgets.button.MaterialButton;
import io.homo.superresolution.core.gui.widgets.button.MaterialButtonSize;
import io.homo.superresolution.core.gui.widgets.button.MaterialButtonVariant;
import io.homo.superresolution.core.gui.widgets.chart.MaterialChart;
import io.homo.superresolution.core.gui.widgets.chart.MaterialChartDataSeries;
import io.homo.superresolution.core.gui.widgets.chart.MaterialChartType;
import io.homo.superresolution.common.gui.widgets.SponsorChip;
import io.homo.superresolution.core.gui.widgets.dialog.MaterialDialog;
import io.homo.superresolution.core.gui.widgets.label.MaterialLabel;
import io.homo.superresolution.core.gui.widgets.navigation.drawer.MaterialNavigationDrawer;
import io.homo.superresolution.core.gui.widgets.progress.MaterialCircularProgressIndicator;
import io.homo.superresolution.core.gui.widgets.progress.MaterialProgressShape;
import io.homo.superresolution.core.impl.Destroyable;
import io.homo.superresolution.core.impl.Pair;
import io.homo.superresolution.core.utils.Color;
import io.homo.superresolution.core.utils.ImageLoader;
import io.homo.superresolution.core.utils.MouseCursor;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

import io.homo.superresolution.common.gui.SponsorService;
import io.homo.superresolution.common.gui.LogoRenderer;
import io.homo.superresolution.common.gui.config.pages.ConfigPageContext.QualityPresetOption;
import io.homo.superresolution.common.gui.config.pages.ConfigPageContext.TitlePill;
import io.homo.superresolution.common.gui.config.pages.ConfigPageContext.InfoCard;
import io.homo.superresolution.common.gui.config.pages.ConfigPageContext.ContributorInfo;
import io.homo.superresolution.common.gui.config.pages.ConfigPageContext.LibraryInfo;
import io.homo.superresolution.common.gui.config.pages.ConfigPageContext.SponsorWrappingRow;
import static io.homo.superresolution.common.gui.config.pages.ConfigPageContext.CURRENT_VERSION_SUPPORTS_VULKAN_PRESENTATION;

public final class AppearancePage implements ConfigPage {
    public static final AppearancePage INSTANCE = new AppearancePage();

    private AppearancePage() {
    }

    @Override
    public Frame create(ConfigPageContext context) {
        return build(context);
    }

    private static Frame build(ConfigPageContext context) {
        ScrollableFrame frame = context.createStandardScrollableFrame();
        ContainerWidget container = context.createStandardContainer();
        context.addFrameTitle(container, Text.translatable("superresolution.screen.config.section.appearance"));
        OptionBuilder builder = context.createOptionBuilder(Text.translatable("superresolution.screen.config.category.appearance"));
        builder.enumSelectorOption(
                        Text.translatable("superresolution.screen.config.options.label.theme"),
                        MaterialTheme.class,
                        SuperResolutionConfig.getTheme())
                .setDefaultValue(MaterialTheme.Light)
                .setEnumNameProvider(t -> Text.translatable("superresolution.enum.theme." + t.name().toLowerCase()).getString())
                .setSaveConsumer(value -> {
                    SuperResolutionConfig.setTheme(value);
                    MaterialUI.setScheme(MaterialScheme.from(value, SuperResolutionConfig.getThemeColor(),
                            SuperResolutionConfig.getThemeSchemeVariant(), SuperResolutionConfig.getThemeContrastLevel()));
                    context.setMaterialScheme(MaterialUI.Scheme);
                })
                .build();
        builder.colorSelectOption(
                        Text.translatable("superresolution.screen.config.options.label.theme_color"),
                        SuperResolutionConfig.getThemeColor())
                .setDefaultValue(() -> Color.from("#78DC77"))
                .setValueChangeListener(value -> {
                    MaterialUI.setScheme(MaterialScheme.from(SuperResolutionConfig.getTheme(), value,
                            SuperResolutionConfig.getThemeSchemeVariant(), SuperResolutionConfig.getThemeContrastLevel()));
                    context.setMaterialScheme(MaterialUI.Scheme);
                })
                .setSaveConsumer(value -> {
                    SuperResolutionConfig.setThemeColor(value);
                    MaterialUI.setScheme(MaterialScheme.from(SuperResolutionConfig.getTheme(), value,
                            SuperResolutionConfig.getThemeSchemeVariant(), SuperResolutionConfig.getThemeContrastLevel()));
                    context.setMaterialScheme(MaterialUI.Scheme);
                })
                .build();
        builder.enumSelectorOption(
                        Text.translatable("superresolution.screen.config.options.label.theme_scheme_variant"),
                        SchemeVariant.class,
                        SuperResolutionConfig.getThemeSchemeVariant())
                .setDefaultValue(SchemeVariant.CONTENT)
                .setEnumNameProvider(v -> Text.translatable("superresolution.enum.schemevarinat." + v.name().toLowerCase()).getString())
                .setSaveConsumer(value -> {
                    SuperResolutionConfig.setThemeSchemeVariant(value);
                    MaterialUI.setScheme(MaterialScheme.from(SuperResolutionConfig.getTheme(), SuperResolutionConfig.getThemeColor(),
                            value, SuperResolutionConfig.getThemeContrastLevel()));
                    context.setMaterialScheme(MaterialUI.Scheme);
                })
                .build();
        builder.numberOption(
                        Text.translatable("superresolution.screen.config.options.label.theme_contrast_level"),
                        SuperResolutionConfig.getThemeContrastLevel(),
                        1.0f,
                        -1.0f)
                .setStep(0.2)
                .setValueFormater((value) -> String.format("%.0f", value.doubleValue() * 100) + "%")
                .setDescription(Text.translatable("superresolution.screen.config.options.tooltip.theme_contrast_level"))
                .setDefaultValue(() -> 0.0f)
                .setValueChangeListener(value -> {
                    MaterialUI.setScheme(MaterialScheme.from(SuperResolutionConfig.getTheme(), SuperResolutionConfig.getThemeColor(),
                            SuperResolutionConfig.getThemeSchemeVariant(), value.doubleValue()));
                    context.setMaterialScheme(MaterialUI.Scheme);
                })
                .setSaveConsumer(value -> {
                    SuperResolutionConfig.setThemeContrastLevel(value.floatValue());
                    MaterialUI.setScheme(MaterialScheme.from(SuperResolutionConfig.getTheme(), SuperResolutionConfig.getThemeColor(),
                            SuperResolutionConfig.getThemeSchemeVariant(), value.doubleValue()));
                    context.setMaterialScheme(MaterialUI.Scheme);
                })
                .build();

        context.addOptionGroupToContainer(container, builder);
        context.finalizeFrame(frame, container);
        return frame;
    }

}
