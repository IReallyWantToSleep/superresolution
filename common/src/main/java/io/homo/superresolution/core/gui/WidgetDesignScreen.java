package io.homo.superresolution.core.gui;

import io.homo.superresolution.common.minecraft.MinecraftWindow;
import io.homo.superresolution.core.gui.core.UIInputState;
import io.homo.superresolution.core.gui.core.backends.interfaces.IUIDrawContext;
import io.homo.superresolution.core.gui.core.backends.interfaces.TextAlign;
import io.homo.superresolution.core.gui.core.backends.interfaces.TextAlignType;
import io.homo.superresolution.core.gui.core.impl.Rectangle;
import io.homo.superresolution.core.gui.core.ContainerWidget;
import io.homo.superresolution.core.gui.widgets.MaterialScrollableContainerWidget;
import io.homo.superresolution.core.gui.widgets.button.MaterialButton;
import io.homo.superresolution.core.gui.widgets.button.MaterialButtonSize;
import io.homo.superresolution.core.gui.widgets.menu.*;
import io.homo.superresolution.core.gui.widgets.sliders.MaterialSlider;
import io.homo.superresolution.core.gui.widgets.sliders.MaterialSliderSize;
import io.homo.superresolution.core.gui.widgets.switchs.MaterialSwitch;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.*;
import org.joml.Vector2f;
import io.homo.superresolution.core.utils.Color;
import net.minecraft.network.chat.Component;

public class WidgetDesignScreen extends NanoVGScreen<WidgetDesignScreen> {

    private ContainerWidget rootContainer;
    private MaterialScheme materialScheme;

    public WidgetDesignScreen(Component title) {
        super(title);
    }


    private ContainerWidget createLine(int line) {
        ContainerWidget lineContainer = new ContainerWidget();
        lineContainer.layout().setWidthPercent(100);
        lineContainer.setElementHeight(60);
        lineContainer.layout().setFlexDirection(YogaFlexDirection.ROW);
        lineContainer.layout().setJustifyContent(YogaJustify.SPACE_BETWEEN);
        lineContainer.layout().setAlignItems(YogaAlign.STRETCH);
        MaterialButton btn = MaterialButton.filled("嘻嘻 😋", materialScheme)
                .size(MaterialButtonSize.Medium);
        MaterialSlider btnFilled = MaterialSlider.create()
                .setMax(100)
                .setMin(-100)
                .setStep(0.1f)
                .setValue(-10)
                .scheme(materialScheme);
        btnFilled.setValueIndicatorTextFormater((value) -> String.format("%.1f%%", value.doubleValue()));
        btnFilled.style().valueIndicator(true);
        btnFilled.style().size(MaterialSliderSize.Small);
        btn.layout().setAlignSelf(YogaAlign.CENTER);
        btnFilled.layout().setAlignSelf(YogaAlign.CENTER);
        btnFilled.layout().setMargin(YogaEdge.RIGHT, 5);
        btn.layout().setMargin(YogaEdge.RIGHT, 5);
        MaterialSwitch switch0 = MaterialSwitch.create()
                .scheme(materialScheme)
                .setChecked(true);
        switch0.style().showCheckedIconWhenEnable(false);
        switch0.layout().setAlignSelf(YogaAlign.CENTER);
        lineContainer.addChild(btn);
        lineContainer.addChild(btnFilled);
        lineContainer.addChild(switch0);

        return lineContainer;
    }

    @Override
    protected void buildWidgets() {
        materialScheme = MaterialScheme.from(MaterialTheme.Dark, Color.from("#6750A4"));
        rootContainer = new ContainerWidget();
        MaterialScrollableContainerWidget scrollableContainer = new MaterialScrollableContainerWidget();
        scrollableContainer.scheme(materialScheme);
        scrollableContainer.setElementSize(750, 480);
        scrollableContainer.setViewRegion(new Vector2f(750, 480));

        scrollableContainer.setHorizontalScrollEnabled(false);
        scrollableContainer.setVerticalScrollEnabled(true);

        scrollableContainer.layout().setFlexDirection(YogaFlexDirection.COLUMN);
        scrollableContainer.layout().setGap(YogaGutter.COLUMN, 4);
        for (int i = 0; i < 20; i++) {
            ContainerWidget line = createLine(i);
            scrollableContainer.addChild(line);
        }
        {
            MaterialMenuGroup group1 = MaterialMenuGroup.create()
                    .addItem(MaterialMenuItem.create()
                            .text("Option 1")
                            .selectable(true)
                            .value("option1")
                            .onSelectionChanged(selected -> System.out.println("Option 1: " + selected)))
                    .addItem(MaterialMenuItem.create()
                            .text("Option 2")
                            .selectable(true)
                            .value("option2"))
                    .addItem(MaterialMenuItem.create()
                            .text("Action 3")
                            .selectable(false)
                            .value("action3"));
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
                            .text("Option 3")
                            .selectable(true)
                            .value("option3"));
            MaterialMenu menu = MaterialMenu.create()
                    .selectionMode(MaterialMenuSelectionMode.SingleAtLeastOne)
                    .addGroup(group1)
                    .addGroup(group2)
                    .selectItemQuietly("option2");
            menu.style().colors(MaterialMenuColors.VIBRANT);
            menu.layout().setWidth(200);
            menu.layout().setAlignSelf(YogaAlign.CENTER);
            menu.scheme(materialScheme);
            scrollableContainer.addChild(menu);
        }
        {
            MaterialMenuGroup group1 = MaterialMenuGroup.create()
                    .addItem(MaterialMenuItem.create()
                            .text("Option 1")
                            .selectable(true)
                            .value("option1")
                            .onSelectionChanged(selected -> System.out.println("Option 1: " + selected)))
                    .addItem(MaterialMenuItem.create()
                            .text("Option 2")
                            .selectable(true)
                            .value("option2"))
                    .addItem(MaterialMenuItem.create()
                            .text("Action 3")
                            .selectable(false)
                            .value("action3"));
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
                            .text("Option 3")
                            .selectable(true)
                            .value("option3"));
            MaterialMenu menu = MaterialMenu.create()
                    .selectionMode(MaterialMenuSelectionMode.SingleAtLeastOne)
                    .addGroup(group1)
                    .addGroup(group2)
                    .selectItemQuietly("option2");
            menu.style().colors(MaterialMenuColors.STANDARD);
            menu.layout().setWidth(200);
            menu.layout().setAlignSelf(YogaAlign.CENTER);
            menu.scheme(materialScheme);
            scrollableContainer.addChild(menu);
        }


        rootContainer.addChild(scrollableContainer);
        //rootContainer.layout().setPositionType(YogaPositionType.ABSOLUTE);
        rootContainer.layout().setPosition(YogaEdge.LEFT, 50);
        rootContainer.layout().setPosition(YogaEdge.TOP, 100);
        Vector2f screenSize = MinecraftWindow.getWindowSize();
        rootContainer.setElementSize(screenSize.x, screenSize.y);
        rootContainer.getLayoutNode().setDebugName("Root");
        addWidget(rootContainer);
    }

    @Override
    public void draw(IUIDrawContext drawContext, UIInputState inputState) {
        Vector2f screenSize = MinecraftWindow.getWindowSize();
        rootContainer.getLayoutNode().setWidth(screenSize.x);
        rootContainer.getLayoutNode().setHeight(screenSize.y);
        rootContainer.getLayoutNode().setPosition(YogaEdge.TOP, 100);
        rootContainer.getLayoutNode().calculateLayout(screenSize.x, screenSize.y);
        drawContext.beginBatch();
        drawContext.rect(
                0,
                0,
                MinecraftWindow.getWindowSize().x,
                MinecraftWindow.getWindowSize().y,
                materialScheme.background(),
                true
        );
        drawContext.endBatch(-1);
        super.draw(drawContext, inputState);
        this.setTransparent(true);
        drawContext.beginBatch();
        Color[] colors = {
                materialScheme.primary(),
                materialScheme.onPrimary(),
                materialScheme.primaryContainer(),
                materialScheme.onPrimaryContainer(),
                materialScheme.secondary(),
                materialScheme.onSecondary(),
                materialScheme.secondaryContainer(),
                materialScheme.onSecondaryContainer(),
                materialScheme.tertiary(),
                materialScheme.onTertiary(),
                materialScheme.tertiaryContainer(),
                materialScheme.onTertiaryContainer(),
                materialScheme.error(),
                materialScheme.onError(),
                materialScheme.errorContainer(),
                materialScheme.onErrorContainer(),
                materialScheme.background(),
                materialScheme.onBackground(),
                materialScheme.surface(),
                materialScheme.onSurface(),
                materialScheme.surfaceVariant(),
                materialScheme.onSurfaceVariant(),
                materialScheme.outline(),
                materialScheme.outlineVariant(),
                materialScheme.shadow(),
                materialScheme.scrim(),
                materialScheme.inverseSurface(),
                materialScheme.inverseOnSurface(),
                materialScheme.inversePrimary(),
                materialScheme.primaryFixed(),
                materialScheme.primaryFixedDim(),
                materialScheme.onPrimaryFixed(),
                materialScheme.onPrimaryFixedVariant(),
                materialScheme.secondaryFixed(),
                materialScheme.secondaryFixedDim(),
                materialScheme.onSecondaryFixed(),
                materialScheme.onSecondaryFixedVariant(),
                materialScheme.tertiaryFixed(),
                materialScheme.tertiaryFixedDim(),
                materialScheme.onTertiaryFixed(),
                materialScheme.onTertiaryFixedVariant(),
                materialScheme.controlActivated(),
                materialScheme.controlNormal(),
                materialScheme.controlHighlight(),
                materialScheme.textPrimaryInverse(),
                materialScheme.textSecondaryAndTertiaryInverse(),
                materialScheme.textPrimaryInverseDisableOnly(),
                materialScheme.textSecondaryAndTertiaryInverseDisabled(),
                materialScheme.textHintInverse()
        };

        String[] colorNames = {
                "Primary", "On Primary", "Primary Container", "On Primary Container",
                "Secondary", "On Secondary", "Secondary Container", "On Secondary Container",
                "Tertiary", "On Tertiary", "Tertiary Container", "On Tertiary Container",
                "Error", "On Error", "Error Container", "On Error Container",
                "Background", "On Background", "Surface", "On Surface",
                "Surface Variant", "On Surface Variant", "Outline", "Outline Variant",
                "Shadow", "Scrim", "Inverse Surface", "Inverse On Surface", "Inverse Primary",
                "Primary Fixed", "Primary Fixed Dim", "On Primary Fixed", "On Primary Fixed Variant",
                "Secondary Fixed", "Secondary Fixed Dim", "On Secondary Fixed", "On Secondary Fixed Variant",
                "Tertiary Fixed", "Tertiary Fixed Dim", "On Tertiary Fixed", "On Tertiary Fixed Variant",
                "Control Activated", "Control Normal", "Control Highlight",
                "Text Primary Inverse", "Text Secondary And Tertiary Inverse",
                "Text Primary Inverse Disable Only", "Text Secondary And Tertiary Inverse Disabled",
                "Text Hint Inverse"
        };

        int colorCount = colors.length;
        int colorsPerColumn = 30;

        float screenWidth = MinecraftWindow.getWindowSize().x;
        float screenHeight = MinecraftWindow.getWindowSize().y;
        float columnWidth = screenWidth * 0.2f;
        float leftColumnX = screenWidth * 0.6f;
        float rightColumnX = screenWidth * 0.8f;

        for (int i = 0; i < colorsPerColumn; i++) {
            float yPos = screenHeight * (i / (float) colorsPerColumn);
            float height = screenHeight / colorsPerColumn;

            drawRibbonWithText(
                    drawContext,
                    new Rectangle(
                            leftColumnX,
                            yPos,
                            columnWidth,
                            height
                    ),
                    colors[i],
                    colorNames[i]
            );
        }

        for (int i = colorsPerColumn; i < colorCount; i++) {
            int rightColumnIndex = i - colorsPerColumn;
            float yPos = screenHeight * (rightColumnIndex / (float) (colorCount - colorsPerColumn));
            float height = screenHeight / (colorCount - colorsPerColumn);

            drawRibbonWithText(
                    drawContext,
                    new Rectangle(
                            rightColumnX,
                            yPos,
                            columnWidth,
                            height
                    ),
                    colors[i],
                    colorNames[i]
            );
        }
        drawContext.endBatch(0);
    }

    private void drawRibbonWithText(IUIDrawContext drawContext, Rectangle rectangle, Color color, String str) {
        drawContext.rect(
                rectangle.x / nvg.globalScale,
                rectangle.y / nvg.globalScale,
                rectangle.width / nvg.globalScale,
                rectangle.height / nvg.globalScale,
                color,
                true
        );
        drawContext.drawAlignedText(
                drawContext.font(),
                14f,
                str,
                rectangle.getCenterX() / nvg.globalScale,
                rectangle.getCenterY() / nvg.globalScale,
                rectangle.width / nvg.globalScale,
                14f,
                Color.rgb(255 - color.red(), 255 - color.green(), 255 - color.blue()),
                TextAlign.of(TextAlignType.ALIGN_CENTER, TextAlignType.ALIGN_MIDDLE),
                false
        );
    }
}
