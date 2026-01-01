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

import io.homo.superresolution.common.minecraft.MinecraftWindow;
import io.homo.superresolution.core.gui.core.UIInputState;
import io.homo.superresolution.core.gui.core.backends.interfaces.IUIDrawContext;
import io.homo.superresolution.core.gui.core.backends.interfaces.TextAlign;
import io.homo.superresolution.core.gui.core.backends.interfaces.TextAlignType;
import io.homo.superresolution.core.gui.core.backends.interfaces.Transform;
import io.homo.superresolution.core.gui.core.frame.Frame;
import io.homo.superresolution.core.gui.core.frame.ScrollableFrame;
import io.homo.superresolution.core.gui.core.impl.Rectangle;
import io.homo.superresolution.core.gui.core.ContainerWidget;
import io.homo.superresolution.core.gui.widgets.button.MaterialButton;
import io.homo.superresolution.core.gui.widgets.button.MaterialButtonSize;
import io.homo.superresolution.core.gui.widgets.label.MaterialLabel;
import io.homo.superresolution.core.gui.widgets.menu.*;
import io.homo.superresolution.core.gui.widgets.navigation.drawer.MaterialNavigationDrawer;
import io.homo.superresolution.core.gui.widgets.sliders.MaterialSlider;
import io.homo.superresolution.core.gui.widgets.switchs.MaterialSwitch;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.*;
import org.joml.Vector2f;
import io.homo.superresolution.core.utils.Color;
import net.minecraft.network.chat.Component;

public class WidgetDesignScreen extends NanoVGScreen<WidgetDesignScreen> {

    private MaterialScheme materialScheme;

    public WidgetDesignScreen(Component title) {
        super(title);
    }

    @Override
    protected void buildWidgets() {
        materialScheme = MaterialScheme.from(MaterialTheme.Dark, Color.from("#6750A4"));

        getView().removeFrame(getDefaultFrame());
        Frame navigationDrawerFrame = createNavigationDrawerFrame();
        YogaNode navigationDrawerLayout = getView().addFrame(navigationDrawerFrame);
        navigationDrawerLayout.setWidthPercent(17.1f);
        navigationDrawerLayout.setHeightPercent(100);
        navigationDrawerLayout.setPadding(YogaEdge.ALL, 0);
        Frame buttonFrame = createButtonFrame();
        YogaNode buttonLayout = getView().addFrame(buttonFrame);
        buttonLayout.setWidthPercent(25);
        buttonLayout.setPadding(YogaEdge.ALL, 10);

        Frame sliderSwitchFrame = createSliderSwitchFrame();
        YogaNode sliderSwitchLayout = getView().addFrame(sliderSwitchFrame);
        sliderSwitchLayout.setWidthPercent(25);
        sliderSwitchLayout.setHeightPercent(100);
        sliderSwitchLayout.setPadding(YogaEdge.ALL, 10);


        view.setDebugRenderEnabled(true);
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

    private Frame createNavigationDrawerFrame() {
        Frame frame = new Frame();
        ContainerWidget container = new ContainerWidget();
        container.layout().setFlexDirection(YogaFlexDirection.COLUMN);
        container.layout().setWidthPercent(100);
        container.layout().setHeightPercent(100);

        MaterialNavigationDrawer drawer = MaterialNavigationDrawer.create()
                .addHeader("Super Resolution", MaterialSymbols.iconSettings())
                .addSectionHeader("配置")
                .addItem("通用", MaterialSymbols.iconSettings(), "general")
                .addItem("算法", MaterialSymbols.iconSettings(), "algorithm")
                .addItem("界面", MaterialSymbols.iconSettings(), "interface")
                .addItem("调试", MaterialSymbols.iconSettings(), "debug")
                .addDivider()
                .addSectionHeader("信息")
                .addItem("性能信息", MaterialSymbols.iconSettings(), "performance")
                .addItem("环境信息", MaterialSymbols.iconSettings(), "environment")
                .addDivider()
                .onItemSelected(item -> {
                    System.out.println("Selected: " + item.getValue());
                })
                .setSelectedByValue("general")
                .addFlexibleSpacer()
                .addItem("关于", MaterialSymbols.iconSettings(), "about")
                .scheme(materialScheme);
        drawer.layout().setWidthPercent(100);
        drawer.layout().setHeightPercent(100);
        container.addChild(drawer);

        frame.setRoot(container);
        return frame;
    }

    @Override
    public void draw(IUIDrawContext drawContext, UIInputState inputState) {
        Vector2f screenSize = MinecraftWindow.getWindowSize();

        drawContext.beginBatch();
        drawContext.rect(
                0,
                0,
                screenSize.x,
                screenSize.y,
                materialScheme.background(),
                true
        );
        drawContext.endBatch(-1);
        view.markLayoutDirty();
        view.getFrames().forEach(Frame::markLayoutDirty);
        super.draw(drawContext, inputState);
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
