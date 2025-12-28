package io.homo.superresolution.core.gui;

import io.homo.superresolution.common.minecraft.MinecraftWindow;
import io.homo.superresolution.core.gui.core.UIInputState;
import io.homo.superresolution.core.gui.core.backends.interfaces.IUIDrawContext;
import io.homo.superresolution.core.gui.core.backends.interfaces.TextAlign;
import io.homo.superresolution.core.gui.core.backends.interfaces.TextAlignType;
import io.homo.superresolution.core.gui.core.backends.interfaces.Transform;
import io.homo.superresolution.core.gui.core.frame.Frame;
import io.homo.superresolution.core.gui.core.impl.Rectangle;
import io.homo.superresolution.core.gui.core.ContainerWidget;
import io.homo.superresolution.core.gui.widgets.MaterialScrollableContainerWidget;
import io.homo.superresolution.core.gui.widgets.button.MaterialButton;
import io.homo.superresolution.core.gui.widgets.button.MaterialButtonSize;
import io.homo.superresolution.core.gui.widgets.button.MaterialButtonVariant;
import io.homo.superresolution.core.gui.widgets.label.MaterialLabel;
import io.homo.superresolution.core.gui.widgets.menu.*;
import io.homo.superresolution.core.gui.widgets.sliders.MaterialSlider;
import io.homo.superresolution.core.gui.widgets.sliders.MaterialSliderSize;
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

        Frame buttonFrame = createButtonFrame();
        YogaNode buttonLayout = getView().addFrame(buttonFrame);
        buttonLayout.setWidthPercent(25);
        buttonLayout.setHeightPercent(100);
        buttonLayout.setPadding(YogaEdge.ALL, 10);

        Frame sliderSwitchFrame = createSliderSwitchFrame();
        YogaNode sliderSwitchLayout = getView().addFrame(sliderSwitchFrame);
        sliderSwitchLayout.setWidthPercent(25);
        sliderSwitchLayout.setHeightPercent(100);
        sliderSwitchLayout.setPadding(YogaEdge.ALL, 10);

        Frame menuLabelFrame = createMenuLabelFrame();
        YogaNode menuLabelLayout = getView().addFrame(menuLabelFrame);
        menuLabelLayout.setWidthPercent(25);
        menuLabelLayout.setHeightPercent(100);
        menuLabelLayout.setPadding(YogaEdge.ALL, 10);

        view.setDebugRenderEnabled(true);
    }

    private Frame createButtonFrame() {
        Frame frame = new Frame();
        ContainerWidget container = new ContainerWidget();
        container.layout().setFlexDirection(YogaFlexDirection.COLUMN);
        container.layout().setWidthPercent(100);
        container.layout().setHeightPercent(100);
        container.layout().setGap(YogaGutter.COLUMN, 20);
        container.layout().setAlignItems(YogaAlign.CENTER);
        container.layout().setJustifyContent(YogaJustify.FLEX_START);

        MaterialLabel title = MaterialLabel.create()
                .text("Material Buttons")
                .fontSize(24)
                .color(materialScheme.primary())
                .scheme(materialScheme);
        title.layout().setMargin(YogaEdge.BOTTOM, 20);
        container.addChild(title);

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

        MaterialButton iconBtn = MaterialButton.filled("Icon Button", materialScheme)
                .icon(MaterialSymbols.iconAdd())
                .size(MaterialButtonSize.Medium);
        iconBtn.layout().setMargin(YogaEdge.VERTICAL, 5);
        container.addChild(iconBtn);

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

        frame.setRoot(container);
        return frame;
    }

    /**
     * 创建 Slider 和 Switch 示例 Frame
     */
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

    /**
     * 创建 Menu 和 Label 示例 Frame
     */
    private Frame createMenuLabelFrame() {
        Frame frame = new Frame();
        ContainerWidget container = new ContainerWidget();
        container.layout().setFlexDirection(YogaFlexDirection.COLUMN);
        container.layout().setWidthPercent(100);
        container.layout().setHeightPercent(100);
        container.layout().setGap(YogaGutter.COLUMN, 20);
        container.layout().setAlignItems(YogaAlign.CENTER);
        container.layout().setJustifyContent(YogaJustify.FLEX_START);

        MaterialLabel labelTitle = MaterialLabel.create()
                .text("Material Labels")
                .fontSize(24)
                .color(materialScheme.primary())
                .scheme(materialScheme);
        labelTitle.layout().setMargin(YogaEdge.BOTTOM, 20);
        container.addChild(labelTitle);

        MaterialLabel label1 = MaterialLabel.create()
                .text("Primary Color Label")
                .fontSize(18)
                .color(materialScheme.primary())
                .scheme(materialScheme);
        label1.layout().setMargin(YogaEdge.VERTICAL, 5);
        container.addChild(label1);

        MaterialLabel label2 = MaterialLabel.create()
                .text("Secondary Color Label")
                .fontSize(18)
                .color(materialScheme.secondary())
                .scheme(materialScheme);
        label2.layout().setMargin(YogaEdge.VERTICAL, 5);
        container.addChild(label2);

        MaterialLabel label3 = MaterialLabel.create()
                .text("Error Color Label")
                .fontSize(18)
                .color(materialScheme.error())
                .scheme(materialScheme);
        label3.layout().setMargin(YogaEdge.VERTICAL, 5);
        container.addChild(label3);

        MaterialLabel label4 = MaterialLabel.create()
                .text("Default OnSurface Label")
                .fontSize(16)
                .scheme(materialScheme);
        label4.layout().setMargin(YogaEdge.VERTICAL, 5);
        container.addChild(label4);

        MaterialLabel menuTitle = MaterialLabel.create()
                .text("Material Menu")
                .fontSize(24)
                .color(materialScheme.primary())
                .scheme(materialScheme);
        menuTitle.layout().setMargin(YogaEdge.TOP, 40);
        menuTitle.layout().setMargin(YogaEdge.BOTTOM, 20);
        container.addChild(menuTitle);

        MaterialMenuGroup group1 = MaterialMenuGroup.create()
                .addItem(MaterialMenuItem.create()
                        .text("Option 1")
                        .selectable(true)
                        .value("option1")
                        .icon(MaterialSymbols.iconAdd()))
                .addItem(MaterialMenuItem.create()
                        .text("Option 2")
                        .selectable(true)
                        .value("option2")
                        .icon(MaterialSymbols.iconEdit()))
                .addItem(MaterialMenuItem.create()
                        .text("Option 3")
                        .selectable(true)
                        .value("option3")
                        .icon(MaterialSymbols.iconDelete()));

        MaterialMenuGroup group2 = MaterialMenuGroup.create()
                .addItem(MaterialMenuItem.create()
                        .text("Action 1")
                        .selectable(false)
                        .value("action1")
                        .onClick(event -> System.out.println("Action 1 clicked")))
                .addItem(MaterialMenuItem.create()
                        .text("Action 2")
                        .selectable(false)
                        .value("action2"))
                .addItem(MaterialMenuItem.create()
                        .text("Disabled")
                        .selectable(false)
                        .setDisabled(true)
                        .value("disabled"));

        MaterialMenu menu = MaterialMenu.create()
                .selectionMode(MaterialMenuSelectionMode.Single)
                .addGroup(group1)
                .addGroup(group2)
                .selectItemQuietly("option1")
                .scheme(materialScheme);
        menu.style().colors(MaterialMenuColors.STANDARD);
        menu.layout().setWidth(250);
        menu.layout().setMargin(YogaEdge.VERTICAL, 10);

        MaterialButton toggleBtn = MaterialButton.elevated("Toggle Menu", materialScheme)
                .size(MaterialButtonSize.Small);
        toggleBtn.onClick(e -> menu.toggle());
        toggleBtn.layout().setMargin(YogaEdge.TOP, 20);
        container.addChild(toggleBtn);
        container.addChild(menu);

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
