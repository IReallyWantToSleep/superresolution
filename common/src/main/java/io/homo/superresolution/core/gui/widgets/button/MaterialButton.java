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

package io.homo.superresolution.core.gui.widgets.button;

import io.homo.superresolution.core.graphics.nanovg.NanoVG;
import io.homo.superresolution.core.graphics.nanovg.renderer.TextAlign;
import io.homo.superresolution.core.gui.MaterialRipple;
import io.homo.superresolution.core.gui.MaterialSymbol;
import io.homo.superresolution.core.gui.core.UIDrawContext;
import io.homo.superresolution.core.gui.core.UIInputState;
import io.homo.superresolution.core.gui.core.animator.Easing;
import io.homo.superresolution.core.gui.core.impl.Rectangle;
import io.homo.superresolution.core.gui.core.layout.ILayoutElement;
import io.homo.superresolution.core.gui.widgets.MaterialWidget;
import io.homo.superresolution.core.math.Vector2f;
import io.homo.superresolution.core.utils.Color;
import io.homo.superresolution.core.utils.MouseCursor;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NVGPaint;

import static org.lwjgl.nanovg.NanoVG.*;

import java.util.function.Supplier;

public class MaterialButton extends MaterialWidget<MaterialButton, MaterialButtonStyle, MaterialButtonAnimationSet> {
    private Supplier<String> textContextSupplier = () -> null;
    private Rectangle rectangle = new Rectangle();
    private MaterialRipple ripple;
    private Vector2f lastClickPosition;
    private Supplier<MaterialSymbol> iconContextSupplier = () -> null;

    public MaterialButton(MaterialButtonSize size) {
        this.style = new MaterialButtonStyle();
        this.ripple = new MaterialRipple();
        this.style.size(size);
        updateRectangle();
    }

    public static MaterialButton create(MaterialButtonSize size) {
        return new MaterialButton(size);
    }

    public static MaterialButton create() {
        return new MaterialButton(MaterialButtonSize.Medium);
    }

    public static MaterialButton create(Vector2f size) {
        float iconSizeRatio = 0.1f;
        float iconPaddingRatio = 0.038f;
        float paddingRatio = 0.1525f;
        float squareCornerSizeRatio = 0.07f;
        float pressedCornerSizeRatio = 0.07f * 0.7f;
        float fontSizeRatio = 0.076f;

        MaterialButtonSize buttonSize = new MaterialButtonSize(
                size.y,
                size.x * paddingRatio,
                size.x * iconPaddingRatio,
                0,
                size.x * squareCornerSizeRatio,
                size.x * pressedCornerSizeRatio,
                size.x * iconSizeRatio,
                size.x * fontSizeRatio
        );
        return new MaterialButton(buttonSize);
    }

    @Override
    protected void init() {
        this.animationSet = new MaterialButtonAnimationSet();
        onMouseHover((widget, mousePosition, hover) -> onHover(mousePosition, hover));
        onMouseRelease((widget, mousePosition) -> onRelease(mousePosition));
        onMousePress((widget, mousePosition) -> onPress(mousePosition));
    }

    public MaterialButtonSize size() {
        return style.size();
    }

    public MaterialButton size(MaterialButtonSize size) {
        this.style.size(size);
        updateRectangle();
        return this;
    }

    public MaterialButton text(String string) {
        this.textContextSupplier = () -> string;
        updateRectangle();
        return this;
    }

    public MaterialButton text(Supplier<String> supplier) {
        this.textContextSupplier = supplier;
        updateRectangle();
        return this;
    }

    public MaterialButton icon(MaterialSymbol icon) {
        this.iconContextSupplier = () -> icon;
        updateRectangle();
        return this;
    }

    public MaterialButton icon(Supplier<MaterialSymbol> supplier) {
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
        rectangle.x = getAbsolutePosition().x;
        rectangle.y = getAbsolutePosition().y;
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
        float cornerSize = style.shape() == MaterialButtonShape.Round ?
                rectangle.height / 2 :
                style.size().squareCornerSize();
        float deltaValue = style.size().pressedCornerSize() - style.size().squareCornerSize();
        cornerSize += animationSet.press.floatValue() * deltaValue;
        if (colors.backgroundColor != null) {
            drawContext.drawRoundedRect(
                    rectangle.x,
                    rectangle.y,
                    rectangle.width,
                    rectangle.height,
                    cornerSize,
                    colors.backgroundColor,
                    true
            );
        }
        if (colors.coverColor != null) {
            drawContext.drawRoundedRect(
                    rectangle.x,
                    rectangle.y,
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
                    rectangle.x,
                    rectangle.y,
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
                    style.variant() == MaterialButtonVariant.Elevated ? scheme.primary() :
                            style.variant() == MaterialButtonVariant.Filled ? scheme.onPrimary() :
                                    style.variant() == MaterialButtonVariant.Tonal ? scheme.onSecondaryContainer() :
                                            style.variant() == MaterialButtonVariant.Text ?
                                                    scheme.primary() : scheme.onSurfaceVariant(),
                    drawContext,
                    lastClickPosition,
                    rectangle.getPosition()
            ));
            drawContext.roundedRect(
                    rectangle.x,
                    rectangle.y,
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
                            rectangle.x + size().padding() + (style.size().iconSize() / 2),
                            rectangle.getCenterY()
                    )
            );
        }
        drawContext.text().drawAlignedText(
                drawContext.font(),
                size().fontSize(),
                textContextSupplier.get(),
                rectangle.x + size().padding() + iconContextWidth + (iconContextWidth == 0 ? 0 : style.size().iconPadding()),
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
        this.ripple.setPressed(
                true,
                lastClickPosition,
                new Vector2f(rectangle.width, rectangle.height)
        );
        System.out.printf("press %s %s%n", mousePosition.x, mousePosition.y);
    }

    private void onRelease(Vector2f mousePosition) {
        animationSet.hover
                .ease(Easing.cubicBezier(0.2f, 0, 0, 1))
                .animateTo(isHovered() ? 0.08 : 0, 200);
        animationSet.press
                .ease(Easing.cubicBezier(0.2f, 0, 0, 1))
                .animateTo(0, 200);
        this.ripple.setPressed(
                false,
                lastClickPosition,
                new Vector2f(rectangle.width, rectangle.height)
        );
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