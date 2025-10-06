package io.homo.superresolution.core.gui;

import io.homo.superresolution.core.graphics.nanovg.renderer.TextAlign;
import io.homo.superresolution.core.gui.core.UIDrawContext;
import io.homo.superresolution.core.gui.core.impl.Rectangle;
import io.homo.superresolution.core.gui.core.layout.AbsoluteLayout;
import io.homo.superresolution.core.gui.widgets.ContainerWidget;
import io.homo.superresolution.core.gui.widgets.button.MaterialButton;
import io.homo.superresolution.core.gui.widgets.button.MaterialButtonShape;
import io.homo.superresolution.core.gui.widgets.button.MaterialButtonSize;
import io.homo.superresolution.core.gui.widgets.button.MaterialButtonVariant;
import io.homo.superresolution.core.gui.widgets.switchs.MaterialSwitch;
import io.homo.superresolution.core.math.Vector2f;
import io.homo.superresolution.core.utils.Color;
import io.homo.superresolution.core.utils.MinecraftUtil;
import net.minecraft.network.chat.Component;
import org.lwjgl.nanovg.NVGPaint;

import static org.lwjgl.nanovg.NanoVG.*;
import static org.lwjgl.nanovg.NanoVG.nvgFill;
import static org.lwjgl.nanovg.NanoVG.nvgFillPaint;

public class WidgetDesignScreen extends NanoVGScreen<WidgetDesignScreen> {

    private MaterialScheme materialScheme;
    private ContainerWidget container;

    public WidgetDesignScreen(Component title) {
        super(title);
    }

    @Override
    protected void buildWidgets() {
        materialScheme = MaterialScheme.from(MaterialTheme.Dark, Color.from("#6750A4"));
        ContainerWidget rootContainer = new ContainerWidget();
        ContainerWidget btnContainer = new ContainerWidget();
        container = new ContainerWidget();
        container.layout(new AbsoluteLayout());
        rootContainer.layout(new AbsoluteLayout());
        btnContainer.layout(new AbsoluteLayout());
        rootContainer.addChild(btnContainer);
        rootContainer.addChild(container);
        MaterialSwitch switchA = MaterialSwitch.create().scheme(materialScheme);
        MaterialSwitch switchB = MaterialSwitch.create().scheme(materialScheme);
        MaterialSwitch switchC = MaterialSwitch.create().scheme(materialScheme);
        MaterialSwitch switchD = MaterialSwitch.create().scheme(materialScheme);
        MaterialSwitch switchE = MaterialSwitch.create().scheme(materialScheme);
        MaterialSwitch switchF = MaterialSwitch.create().scheme(materialScheme);
        MaterialButton buttonElevated = MaterialButton.create()
                .text("Elevated").icon(MaterialSymbols.iconEdit())
                .scheme(materialScheme).size(MaterialButtonSize.Small);
        MaterialButton buttonFilled = MaterialButton.create().text("Filled").scheme(materialScheme).size(MaterialButtonSize.Small);
        MaterialButton buttonTonal = MaterialButton.create().text("Tonal").scheme(materialScheme).size(MaterialButtonSize.Small);
        MaterialButton buttonOutlined = MaterialButton.create().text("Outlined").scheme(materialScheme).size(MaterialButtonSize.Small);
        MaterialButton buttonText = MaterialButton.create().text("Text").size(MaterialButtonSize.Small).icon(MaterialSymbols.iconEdit()).scheme(materialScheme);
        buttonElevated.style().variant(MaterialButtonVariant.Elevated);
        buttonFilled.style().variant(MaterialButtonVariant.Filled);
        buttonTonal.style().variant(MaterialButtonVariant.Tonal);
        buttonOutlined.style().variant(MaterialButtonVariant.Outlined);
        buttonText.style().variant(MaterialButtonVariant.Text).shape(MaterialButtonShape.Square);
        AbsoluteLayout layout = (AbsoluteLayout) btnContainer.getLayout();
        btnContainer.addChild(buttonElevated);
        btnContainer.addChild(buttonFilled);
        btnContainer.addChild(buttonTonal);
        btnContainer.addChild(buttonOutlined);
        btnContainer.addChild(buttonText);
        layout.setPosition(buttonElevated, new Vector2f(50, 50));
        layout.setPosition(buttonFilled, new Vector2f(50, 100));
        layout.setPosition(buttonTonal, new Vector2f(50, 150));
        layout.setPosition(buttonOutlined, new Vector2f(50, 200));
        layout.setPosition(buttonText, new Vector2f(50, 250));
        layout = (AbsoluteLayout) container.getLayout();
        switchB.style().showCheckedIconWhenEnable(true);
        switchC.style().showUncheckedIconWhenEnable(true);
        switchD.style().showCheckedIconAlways(true);
        switchE.style().showUncheckedIconAlways(true);
        switchF.style().showCheckedIconAlways(true).showUncheckedIconAlways(true);
        container.addChild(switchA);
        container.addChild(switchB);
        container.addChild(switchC);
        container.addChild(switchD);
        container.addChild(switchE);
        container.addChild(switchF);
        layout.setPosition(switchA, new Vector2f(50, 50));
        layout.setPosition(switchF, new Vector2f(110, 50));
        layout.setPosition(switchB, new Vector2f(50, 110));
        layout.setPosition(switchC, new Vector2f(110, 110));
        layout.setPosition(switchD, new Vector2f(50, 170));
        layout.setPosition(switchE, new Vector2f(110, 170));

        layout = (AbsoluteLayout) rootContainer.getLayout();
        layout.setPosition(btnContainer, new Vector2f(0, 0));
        layout.setPosition(container, new Vector2f(300, 0));
        addWidget(rootContainer);
    }

    @Override
    public void draw(UIDrawContext drawContext, int mouseX, int mouseY, float delta) {

        drawContext.drawRect(
                0,
                0,
                MinecraftUtil.getScreenSize().x,
                MinecraftUtil.getScreenSize().y,
                materialScheme.background(),
                true
        );
        super.draw(drawContext, mouseX, mouseY, delta);
        this.setTransparent(true);

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
                // 添加Fixed系列颜色
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
                // 添加控制颜色
                materialScheme.controlActivated(),
                materialScheme.controlNormal(),
                materialScheme.controlHighlight(),
                // 添加文本颜色
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
        int colorsPerColumn = 30; // 计算每列的颜色数量，向上取整

        float screenWidth = MinecraftUtil.getScreenSize().x;
        float screenHeight = MinecraftUtil.getScreenSize().y;
        float columnWidth = screenWidth * 0.2f; // 每列宽度为屏幕宽度的20%
        float leftColumnX = screenWidth * 0.6f; // 左列起始位置在屏幕75%处
        float rightColumnX = screenWidth * 0.8f; // 右列起始位置在屏幕95%处

        // 绘制左列
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

        // 绘制右列
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
        /*
        nvgReset(drawContext.nvg().contextPtr);
        NVGPaint gpaint = nvgRadialGradient(
                drawContext.nvg().contextPtr,
                350, 350, 0, 100,
                materialScheme.primary().nvg(),
                materialScheme.onError().nvg(),
                NVGPaint.calloc()
        );
        nvgBeginPath(drawContext.nvg().contextPtr);
        nvgRect(drawContext.nvg().contextPtr, 200, 200, 300, 300);
        nvgFillPaint(drawContext.nvg().contextPtr, gpaint);
        nvgFill(drawContext.nvg().contextPtr);
        */
    }

    private void drawRibbonWithText(UIDrawContext drawContext, Rectangle rectangle, Color color, String str) {
        drawContext.drawRect(
                rectangle.x / nvg.globalScale,
                rectangle.y / nvg.globalScale,
                rectangle.width / nvg.globalScale,
                rectangle.height / nvg.globalScale,
                color,
                true
        );
        drawContext.text().drawAlignedText(
                drawContext.font(),
                14f,
                str,
                rectangle.getCenterX() / nvg.globalScale,
                rectangle.getCenterY() / nvg.globalScale,
                rectangle.width / nvg.globalScale,
                14f,
                Color.rgb(255 - color.red(), 255 - color.green(), 255 - color.blue()),
                TextAlign.of(TextAlign.ALIGN_CENTER, TextAlign.ALIGN_MIDDLE),
                false
        );
    }
}
