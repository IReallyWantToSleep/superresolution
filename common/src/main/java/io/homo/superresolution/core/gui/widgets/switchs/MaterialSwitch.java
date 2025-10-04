/*
 * Super Resolution
 * Copyright (c) 2025. 187J3X1-114514
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

package io.homo.superresolution.core.gui.widgets.switchs;

import io.homo.superresolution.core.graphics.nanovg.NanoVG;
import io.homo.superresolution.core.graphics.nanovg.renderer.TextAlign;
import io.homo.superresolution.core.gui.MaterialRipple;
import io.homo.superresolution.core.gui.MaterialSymbol;
import io.homo.superresolution.core.gui.core.UIDrawContext;
import io.homo.superresolution.core.gui.core.UIInputState;
import io.homo.superresolution.core.gui.core.animator.Easing;
import io.homo.superresolution.core.gui.core.impl.Rectangle;
import io.homo.superresolution.core.gui.widgets.MaterialWidget;
import io.homo.superresolution.core.math.Vector2f;
import io.homo.superresolution.core.utils.Color;
import io.homo.superresolution.core.utils.MouseCursor;

import java.util.function.Supplier;

public class MaterialSwitch extends MaterialWidget<MaterialSwitch, MaterialSwitchStyle, MaterialSwitchAnimationSet> {
    private Supplier<String> textContextSupplier = () -> null;
    private Rectangle rectangle = new Rectangle();
    private Vector2f position;
    private MaterialRipple ripple;
    private Vector2f lastClickPosition;
    private Supplier<MaterialSymbol> iconContextSupplier = () -> null;

    public MaterialSwitch(MaterialSwitchSize size, Vector2f position) {
        this.style = new MaterialSwitchStyle();
        this.ripple = new MaterialRipple();
        this.style.size(size);
        this.position = position;
        updateRectangle();
    }

    public static MaterialSwitch create(Vector2f position) {
        return create(position, MaterialSwitchSize.Medium);
    }

    public static MaterialSwitch create(Vector2f position, MaterialSwitchSize size) {
        return new MaterialSwitch(size, position);
    }

    public static MaterialSwitch create(Rectangle rectangle) {
        float iconSizeRatio = 0.1f;
        float iconPaddingRatio = 0.038f;
        float paddingRatio = 0.1525f;
        float squareCornerSizeRatio = 0.07f;
        float pressedCornerSizeRatio = 0.07f * 0.7f;
        float fontSizeRatio = 0.076f;

        MaterialSwitchSize buttonSize = new MaterialSwitchSize(
                rectangle.height,
                rectangle.width * paddingRatio,
                rectangle.width * iconPaddingRatio,
                0,
                rectangle.width * squareCornerSizeRatio,
                rectangle.width * pressedCornerSizeRatio,
                rectangle.width * iconSizeRatio,
                rectangle.width * fontSizeRatio
        );
        return new MaterialSwitch(buttonSize, new Vector2f(rectangle.x, rectangle.y));
    }

    @Override
    protected void init() {
        this.animationSet = new MaterialSwitchAnimationSet();
        onMouseHover((widget, mousePosition, hover) -> onHover(mousePosition, hover));
        onMouseRelease((widget, mousePosition) -> onRelease(mousePosition));
        onMousePress((widget, mousePosition) -> onPress(mousePosition));
    }

    public Vector2f position() {
        return position;
    }

    public MaterialSwitch position(Vector2f position) {
        this.position = position;
        updateRectangle();
        return this;
    }

    public MaterialSwitchSize size() {
        return style.size();
    }

    public MaterialSwitch size(MaterialSwitchSize size) {
        this.style.size(size);
        updateRectangle();
        return this;
    }

    public MaterialSwitch text(String string) {
        this.textContextSupplier = () -> string;
        updateRectangle();
        return this;
    }

    public MaterialSwitch text(Supplier<String> supplier) {
        this.textContextSupplier = supplier;
        updateRectangle();
        return this;
    }

    public MaterialSwitch icon(MaterialSymbol icon) {
        this.iconContextSupplier = () -> icon;
        updateRectangle();
        return this;
    }

    public MaterialSwitch icon(Supplier<MaterialSymbol> supplier) {
        this.iconContextSupplier = supplier;
        updateRectangle();
        return this;
    }

    private void updateRectangle() {
        NanoVG.context.save();
        NanoVG.context.fontSize(size().fontSize());
        float textContextWidth = NanoVG.RENDERER.TEXT.measureTextWidth(textContextSupplier.get(), size().fontSize());
        NanoVG.context.restore();

        float iconContextWidth = 0;
        if (iconContextSupplier.get() != null) {
            iconContextWidth = style.size().iconSize();
        }
        float width =
                style.size().padding() +
                        iconContextWidth +
                        (iconContextWidth == 0 ? 0 : style.size().iconPadding()) +
                        textContextWidth +
                        style.size().padding();
        rectangle.x = position.x;
        rectangle.y = position.y;
        rectangle.width = width;
        rectangle.height = style.size().height();
    }

    @Override
    public Rectangle getRectangle() {
        return rectangle;
    }

    @Override
    public void render(UIDrawContext drawContext, UIInputState inputState) {
        updateRectangle();
        animationSet.update();
        this.ripple.update();

        ButtonColors colors = getButtonColors();
        float cornerSize = style.shape() == MaterialSwitchShape.Round ?
                rectangle.height / 2 :
                style.size().squareCornerSize();
        float deltaValue = style.size().pressedCornerSize() - style.size().squareCornerSize();
        cornerSize += animationSet.press.floatValue() * deltaValue;
        if (colors.backgroundColor != null) {
            drawContext.drawRoundedRect(
                    position.x,
                    position.y,
                    rectangle.width,
                    rectangle.height,
                    cornerSize,
                    colors.backgroundColor,
                    true
            );
        }
        if (colors.coverColor != null) {
            drawContext.drawRoundedRect(
                    position.x,
                    position.y,
                    rectangle.width,
                    rectangle.height,
                    cornerSize,
                    colors.coverColor,
                    true
            );
        }

        if (colors.borderColor != null) {
            drawContext.strokeWidth(1);
            drawContext.drawRoundedRect(
                    position.x,
                    position.y,
                    rectangle.width,
                    rectangle.height,
                    cornerSize,
                    colors.borderColor,
                    false
            );
        }
        if ((ripple.shouldRender() || isPressed()) && lastClickPosition != null) {
            drawContext.beginPath();
            drawContext.paint(ripple.getPaint(
                    style.variant() == MaterialSwitchVariant.Elevated ? scheme.primary() :
                            style.variant() == MaterialSwitchVariant.Filled ? scheme.onPrimary() :
                                    style.variant() == MaterialSwitchVariant.Tonal ? scheme.onSecondaryContainer() :
                                            style.variant() == MaterialSwitchVariant.Text ?
                                                    scheme.primary() : scheme.onSurfaceVariant(),
                    drawContext,
                    lastClickPosition,
                    rectangle.getPosition()
            ));
            drawContext.roundedRect(
                    position.x,
                    position.y,
                    rectangle.width,
                    rectangle.height,
                    cornerSize
            );
            drawContext.endPath();
        }

        float iconContextWidth = 0;
        if (iconContextSupplier.get() != null && colors.iconColor != null) {
            iconContextWidth = style.size().iconSize();
            iconContextSupplier.get().render(
                    drawContext,
                    colors.iconColor,
                    style.size().iconSize(),
                    new Vector2f(
                            position.x + size().padding() + (style.size().iconSize() / 2),
                            rectangle.getCenterY()
                    )
            );
        }
        drawContext.text().drawAlignedText(
                drawContext.font(),
                size().fontSize(),
                textContextSupplier.get(),
                position.x + size().padding() + iconContextWidth + (iconContextWidth == 0 ? 0 : style.size().iconPadding()),
                rectangle.getCenterY(),
                rectangle.width,
                20,
                colors.textColor,
                TextAlign.of(TextAlign.ALIGN_LEFT, TextAlign.ALIGN_MIDDLE),
                false
        );


    }

    private ButtonColors getButtonColors() {
        ButtonColors colors = new ButtonColors();

        switch (style.variant()) {
            case Elevated:
                if (isHovered() || animationSet.hover.isRunning()) {
                    colors.coverColor = scheme.primary().copy().alpha((int) (255 * animationSet.hover.doubleValue()));
                }
                colors.backgroundColor = scheme.surfaceContainerLow();
                colors.textColor = scheme.primary();
                colors.iconColor = scheme.primary();
                break;

            case Filled:
                if (isHovered() || animationSet.hover.isRunning()) {
                    colors.coverColor = scheme.onPrimary().copy().alpha((int) (255 * animationSet.hover.doubleValue()));
                }
                colors.backgroundColor = scheme.primary();
                colors.textColor = scheme.onPrimary();
                colors.iconColor = scheme.onPrimary();
                break;

            case Tonal:
                if (isHovered() || animationSet.hover.isRunning()) {
                    colors.coverColor = scheme.onSecondaryContainer().copy().alpha((int) (255 * animationSet.hover.doubleValue()));
                }
                colors.backgroundColor = scheme.secondaryContainer();
                colors.textColor = scheme.onSecondaryContainer();
                colors.iconColor = scheme.onSecondaryContainer();
                break;

            case Text:
                if (isHovered() || animationSet.hover.isRunning()) {
                    colors.coverColor = scheme.primary().copy().alpha((int) (255 * animationSet.hover.doubleValue()));
                }
                colors.textColor = scheme.primary();
                colors.iconColor = scheme.primary();
                break;

            case Outlined:
                if (isHovered() || animationSet.hover.isRunning()) {
                    colors.coverColor = scheme.onSurfaceVariant().copy().alpha((int) (255 * animationSet.hover.doubleValue()));
                }
                colors.borderColor = scheme.outlineVariant();
                colors.textColor = scheme.onSurfaceVariant();
                colors.iconColor = scheme.onSurfaceVariant();
                break;
            default:
                colors.backgroundColor = scheme.primary();
                colors.textColor = scheme.primary();
                break;
        }

        return colors;
    }

    private void onHover(Vector2f mousePosition, boolean hover) {
        if (hover) {
            animationSet.hover
                    .ease(Easing.cubicBezier(0.2f, 0, 0, 1))
                    .animateTo(0.08, 200);
            MouseCursor.HAND.use();
        } else {
            animationSet.hover
                    .ease(Easing.cubicBezier(0.2f, 0, 0, 1))
                    .animateTo(0, 200);
            MouseCursor.ARROW.use();
        }
        System.out.printf("hover %s %s %s%n", mousePosition.x, mousePosition.y, hover);

    }

    private void onPress(Vector2f mousePosition) {
        animationSet.hover
                .ease(Easing.cubicBezier(0.2f, 0, 0, 1))
                .animateTo(0.1, 200);
        animationSet.press
                .ease(Easing.cubicBezier(0.2f, 0, 0, 1))
                .animateTo(1, 200);
        lastClickPosition = mousePosition.copy();
        //this.ripple.setPressed(true);
        System.out.printf("press %s %s%n", mousePosition.x, mousePosition.y);
    }

    private void onRelease(Vector2f mousePosition) {
        animationSet.hover
                .ease(Easing.cubicBezier(0.2f, 0, 0, 1))
                .animateTo(isHovered() ? 0.08 : 0, 200);
        animationSet.press
                .ease(Easing.cubicBezier(0.2f, 0, 0, 1))
                .animateTo(0, 200);
        //this.ripple.setPressed(false);
        System.out.printf("release %s %s%n", mousePosition.x, mousePosition.y);

    }

    private static class ButtonColors {
        Color coverColor;
        Color backgroundColor;
        Color textColor;
        Color borderColor;
        Color iconColor;
    }
}