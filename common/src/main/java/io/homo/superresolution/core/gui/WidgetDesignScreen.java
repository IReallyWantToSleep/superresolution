/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.homo.superresolution.core.gui;

import io.homo.superresolution.common.gui.TestOptionBuilder;
import io.homo.superresolution.common.gui.options.OptionBuilder;
import io.homo.superresolution.common.minecraft.MinecraftWindow;
import io.homo.superresolution.core.gui.core.UIInputState;
import io.homo.superresolution.core.gui.core.backends.render.RenderContext;
import io.homo.superresolution.core.gui.core.backends.interfaces.TextAlign;
import io.homo.superresolution.core.gui.core.backends.interfaces.TextAlignType;
import io.homo.superresolution.core.gui.core.backends.interfaces.Transform;
import io.homo.superresolution.core.gui.core.frame.Frame;
import io.homo.superresolution.core.gui.core.frame.ScrollableFrame;
import io.homo.superresolution.core.gui.core.impl.Rectangle;
import io.homo.superresolution.core.gui.core.ContainerWidget;
import io.homo.superresolution.core.gui.widgets.SpacerWidget;
import io.homo.superresolution.core.gui.widgets.button.MaterialButton;
import io.homo.superresolution.core.gui.widgets.button.MaterialButtonSize;
import io.homo.superresolution.core.gui.widgets.label.MaterialLabel;
import io.homo.superresolution.core.gui.widgets.menu.*;
import io.homo.superresolution.core.gui.widgets.navigation.drawer.MaterialNavigationDrawer;
import io.homo.superresolution.core.gui.widgets.select.MaterialSelect;
import io.homo.superresolution.core.gui.widgets.sliders.MaterialSlider;
import io.homo.superresolution.core.gui.widgets.switchs.MaterialSwitch;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.*;
import org.joml.Vector2f;
import io.homo.superresolution.core.utils.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class WidgetDesignScreen extends NanoVGScreen<WidgetDesignScreen> {

    private MaterialScheme materialScheme;
    private String currentContentKey = "general";
    private java.util.Map<String, Frame> contentFrames;
    private YogaNode navigationDrawerLayout;
    private YogaNode contentLayout;
    private Frame currentContentFrame;
    private MaterialNavigationDrawer drawer;

    public WidgetDesignScreen(Component title) {
        super(title);
    }

    @Override
    protected void buildWidgets() {
        materialScheme = MaterialScheme.from(MaterialTheme.Dark, Color.from("#6750A4"));
        contentFrames = new java.util.HashMap<>();
        currentContentKey = "general";

        getView().removeFrame(getDefaultFrame());
        Frame navigationDrawerFrame = createNavigationDrawerFrame();
        navigationDrawerLayout = getView().addFrame(navigationDrawerFrame);
        navigationDrawerLayout.setMinWidthPercent(17.1f);
        navigationDrawerLayout.setHeightPercent(100);
        navigationDrawerLayout.setPadding(YogaEdge.ALL, 0);

        currentContentFrame = getOrCreateContentFrame(currentContentKey);
        contentLayout = getView().addFrame(currentContentFrame);
        contentLayout.setWidthPercent(82.9f);
        contentLayout.setHeightPercent(100);
        contentLayout.setPadding(YogaEdge.ALL, 0);

        view.setDebugRenderEnabled(true);
    }

    private Frame getOrCreateContentFrame(String key) {
        if (contentFrames.containsKey(key)) {
            return contentFrames.get(key);
        }
        Frame frame;
        switch (key) {
            case "general":
                frame = createGeneralFrame();
                break;
            case "algorithm":
                frame = createAlgorithmFrame();
                break;
            case "debug":
                frame = createDebugFrame();
                break;
            case "performance":
                frame = createPerformanceFrame();
                break;
            case "environment":
                frame = createEnvironmentFrame();
                break;
            case "about":
                frame = createAboutFrame();
                break;
            case "options":
                frame = createOptionsFrame();
                break;
            case "select":
                frame = createSelectFrame();
                break;
            default:
                frame = createEmptyFrame();
        }
        contentFrames.put(key, frame);
        return frame;
    }

    private void switchContentFrame(String key) {
        if (key.equals(currentContentKey)) {
            return;
        }
        if (currentContentFrame != null) {
            getView().removeFrame(currentContentFrame);
        }
        currentContentKey = key;
        currentContentFrame = getOrCreateContentFrame(key);
        contentLayout = getView().addFrame(currentContentFrame);
        contentLayout.setWidthPercent(82.9f);
        contentLayout.setHeightPercent(100);
        contentLayout.setPadding(YogaEdge.ALL, 0);
        view.markLayoutDirty();
    }

    private Frame createButtonFrame() {
        ScrollableFrame frame = new ScrollableFrame();
        frame.setContentPadding(10);
        frame.setVerticalScrollEnabled(true);
        frame.setHorizontalScrollEnabled(false);

        ContainerWidget container = new ContainerWidget();
        container.layout().setFlexDirection(YogaFlexDirection.COLUMN);
        container.layout().setWidthPercent(100);
        container.layout().setGap(YogaGutter.COLUMN, 15);
        container.layout().setAlignItems(YogaAlign.CENTER);
        container.layout().setJustifyContent(YogaJustify.FLEX_START);

        MaterialLabel title = MaterialLabel.create()
                .text("Material Buttons")
                .fontSize(24)
                .color(materialScheme.primary())
                .scheme(materialScheme);
        title.layout().setMargin(YogaEdge.BOTTOM, 20);
        container.addChild(title);

        MaterialLabel typeLabel = MaterialLabel.create()
                .text("Button Types")
                .fontSize(18)
                .color(materialScheme.onSurface())
                .scheme(materialScheme);
        typeLabel.layout().setMargin(YogaEdge.BOTTOM, 10);
        container.addChild(typeLabel);

        MaterialButton filledBtn = MaterialButton.filled("Filled Button", materialScheme)
                .size(MaterialButtonSize.Medium);
        filledBtn.layout().setMargin(YogaEdge.VERTICAL, 5);
        container.addChild(filledBtn);

        MaterialButton elevatedBtn = MaterialButton.elevated("Elevated Button", materialScheme)
                .size(MaterialButtonSize.Medium);
        elevatedBtn.layout().setMargin(YogaEdge.VERTICAL, 5);
        container.addChild(elevatedBtn);

        MaterialButton tonalBtn = MaterialButton.tonal("Tonal Button", materialScheme)
                .size(MaterialButtonSize.Medium);
        tonalBtn.layout().setMargin(YogaEdge.VERTICAL, 5);
        container.addChild(tonalBtn);

        MaterialButton outlinedBtn = MaterialButton.outlined("Outlined Button", materialScheme)
                .size(MaterialButtonSize.Medium);
        outlinedBtn.layout().setMargin(YogaEdge.VERTICAL, 5);
        container.addChild(outlinedBtn);

        MaterialButton textBtn = MaterialButton.text("Text Button", materialScheme)
                .size(MaterialButtonSize.Medium);
        textBtn.layout().setMargin(YogaEdge.VERTICAL, 5);
        container.addChild(textBtn);

        MaterialLabel iconLabel = MaterialLabel.create()
                .text("Icon Buttons")
                .fontSize(18)
                .color(materialScheme.onSurface())
                .scheme(materialScheme);
        iconLabel.layout().setMargin(YogaEdge.TOP, 20);
        iconLabel.layout().setMargin(YogaEdge.BOTTOM, 10);
        container.addChild(iconLabel);

        MaterialButton iconBtn1 = MaterialButton.filled("Add Item", materialScheme)
                .icon(MaterialSymbols.iconAdd())
                .size(MaterialButtonSize.Medium);
        iconBtn1.layout().setMargin(YogaEdge.VERTICAL, 5);
        container.addChild(iconBtn1);

        MaterialButton iconBtn2 = MaterialButton.outlined("Edit", materialScheme)
                .icon(MaterialSymbols.iconEdit())
                .size(MaterialButtonSize.Medium);
        iconBtn2.layout().setMargin(YogaEdge.VERTICAL, 5);
        container.addChild(iconBtn2);

        MaterialButton iconBtn3 = MaterialButton.tonal("Delete", materialScheme)
                .icon(MaterialSymbols.iconDelete())
                .size(MaterialButtonSize.Medium);
        iconBtn3.layout().setMargin(YogaEdge.VERTICAL, 5);
        container.addChild(iconBtn3);

        MaterialButton iconBtn4 = MaterialButton.text("Settings", materialScheme)
                .icon(MaterialSymbols.iconSettings())
                .size(MaterialButtonSize.Medium);
        iconBtn4.layout().setMargin(YogaEdge.VERTICAL, 5);
        container.addChild(iconBtn4);

        MaterialLabel sizeLabel = MaterialLabel.create()
                .text("Button Sizes")
                .fontSize(18)
                .color(materialScheme.onSurface())
                .scheme(materialScheme);
        sizeLabel.layout().setMargin(YogaEdge.TOP, 20);
        sizeLabel.layout().setMargin(YogaEdge.BOTTOM, 10);
        container.addChild(sizeLabel);

        MaterialButton smallBtn = MaterialButton.filled("Small", materialScheme)
                .size(MaterialButtonSize.Small);
        smallBtn.layout().setMargin(YogaEdge.VERTICAL, 5);
        container.addChild(smallBtn);

        MaterialButton mediumBtn = MaterialButton.filled("Medium", materialScheme)
                .size(MaterialButtonSize.Medium);
        mediumBtn.layout().setMargin(YogaEdge.VERTICAL, 5);
        container.addChild(mediumBtn);

        MaterialButton largeBtn = MaterialButton.filled("Large", materialScheme)
                .size(MaterialButtonSize.Large);
        largeBtn.layout().setMargin(YogaEdge.VERTICAL, 5);
        container.addChild(largeBtn);

        MaterialLabel interactiveLabel = MaterialLabel.create()
                .text("Interactive Buttons")
                .fontSize(18)
                .color(materialScheme.onSurface())
                .scheme(materialScheme);
        interactiveLabel.layout().setMargin(YogaEdge.TOP, 20);
        interactiveLabel.layout().setMargin(YogaEdge.BOTTOM, 10);
        container.addChild(interactiveLabel);

        MaterialButton clickCounterBtn = MaterialButton.elevated("Click Count: 0", materialScheme)
                .size(MaterialButtonSize.Medium);
        final int[] clickCount = {0};
        clickCounterBtn.onClick(e -> {
            clickCount[0]++;
            clickCounterBtn.text("Click Count: " + clickCount[0]);
        });
        clickCounterBtn.layout().setMargin(YogaEdge.VERTICAL, 5);
        container.addChild(clickCounterBtn);

        MaterialButton toggleBtn = MaterialButton.tonal("Toggle State", materialScheme)
                .size(MaterialButtonSize.Medium);
        final boolean[] toggleState = {false};
        toggleBtn.onClick(e -> {
            toggleState[0] = !toggleState[0];
            toggleBtn.text(toggleState[0] ? "State: ON" : "State: OFF");
        });
        toggleBtn.layout().setMargin(YogaEdge.VERTICAL, 5);
        container.addChild(toggleBtn);

        MaterialLabel combinationLabel = MaterialLabel.create()
                .text("Button Combinations")
                .fontSize(18)
                .color(materialScheme.onSurface())
                .scheme(materialScheme);
        combinationLabel.layout().setMargin(YogaEdge.TOP, 20);
        combinationLabel.layout().setMargin(YogaEdge.BOTTOM, 10);
        container.addChild(combinationLabel);

        ContainerWidget buttonRow = new ContainerWidget();
        buttonRow.layout().setFlexDirection(YogaFlexDirection.ROW);
        buttonRow.layout().setGap(YogaGutter.ROW, 10);
        buttonRow.layout().setJustifyContent(YogaJustify.CENTER);

        MaterialButton btn1 = MaterialButton.filled("A", materialScheme).size(MaterialButtonSize.Small);
        MaterialButton btn2 = MaterialButton.filled("B", materialScheme).size(MaterialButtonSize.Small);
        MaterialButton btn3 = MaterialButton.filled("C", materialScheme).size(MaterialButtonSize.Small);
        buttonRow.addChild(btn1);
        buttonRow.addChild(btn2);
        buttonRow.addChild(btn3);
        buttonRow.layout().setMargin(YogaEdge.VERTICAL, 5);
        container.addChild(buttonRow);

        for (int i = 1; i <= 5; i++) {
            MaterialButton extraBtn = MaterialButton.outlined("Extra Button " + i, materialScheme)
                    .size(MaterialButtonSize.Medium);
            extraBtn.layout().setMargin(YogaEdge.VERTICAL, 5);
            container.addChild(extraBtn);
        }

        MaterialLabel spacer = MaterialLabel.create()
                .text("")
                .scheme(materialScheme);
        spacer.layout().setHeight(20);
        container.addChild(spacer);

        frame.setRoot(container);
        return frame;
    }

    private Frame createSliderSwitchFrame() {
        Frame frame = new Frame();
        ContainerWidget container = new ContainerWidget();
        container.layout().setFlexDirection(YogaFlexDirection.COLUMN);
        container.layout().setWidthPercent(100);
        container.layout().setHeightPercent(100);
        container.layout().setGap(YogaGutter.COLUMN, 20);
        container.layout().setAlignItems(YogaAlign.CENTER);
        container.layout().setJustifyContent(YogaJustify.FLEX_START);

        MaterialLabel sliderTitle = MaterialLabel.create()
                .text("Material Sliders")
                .fontSize(24)
                .color(materialScheme.primary())
                .scheme(materialScheme);
        sliderTitle.layout().setMargin(YogaEdge.BOTTOM, 20);
        container.addChild(sliderTitle);

        MaterialSlider slider1 = MaterialSlider.create(300)
                .setMax(100)
                .setMin(0)
                .setValue(50)
                .scheme(materialScheme);
        slider1.layout().setMargin(YogaEdge.VERTICAL, 10);
        container.addChild(slider1);

        MaterialSlider slider2 = MaterialSlider.create(300)
                .setMax(100)
                .setMin(0)
                .setStep(1)
                .setValue(75)
                .useIntegerFormatter()
                .scheme(materialScheme);
        slider2.style().valueIndicator(true);
        slider2.layout().setMargin(YogaEdge.VERTICAL, 10);
        container.addChild(slider2);

        MaterialSlider slider3 = MaterialSlider.create(300)
                .setMax(100)
                .setMin(0)
                .setStep(10)
                .setValue(30)
                .scheme(materialScheme);
        slider3.style().steps(true);
        slider3.style().valueIndicator(true);
        slider3.usePercentageFormatter();
        slider3.layout().setMargin(YogaEdge.VERTICAL, 10);
        container.addChild(slider3);

        MaterialLabel switchTitle = MaterialLabel.create()
                .text("Material Switches")
                .fontSize(24)
                .color(materialScheme.primary())
                .scheme(materialScheme);
        switchTitle.layout().setMargin(YogaEdge.TOP, 40);
        switchTitle.layout().setMargin(YogaEdge.BOTTOM, 20);
        container.addChild(switchTitle);

        ContainerWidget switchRow1 = new ContainerWidget();
        switchRow1.layout().setFlexDirection(YogaFlexDirection.ROW);
        switchRow1.layout().setGap(YogaGutter.ROW, 10);
        switchRow1.layout().setAlignItems(YogaAlign.CENTER);

        MaterialLabel switchLabel1 = MaterialLabel.create()
                .text("Basic Switch:")
                .fontSize(16)
                .scheme(materialScheme);
        MaterialSwitch switch1 = MaterialSwitch.create()
                .scheme(materialScheme)
                .setChecked(true);
        switchRow1.addChild(switchLabel1);
        switchRow1.addChild(switch1);
        switchRow1.layout().setMargin(YogaEdge.VERTICAL, 10);
        switch1.layout().setMargin(YogaEdge.LEFT, 10);
        container.addChild(switchRow1);

        ContainerWidget switchRow2 = new ContainerWidget();
        switchRow2.layout().setFlexDirection(YogaFlexDirection.ROW);
        switchRow2.layout().setGap(YogaGutter.ROW, 10);
        switchRow2.layout().setAlignItems(YogaAlign.CENTER);

        MaterialLabel switchLabel2 = MaterialLabel.create()
                .text("With Icons:")
                .fontSize(16)
                .scheme(materialScheme);
        MaterialSwitch switch2 = MaterialSwitch.create()
                .scheme(materialScheme)
                .setChecked(false);
        switch2.style().showCheckedIconWhenEnable(true);
        switch2.style().showUncheckedIconWhenEnable(true);
        switchRow2.addChild(switchLabel2);
        switchRow2.addChild(switch2);
        switch2.layout().setMargin(YogaEdge.LEFT, 10);
        switchRow2.layout().setMargin(YogaEdge.VERTICAL, 10);
        container.addChild(switchRow2);

        frame.setRoot(container);
        return frame;
    }

    private Frame createGeneralFrame() {
        ScrollableFrame frame = new ScrollableFrame();
        frame.setContentPadding(20);
        frame.setVerticalScrollEnabled(true);
        frame.setHorizontalScrollEnabled(false);

        ContainerWidget container = new ContainerWidget();
        container.layout().setFlexDirection(YogaFlexDirection.COLUMN);
        container.layout().setWidthPercent(100);
        container.layout().setGap(YogaGutter.COLUMN, 15);
        container.layout().setAlignItems(YogaAlign.FLEX_START);

        MaterialLabel title = MaterialLabel.create()
                .text("通用设置")
                .fontSize(24)
                .color(materialScheme.primary())
                .scheme(materialScheme);
        title.layout().setMargin(YogaEdge.BOTTOM, 20);
        container.addChild(title);

        ContainerWidget enableRow = createSettingRow("启用超分辨率", "开启或关闭超分辨率功能");
        MaterialSwitch enableSwitch = MaterialSwitch.create().setChecked(true).scheme(materialScheme);
        enableRow.addChild(enableSwitch);
        container.addChild(enableRow);
        MaterialLabel scaleLabel = MaterialLabel.create()
                .text("渲染比例")
                .fontSize(16)
                .color(materialScheme.onSurface())
                .scheme(materialScheme);
        scaleLabel.layout().setMargin(YogaEdge.TOP, 10);
        container.addChild(scaleLabel);

        MaterialSlider scaleSlider = MaterialSlider.of(0.25, 1.0, 0.5, 0.05, 300)
                .usePercentageFormatter()
                .scheme(materialScheme);
        scaleSlider.layout().setMargin(YogaEdge.TOP, 5);
        container.addChild(scaleSlider);

        frame.setRoot(container);
        return frame;
    }

    private Frame createAlgorithmFrame() {
        ScrollableFrame frame = new ScrollableFrame();
        frame.setContentPadding(20);
        frame.setVerticalScrollEnabled(true);
        frame.setHorizontalScrollEnabled(false);

        ContainerWidget container = new ContainerWidget();
        container.layout().setFlexDirection(YogaFlexDirection.COLUMN);
        container.layout().setWidthPercent(100);
        container.layout().setGap(YogaGutter.COLUMN, 15);
        container.layout().setAlignItems(YogaAlign.FLEX_START);

        MaterialLabel title = MaterialLabel.create()
                .text("算法设置")
                .fontSize(24)
                .color(materialScheme.primary())
                .scheme(materialScheme);
        title.layout().setMargin(YogaEdge.BOTTOM, 20);
        container.addChild(title);

        MaterialLabel algoLabel = MaterialLabel.create()
                .text("超分辨率算法")
                .fontSize(16)
                .color(materialScheme.onSurface())
                .scheme(materialScheme);
        container.addChild(algoLabel);

        ContainerWidget algoRow = new ContainerWidget();
        algoRow.layout().setFlexDirection(YogaFlexDirection.ROW);
        algoRow.layout().setGap(YogaGutter.ALL, 10);

        MaterialButton dlssBtn = MaterialButton.tonal("DLSS", materialScheme);
        MaterialButton fsrBtn = MaterialButton.outlined("FSR", materialScheme);
        MaterialButton xessBtn = MaterialButton.outlined("XeSS", materialScheme);
        algoRow.addChild(dlssBtn);
        algoRow.addChild(fsrBtn);
        algoRow.addChild(xessBtn);
        container.addChild(algoRow);

        MaterialLabel qualityLabel = MaterialLabel.create()
                .text("质量预设")
                .fontSize(16)
                .color(materialScheme.onSurface())
                .scheme(materialScheme);
        qualityLabel.layout().setMargin(YogaEdge.TOP, 15);
        container.addChild(qualityLabel);

        ContainerWidget qualityRow = new ContainerWidget();
        qualityRow.layout().setFlexDirection(YogaFlexDirection.ROW);
        qualityRow.layout().setGap(YogaGutter.ALL, 8);

        MaterialButton ultraBtn = MaterialButton.outlined("Ultra Quality", materialScheme);
        MaterialButton qualityBtn = MaterialButton.tonal("Quality", materialScheme);
        MaterialButton balancedBtn = MaterialButton.outlined("Balanced", materialScheme);
        MaterialButton performanceBtn = MaterialButton.outlined("Performance", materialScheme);
        qualityRow.addChild(ultraBtn);
        qualityRow.addChild(qualityBtn);
        qualityRow.addChild(balancedBtn);
        qualityRow.addChild(performanceBtn);
        container.addChild(qualityRow);

        MaterialLabel sharpnessLabel = MaterialLabel.create()
                .text("锐化强度")
                .fontSize(16)
                .color(materialScheme.onSurface())
                .scheme(materialScheme);
        sharpnessLabel.layout().setMargin(YogaEdge.TOP, 15);
        container.addChild(sharpnessLabel);

        MaterialSlider sharpnessSlider = MaterialSlider.of(0.0, 1.0, 0.5, 0.1, 300)
                .usePercentageFormatter()
                .scheme(materialScheme);
        container.addChild(sharpnessSlider);

        frame.setRoot(container);
        return frame;
    }


    private Frame createDebugFrame() {
        ScrollableFrame frame = new ScrollableFrame();
        frame.setContentPadding(20);
        frame.setVerticalScrollEnabled(true);
        frame.setHorizontalScrollEnabled(false);

        ContainerWidget container = new ContainerWidget();
        container.layout().setFlexDirection(YogaFlexDirection.COLUMN);
        container.layout().setWidthPercent(100);
        container.layout().setGap(YogaGutter.COLUMN, 15);
        container.layout().setAlignItems(YogaAlign.FLEX_START);

        frame.setRoot(container);
        return frame;
    }

    private Frame createPerformanceFrame() {
        ScrollableFrame frame = new ScrollableFrame();
        frame.setContentPadding(20);
        frame.setVerticalScrollEnabled(true);
        frame.setHorizontalScrollEnabled(false);

        ContainerWidget container = new ContainerWidget();
        container.layout().setFlexDirection(YogaFlexDirection.COLUMN);
        container.layout().setWidthPercent(100);
        container.layout().setGap(YogaGutter.COLUMN, 12);
        container.layout().setAlignItems(YogaAlign.FLEX_START);

        MaterialLabel title = MaterialLabel.create()
                .text("性能信息")
                .fontSize(24)
                .color(materialScheme.primary())
                .scheme(materialScheme);
        title.layout().setMargin(YogaEdge.BOTTOM, 20);
        container.addChild(title);

        // 性能统计信息
        container.addChild(createInfoRow("当前FPS", "60"));
        container.addChild(createInfoRow("平均帧时间", "16.67 ms"));
        container.addChild(createInfoRow("超分辨率耗时", "2.3 ms"));
        container.addChild(createInfoRow("渲染分辨率", "1280 x 720"));
        container.addChild(createInfoRow("输出分辨率", "1920 x 1080"));
        container.addChild(createInfoRow("缩放比例", "1.5x"));

        MaterialLabel gpuTitle = MaterialLabel.create()
                .text("GPU 信息")
                .fontSize(18)
                .color(materialScheme.primary())
                .scheme(materialScheme);
        gpuTitle.layout().setMargin(YogaEdge.TOP, 20);
        gpuTitle.layout().setMargin(YogaEdge.BOTTOM, 10);
        container.addChild(gpuTitle);

        container.addChild(createInfoRow("GPU 使用率", "65%"));
        container.addChild(createInfoRow("显存使用", "4.2 GB / 8.0 GB"));
        container.addChild(createInfoRow("GPU 温度", "62°C"));

        frame.setRoot(container);
        return frame;
    }

    private Frame createEnvironmentFrame() {
        ScrollableFrame frame = new ScrollableFrame();
        frame.setContentPadding(20);
        frame.setVerticalScrollEnabled(true);
        frame.setHorizontalScrollEnabled(false);

        ContainerWidget container = new ContainerWidget();
        container.layout().setFlexDirection(YogaFlexDirection.COLUMN);
        container.layout().setWidthPercent(100);
        container.layout().setGap(YogaGutter.COLUMN, 12);
        container.layout().setAlignItems(YogaAlign.FLEX_START);

        MaterialLabel title = MaterialLabel.create()
                .text("环境信息")
                .fontSize(24)
                .color(materialScheme.primary())
                .scheme(materialScheme);
        title.layout().setMargin(YogaEdge.BOTTOM, 20);
        container.addChild(title);

        // 系统信息
        container.addChild(createInfoRow("操作系统", "Windows 11"));
        container.addChild(createInfoRow("Java 版本", "21.0.1"));
        container.addChild(createInfoRow("Minecraft 版本", "1.21.1"));
        container.addChild(createInfoRow("Mod 加载器", "NeoForge 21.1.77"));

        MaterialLabel gpuTitle = MaterialLabel.create()
                .text("GPU 环境")
                .fontSize(18)
                .color(materialScheme.primary())
                .scheme(materialScheme);
        gpuTitle.layout().setMargin(YogaEdge.TOP, 20);
        gpuTitle.layout().setMargin(YogaEdge.BOTTOM, 10);
        container.addChild(gpuTitle);

        container.addChild(createInfoRow("GPU 型号", "NVIDIA GeForce RTX 4070"));
        container.addChild(createInfoRow("驱动版本", "546.33"));
        container.addChild(createInfoRow("Vulkan 版本", "1.3.275"));

        MaterialLabel srTitle = MaterialLabel.create()
                .text("超分辨率支持")
                .fontSize(18)
                .color(materialScheme.primary())
                .scheme(materialScheme);
        srTitle.layout().setMargin(YogaEdge.TOP, 20);
        srTitle.layout().setMargin(YogaEdge.BOTTOM, 10);
        container.addChild(srTitle);

        container.addChild(createInfoRow("DLSS", "✓ 支持"));
        container.addChild(createInfoRow("FSR", "✓ 支持"));
        container.addChild(createInfoRow("XeSS", "✓ 支持"));

        frame.setRoot(container);
        return frame;
    }

    private ContainerWidget createSettingRow(String label, String description) {
        ContainerWidget row = new ContainerWidget();
        row.layout().setFlexDirection(YogaFlexDirection.ROW);
        row.layout().setWidthPercent(100);
        row.layout().setJustifyContent(YogaJustify.SPACE_BETWEEN);
        row.layout().setAlignItems(YogaAlign.CENTER);
        row.layout().setPadding(YogaEdge.VERTICAL, 8);

        ContainerWidget textContainer = new ContainerWidget();
        textContainer.layout().setFlexDirection(YogaFlexDirection.COLUMN);
        textContainer.layout().setFlexGrow(1f);

        MaterialLabel labelWidget = MaterialLabel.create()
                .text(label)
                .fontSize(16)
                .color(materialScheme.onSurface())
                .scheme(materialScheme);
        textContainer.addChild(labelWidget);

        if (description != null && !description.isEmpty()) {
            MaterialLabel descWidget = MaterialLabel.create()
                    .text(description)
                    .fontSize(12)
                    .color(materialScheme.onSurfaceVariant())
                    .scheme(materialScheme);
            descWidget.layout().setMargin(YogaEdge.TOP, 2);
            textContainer.addChild(descWidget);
        }

        row.addChild(textContainer);
        return row;
    }

    private ContainerWidget createInfoRow(String label, String value) {
        ContainerWidget row = new ContainerWidget();
        row.layout().setFlexDirection(YogaFlexDirection.ROW);
        row.layout().setWidthPercent(100);
        row.layout().setJustifyContent(YogaJustify.SPACE_BETWEEN);
        row.layout().setAlignItems(YogaAlign.CENTER);
        row.layout().setPadding(YogaEdge.VERTICAL, 6);

        MaterialLabel labelWidget = MaterialLabel.create()
                .text(label)
                .fontSize(14)
                .color(materialScheme.onSurfaceVariant())
                .scheme(materialScheme);
        row.addChild(labelWidget);

        MaterialLabel valueWidget = MaterialLabel.create()
                .text(value)
                .fontSize(14)
                .color(materialScheme.onSurface())
                .scheme(materialScheme);
        row.addChild(valueWidget);

        return row;
    }

    private Frame createNavigationDrawerFrame() {
        ScrollableFrame frame = new ScrollableFrame();
        frame.setHorizontalScrollEnabled(false);
        frame.setVerticalScrollEnabled(true);
        ContainerWidget container = new ContainerWidget();
        container.layout().setFlexDirection(YogaFlexDirection.COLUMN);
        container.layout().setWidthPercent(100);

        drawer = MaterialNavigationDrawer.create()
                .addHeader("Super Resolution", MaterialSymbols.iconSettings())
                .addSectionHeader("配置")
                .addItem("通用", MaterialSymbols.iconSettings(), "general")
                .addItem("算法", MaterialSymbols.iconTune(), "algorithm")
                .addItem("界面", MaterialSymbols.iconPalette(), "interface")
                .addItem("调试", MaterialSymbols.iconBugReport(), "debug")
                .addItem("渲染测试", MaterialSymbols.iconScience(), "render_test")
                .addDivider()
                .addSectionHeader("示例")
                .addItem("选项示例", MaterialSymbols.iconSettings(), "options")
                .addItem("选择器示例", MaterialSymbols.iconArrowDropDown(), "select")
                .addDivider()
                .addSectionHeader("信息")
                .addItem("性能信息", MaterialSymbols.iconSpeed(), "performance")
                .addItem("环境信息", MaterialSymbols.iconInfo(), "environment")
                .addFlexibleSpacer()
                .addItem("关于", MaterialSymbols.iconInfo(), "about")
                .onItemSelected(item -> {
                    String key = String.valueOf(item.getValue());
                    if ("render_test".equals(key)) {
                        Minecraft.getInstance().setScreen(new RenderTestScreen());
                    } else {
                        switchContentFrame(key);
                    }
                })
                .setSelectedByValue("general")
                .scheme(materialScheme);
        drawer.layout().setWidthPercent(100);
        drawer.layout().setHeightPercent(100);
        container.addChild(drawer);

        frame.setRoot(container);
        return frame;
    }

    private Frame createOptionsFrame() {
        ScrollableFrame frame = new ScrollableFrame();
        frame.setContentPadding(20, 0, 20, 0);
        frame.setVerticalScrollEnabled(true);
        frame.setHorizontalScrollEnabled(false);
        SpacerWidget spacerWidgetTop = SpacerWidget.vertical(20f);
        SpacerWidget spacerWidgetBottom = SpacerWidget.vertical(20f);

        ContainerWidget container = new ContainerWidget();
        container.layout().setFlexDirection(YogaFlexDirection.COLUMN);
        container.layout().setWidthPercent(100);
        container.layout().setGap(YogaGutter.COLUMN, 15);
        container.layout().setAlignItems(YogaAlign.FLEX_START);

        MaterialLabel title = MaterialLabel.create()
                .text("选项系统示例")
                .fontSize(24)
                .color(materialScheme.primary())
                .scheme(materialScheme);
        title.layout().setMargin(YogaEdge.BOTTOM, 20);
        container.addChild(spacerWidgetTop);
        container.addChild(title);
        OptionBuilder.OptionsContainer optionsContainer = TestOptionBuilder.buildOptionsContainer();
        optionsContainer.layout().setWidthPercent(100);
        container.addChild(optionsContainer);
        container.addChild(spacerWidgetBottom);

        frame.setRoot(container);
        return frame;
    }

    private Frame createSelectFrame() {
        ScrollableFrame frame = new ScrollableFrame();
        frame.setContentPadding(20);
        frame.setVerticalScrollEnabled(true);
        frame.setHorizontalScrollEnabled(false);

        ContainerWidget container = new ContainerWidget();
        container.layout().setFlexDirection(YogaFlexDirection.COLUMN);
        container.layout().setWidthPercent(100);
        container.layout().setPadding(YogaEdge.ALL, 5);
        container.layout().setGap(YogaGutter.COLUMN, 20);
        container.layout().setAlignItems(YogaAlign.FLEX_START);

        MaterialLabel title = MaterialLabel.create()
                .text("Material Select 组件示例")
                .fontSize(24)
                .color(materialScheme.primary())
                .scheme(materialScheme);
        title.layout().setMargin(YogaEdge.BOTTOM, 20);
        container.addChild(title);

        MaterialLabel basicLabel = MaterialLabel.create()
                .text("基本选择器")
                .fontSize(18)
                .color(materialScheme.onSurface())
                .scheme(materialScheme);
        container.addChild(basicLabel);

        MaterialSelect<String> basicSelect = MaterialSelect.<String>create()
                .label("Label")
                .placeholder("请选择...")
                .width(280)
                .addOption("option1", "Menu item")
                .addOption("option2", "Menu item")
                .addOption("option3", "Menu item")
                .addOption("option4", "Menu item")
                .scheme(materialScheme);
        basicSelect.layout().setMargin(YogaEdge.TOP, 8);
        container.addChild(basicSelect);

        MaterialLabel supportLabel = MaterialLabel.create()
                .text("带支持文本")
                .fontSize(18)
                .color(materialScheme.onSurface())
                .scheme(materialScheme);
        supportLabel.layout().setMargin(YogaEdge.TOP, 30);
        container.addChild(supportLabel);

        MaterialSelect<String> supportSelect = MaterialSelect.<String>create()
                .label("算法选择")
                .placeholder("选择超分算法")
                .supportingText("选择一个超分辨率算法")
                .width(300)
                .addOption("dlss", "DLSS")
                .addOption("fsr", "FSR 2.0")
                .addOption("xess", "XeSS")
                .addOption("native", "原生渲染")
                .setValue("dlss")
                .onSelectionChanged(value -> {
                    System.out.println("选择了: " + value);
                })
                .scheme(materialScheme);
        supportSelect.layout().setMargin(YogaEdge.TOP, 8);
        container.addChild(supportSelect);

        MaterialLabel iconLabel = MaterialLabel.create()
                .text("带前置图标")
                .fontSize(18)
                .color(materialScheme.onSurface())
                .scheme(materialScheme);
        iconLabel.layout().setMargin(YogaEdge.TOP, 30);
        container.addChild(iconLabel);

        MaterialSelect<String> iconSelect = MaterialSelect.<String>create()
                .label("质量预设")
                .leadingIcon(MaterialSymbols.iconTune())
                .width(300)
                .addOption("ultra", "Ultra Quality")
                .addOption("quality", "Quality")
                .addOption("balanced", "Balanced")
                .addOption("performance", "Performance")
                .addOption("ultra_performance", "Ultra Performance")
                .scheme(materialScheme);
        iconSelect.layout().setMargin(YogaEdge.TOP, 8);
        container.addChild(iconSelect);

        MaterialLabel numLabel = MaterialLabel.create()
                .text("数值选择")
                .fontSize(18)
                .color(materialScheme.onSurface())
                .scheme(materialScheme);
        numLabel.layout().setMargin(YogaEdge.TOP, 30);
        container.addChild(numLabel);

        MaterialSelect<Integer> numSelect = MaterialSelect.<Integer>create()
                .label("渲染比例")
                .width(200)
                .displayFormatter(v -> v + "%")
                .addOption(25, "25%")
                .addOption(50, "50%")
                .addOption(75, "75%")
                .addOption(100, "100%")
                .setValue(75)
                .scheme(materialScheme);
        numSelect.layout().setMargin(YogaEdge.TOP, 8);
        container.addChild(numSelect);

        MaterialLabel spacer = MaterialLabel.create().text("").scheme(materialScheme);
        spacer.layout().setHeight(1000);
        spacer.layout().setMargin(YogaEdge.VERTICAL, 1000);

        container.addChild(spacer);

        frame.setRoot(container);
        return frame;
    }

    private Frame createAboutFrame() {
        Frame frame = new Frame();
        ContainerWidget container = new ContainerWidget();
        container.layout().setFlexDirection(YogaFlexDirection.COLUMN);
        container.layout().setWidthPercent(100);
        container.layout().setHeightPercent(100);
        MaterialLabel label = MaterialLabel.create()
                .text("Super Resolution\nCopyright (c) 2026. 187J3X1-114514\n\n本项目为开源项目，遵循 GPLv3 协议。\nhttps://github.com/187J3X1-114514/SuperResolution")
                .fontSize(18)
                .color(materialScheme.onSurface())
                .scheme(materialScheme);
        label.layout().setMargin(YogaEdge.ALL, 30);
        container.addChild(label);
        frame.setRoot(container);
        return frame;
    }

    private Frame createEmptyFrame() {
        Frame frame = new Frame();
        ContainerWidget container = new ContainerWidget();
        container.layout().setFlexDirection(YogaFlexDirection.COLUMN);
        container.layout().setWidthPercent(100);
        container.layout().setHeightPercent(100);
        frame.setRoot(container);
        return frame;
    }

    @Override
    public void draw(RenderContext ctx, UIInputState inputState) {
        Vector2f screenSize = MinecraftWindow.getWindowSize();

        ctx.rect(
                0,
                0,
                screenSize.x,
                screenSize.y,
                materialScheme.background(),
                true);
        view.markLayoutDirty();
        view.getFrames().forEach(Frame::markLayoutDirty);
        super.draw(ctx, inputState);
    }

    @Override
    protected void dispatchKeyPressToFrame(int keyCode, int scancode, int modifiers) {
        if (keyCode == 293) { // GLFW_KEY_F4
            cycleDebugBoundsMode();
        }
        super.dispatchKeyPressToFrame(keyCode, scancode, modifiers);
    }

    private int debugBoundsMode = 0;

    private void cycleDebugBoundsMode() {
        debugBoundsMode = (debugBoundsMode + 1) % 5;
        switch (debugBoundsMode) {
            case 0 -> setDebugBoundsVisible(true, true, true);
            case 1 -> setDebugBoundsVisible(true, false, false);
            case 2 -> setDebugBoundsVisible(false, true, false);
            case 3 -> setDebugBoundsVisible(false, false, true);
            case 4 -> setDebugBoundsVisible(false, false, false);
        }
    }
}
