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

import io.homo.superresolution.core.gui.MaterialRipple;
import io.homo.superresolution.core.gui.MaterialSymbol;
import io.homo.superresolution.core.gui.MaterialWidgetOverlay;
import io.homo.superresolution.core.gui.core.UIInputState;
import io.homo.superresolution.core.gui.core.backends.interfaces.IPaint;
import io.homo.superresolution.core.gui.core.backends.interfaces.IUIDrawContext;
import io.homo.superresolution.core.gui.core.backends.interfaces.TextAlign;
import io.homo.superresolution.core.gui.core.backends.interfaces.TextAlignType;
import io.homo.superresolution.core.gui.core.backends.nanovg.NanoVG;
import io.homo.superresolution.core.gui.core.event.events.WidgetEvent;
import io.homo.superresolution.core.gui.core.impl.Rectangle;
import io.homo.superresolution.core.gui.widgets.MaterialWidget;
import io.homo.superresolution.core.utils.Color;
import io.homo.superresolution.thirdparty.icyllis.modernui.animation.BezierInterpolator;
import io.homo.superresolution.thirdparty.icyllis.modernui.animation.PropertyValuesHolder;
import io.homo.superresolution.thirdparty.icyllis.modernui.animation.TimeInterpolator;
import io.homo.superresolution.thirdparty.icyllis.modernui.animation.ValueAnimator;
import org.joml.Vector2f;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class MaterialButton extends MaterialWidget<MaterialButton, MaterialButtonStyle, MaterialButtonAnimationSet> {
    private Supplier<String> textContextSupplier = () -> null;
    //private MaterialRipple ripple;
    private MaterialWidgetOverlay<MaterialButton> overlay = new MaterialWidgetOverlay<>(this) {
        @Override
        protected void drawShape(
                IUIDrawContext drawContext,
                MaterialButton widget,
                Color color
        ) {
            Rectangle bounds = getBounds();
            drawContext.drawRoundedRect(
                    bounds.x,
                    bounds.y,
                    bounds.width,
                    bounds.height,
                    getCornerSize(),
                    color,
                    true
            );
        }

        @Override
        protected void drawShape(
                IUIDrawContext drawContext,
                MaterialButton widget,
                IPaint paint
        ) {
            Rectangle bounds = getBounds();
            drawContext.beginPath();
            drawContext.paint(paint);
            drawContext.roundedRect(
                    bounds.x,
                    bounds.y,
                    bounds.width,
                    bounds.height,
                    getCornerSize()
            );
            drawContext.endPath();
        }
    };
    private Vector2f lastClickPosition;
    private Supplier<MaterialSymbol> iconContextSupplier = () -> null;
    private ValueAnimator pressAnimator;
    private ValueAnimator.AnimatorUpdateListener pressUpdateListener;

    public MaterialButton(MaterialButtonSize size) {
        this.style = new MaterialButtonStyle();
        this.style.size(size);
        getLayoutNode().setDebugName("MaterialButton");
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

    public void onClick(Consumer<WidgetEvent.ClickEvent> listener) {
        eventBus.addListener(WidgetEvent.ClickEvent.class, listener);
    }

    @Override
    protected void init() {
        initAnimators();

        onHover((event) -> onHover(event.getMousePosition(), event.isHovering()));
        onMouseMove((event) -> onMouseMove(event.getMousePosition()));
        onMouseRelease((event) -> onRelease(event.getMousePosition()));
        onMousePress((event) -> onPress(event.getMousePosition()));
    }

    private void initAnimators() {
        pressAnimator = ValueAnimator.ofFloat(0f, 0f);
        pressAnimator.setDuration(200);
        pressAnimator.setInterpolator(TimeInterpolator.LINEAR);
    }

    private void animatePressTo(float targetValue, long duration) {
        if (pressAnimator.isRunning()) {
            pressAnimator.cancel();
        }

        pressAnimator.setValues(PropertyValuesHolder.ofFloat((Float) pressAnimator.getAnimatedValue(), targetValue));
        pressAnimator.setDuration(duration);
        pressAnimator.start();
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
        setElementSize(width, style.size().height());
    }

    private float getCornerSize() {
        float cornerSize = style.shape() == MaterialButtonShape.Round ?
                getBounds().height / 2 :
                style.size().squareCornerSize();
        float deltaValue = style.size().pressedCornerSize() - style.size().squareCornerSize();
        cornerSize += ((float) pressAnimator.getAnimatedValue()) * deltaValue;

        return cornerSize;
    }

    @Override
    public void render(IUIDrawContext drawContext, UIInputState inputState) {
        drawContext.beginBatch();
        updateRectangle();
        overlay.update();
        Rectangle bounds = getBounds();
        ButtonColors colors = getButtonColors();

        float cornerSize = getCornerSize();
        if (colors.backgroundColor != null) {
            drawContext.drawRoundedRect(
                    bounds.x,
                    bounds.y,
                    bounds.width,
                    bounds.height,
                    cornerSize,
                    colors.backgroundColor,
                    true
            );
        }

        overlay.renderHoverOverlay(
                drawContext,
                colors.coverColor
        );

        if (colors.borderColor != null) {
            drawContext.strokeWidth(1);
            drawContext.drawRoundedRect(
                    bounds.x,
                    bounds.y,
                    bounds.width,
                    bounds.height,
                    cornerSize,
                    colors.borderColor,
                    false
            );
        }

        overlay.renderRippleOverlay(
                drawContext,
                style.variant() == MaterialButtonVariant.Elevated ? scheme.primary() :
                        style.variant() == MaterialButtonVariant.Filled ? scheme.onPrimary() :
                                style.variant() == MaterialButtonVariant.Tonal ? scheme.onSecondaryContainer() :
                                        style.variant() == MaterialButtonVariant.Text ? scheme.primary() :
                                                scheme.onSurfaceVariant()
        );

        float iconContextWidth = 0;
        if (iconContextSupplier.get() != null && colors.iconColor != null) {
            iconContextWidth = style.size().iconSize();
            iconContextSupplier.get().render(
                    drawContext,
                    colors.iconColor,
                    style.size().iconSize(),
                    new Vector2f(
                            bounds.x + size().padding() + (style.size().iconSize() / 2),
                            bounds.getCenterY()
                    )
            );
        }

        drawContext.drawAlignedText(
                drawContext.font(),
                size().fontSize(),
                textContextSupplier.get(),
                bounds.x + size().padding() + iconContextWidth + (iconContextWidth == 0 ? 0 : style.size().iconPadding()),
                bounds.getCenterY(),
                bounds.width,
                20,
                colors.textColor,
                TextAlign.of(TextAlignType.ALIGN_LEFT, TextAlignType.ALIGN_MIDDLE),
                false
        );

        drawContext.endBatch(getZIndex());
    }

    private ButtonColors getButtonColors() {
        ButtonColors colors = new ButtonColors();

        switch (style.variant()) {
            case Elevated:
                colors.coverColor = scheme.primary();
                colors.backgroundColor = isDisabled() ? scheme.onSurface().copy().alpha((int) (255 * 0.1f)) : scheme.surfaceContainerLow();
                colors.textColor = isDisabled() ? scheme.onSurface().copy().alpha((int) (255 * 0.38f)) : scheme.primary();
                colors.iconColor = isDisabled() ? scheme.onSurface().copy().alpha((int) (255 * 0.38f)) : scheme.primary();
                break;

            case Filled:
                colors.coverColor = scheme.onPrimary();
                colors.backgroundColor = isDisabled() ? scheme.onSurface().copy().alpha((int) (255 * 0.1f)) : scheme.primary();
                colors.textColor = isDisabled() ? scheme.onSurface().copy().alpha((int) (255 * 0.38f)) : scheme.onPrimary();
                colors.iconColor = isDisabled() ? scheme.onSurface().copy().alpha((int) (255 * 0.38f)) : scheme.onPrimary();
                break;

            case Tonal:
                colors.coverColor = scheme.onSecondaryContainer();
                colors.backgroundColor = isDisabled() ? scheme.onSurface().copy().alpha((int) (255 * 0.1f)) : scheme.secondaryContainer();
                colors.textColor = isDisabled() ? scheme.onSurface().copy().alpha((int) (255 * 0.38f)) : scheme.onSecondaryContainer();
                colors.iconColor = isDisabled() ? scheme.onSurface().copy().alpha((int) (255 * 0.38f)) : scheme.onSecondaryContainer();
                break;

            case Text:
                colors.coverColor = scheme.primary();
                colors.backgroundColor = isDisabled() ? scheme.onSurface().copy().alpha((int) (255 * 0.1f)) : null;
                colors.textColor = isDisabled() ? scheme.onSurface().copy().alpha((int) (255 * 0.38f)) : scheme.primary();
                colors.iconColor = isDisabled() ? scheme.onSurface().copy().alpha((int) (255 * 0.38f)) : scheme.primary();
                break;

            case Outlined:
                colors.coverColor = scheme.onSurfaceVariant();
                colors.backgroundColor = isDisabled() ? scheme.onSurface().copy().alpha((int) (255 * 0.1f)) : null;
                colors.borderColor = scheme.outlineVariant();
                colors.textColor = isDisabled() ? scheme.onSurface().copy().alpha((int) (255 * 0.38f)) : scheme.onSurfaceVariant();
                colors.iconColor = isDisabled() ? scheme.onSurface().copy().alpha((int) (255 * 0.38f)) : scheme.onSurfaceVariant();
                break;
            default:
                colors.backgroundColor = scheme.primary();
                colors.textColor = scheme.primary();
                break;
        }

        return colors;
    }

    private void onMouseMove(Vector2f mousePosition) {

    }

    private void onHover(Vector2f mousePosition, boolean hover) {
        /*
        if (hover) {
            animateHoverTo(1.0f, 200);
            MouseCursor.HAND.use();
        } else {
            animateHoverTo(0f, 200);
            MouseCursor.ARROW.use();
        }
        */
    }

    @Override
    protected boolean isInteractive() {
        return true;
    }

    private void onPress(Vector2f mousePosition) {
        animatePressTo(1.0f, 200);
        lastClickPosition = new Vector2f(mousePosition);
        eventBus.post(new WidgetEvent.ClickEvent<>(this));
    }

    private void onRelease(Vector2f mousePosition) {
        animatePressTo(0f, 200);
    }

    @Override
    public void destroy() {
        overlay.destroy();
        if (pressAnimator != null) {
            if (pressUpdateListener != null) {
                pressAnimator.removeUpdateListener(pressUpdateListener);
                pressUpdateListener = null;
            }
            if (pressAnimator.isRunning()) {
                pressAnimator.cancel();
            }
            pressAnimator = null;
        }
    }

    private static class ButtonColors {
        Color coverColor;
        Color backgroundColor;
        Color textColor;
        Color borderColor;
        Color iconColor;
    }
}