package io.homo.superresolution.core.gui;

import io.homo.superresolution.core.gui.core.backends.interfaces.IUIDrawContext;
import io.homo.superresolution.core.gui.core.backends.interfaces.TextAlign;
import io.homo.superresolution.core.gui.core.backends.interfaces.TextAlignType;
import io.homo.superresolution.core.gui.core.impl.Rectangle;
import io.homo.superresolution.core.gui.core.layout.AbsoluteLayout;
import io.homo.superresolution.core.gui.core.ContainerWidget;
import io.homo.superresolution.core.gui.core.layout.LinearLayout;
import io.homo.superresolution.core.gui.core.layout.ListLayout;
import io.homo.superresolution.core.gui.widgets.MaterialScrollableContainerWidget;
import io.homo.superresolution.core.gui.widgets.button.MaterialButton;
import io.homo.superresolution.core.gui.widgets.button.MaterialButtonSize;
import io.homo.superresolution.core.gui.widgets.button.MaterialButtonVariant;
import org.joml.Vector2f;
import io.homo.superresolution.core.utils.Color;
import io.homo.superresolution.core.utils.MinecraftUtil;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class WidgetDesignScreen extends NanoVGScreen<WidgetDesignScreen> {

    private MaterialScheme materialScheme;

    public WidgetDesignScreen(Component title) {
        super(title);
    }

    private ContainerWidget createLine(int line) {
        ContainerWidget linearContainer = new ContainerWidget();
        LinearLayout linearLayout = new LinearLayout();
        linearLayout.setLayoutBounds(new Rectangle(0, 0, 400, 60));
        linearLayout.setHorizontalGap(10);
        linearContainer.layout(linearLayout);
        MaterialButton btnElevated = MaterialButton.create()
                .text("Elevated " + line)
                .scheme(materialScheme)
                .size(MaterialButtonSize.ExtraSmall);
        btnElevated.style().variant(MaterialButtonVariant.Elevated);

        MaterialButton btnFilled = MaterialButton.create()
                .text("Filled " + line)
                .scheme(materialScheme)
                .size(MaterialButtonSize.Medium);
        btnFilled.style().variant(MaterialButtonVariant.Filled);

        MaterialButton btnTonal = MaterialButton.create()
                .text("Tonal " + line)
                .scheme(materialScheme)
                .size(MaterialButtonSize.ExtraSmall);
        btnTonal.style().variant(MaterialButtonVariant.Tonal);
        linearContainer.addChild(btnElevated);
        linearContainer.addChild(btnFilled);
        linearContainer.addChild(btnTonal);
        linearLayout.setElementPosition(
                btnElevated,
                LinearLayout.HorizontalAlignment.LEFT,
                LinearLayout.VerticalAlignment.TOP
        );
        linearLayout.setElementPosition(
                btnFilled,
                LinearLayout.HorizontalAlignment.CENTER,
                LinearLayout.VerticalAlignment.CENTER
        );
        linearLayout.setElementPosition(
                btnTonal,
                LinearLayout.HorizontalAlignment.RIGHT,
                LinearLayout.VerticalAlignment.BOTTOM
        );
        return linearContainer;
    }

    @Override
    protected void buildWidgets() {
        materialScheme = MaterialScheme.from(MaterialTheme.Dark, Color.from("#6750A4"));
        ContainerWidget rootContainer = new ContainerWidget();

        MaterialScrollableContainerWidget scrollableContainer = new MaterialScrollableContainerWidget();
        ListLayout listLayout = new ListLayout();
        listLayout.setLayoutBounds(new Rectangle(0, 0, 600, 400));
        listLayout.setVerticalGap(3);
        scrollableContainer.layout(listLayout);
        scrollableContainer.scheme(materialScheme);
        scrollableContainer.setViewRegion(new Vector2f(400, 700));

        List<ContainerWidget> lines = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            ContainerWidget line = createLine(i);
            lines.add(line);
            listLayout.setElementPosition(
                    line, ListLayout.HorizontalAlignment.LEFT, ListLayout.VerticalAlignment.TOP
            );
            scrollableContainer.addChild(line);
        }

        rootContainer.addChild(scrollableContainer);
        ((AbsoluteLayout) rootContainer.getLayout()).setPosition(scrollableContainer, new Vector2f(0, 0));
        addWidget(rootContainer);
    }

    @Override
    public void draw(IUIDrawContext drawContext, int mouseX, int mouseY, float delta) {
        drawContext.beginBatch();
        drawContext.drawRect(
                0,
                0,
                MinecraftUtil.getScreenSize().x,
                MinecraftUtil.getScreenSize().y,
                materialScheme.background(),
                true
        );
        drawContext.endBatch(-1);
        super.draw(drawContext, mouseX, mouseY, delta);
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

        float screenWidth = MinecraftUtil.getScreenSize().x;
        float screenHeight = MinecraftUtil.getScreenSize().y;
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
        drawContext.drawRect(
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
