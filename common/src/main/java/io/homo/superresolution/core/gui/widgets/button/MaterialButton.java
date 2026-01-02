/*
 * Super Resolution
 * Copyright (c) 2025-2026. 187J3X1-114514
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

import io.homo.superresolution.core.gui.MaterialScheme;
import io.homo.superresolution.core.gui.MaterialSymbol;
import io.homo.superresolution.core.gui.MaterialWidgetOverlay;
import io.homo.superresolution.core.gui.core.UIInputState;
import io.homo.superresolution.core.gui.core.backends.interfaces.IPaint;
import io.homo.superresolution.core.gui.core.backends.render.RenderContext;
import io.homo.superresolution.core.gui.core.backends.interfaces.TextAlign;
import io.homo.superresolution.core.gui.core.backends.interfaces.TextAlignType;
import io.homo.superresolution.core.gui.core.backends.nanovg.NanoVG;
import io.homo.superresolution.core.gui.core.event.events.WidgetEvent;
import io.homo.superresolution.core.gui.core.impl.Rectangle;
import io.homo.superresolution.core.gui.widgets.MaterialWidget;
import io.homo.superresolution.core.utils.Color;
import io.homo.superresolution.core.gui.core.animator.Animator;
import io.homo.superresolution.core.gui.core.animator.TimeInterpolator;
import org.joml.Vector2f;

import java.util.function.Supplier;

public class MaterialButton extends MaterialWidget<MaterialButton> {
    private static final long PRESS_ANIMATION_DURATION = 150;

    private Supplier<String> textContextSupplier = () -> null;
    private Supplier<MaterialSymbol> iconContextSupplier = () -> null;
    private Animator.FloatAnimator pressAnimator;
    private boolean pendingRelease = false;

    private MaterialWidgetOverlay<MaterialButton> overlay = new MaterialWidgetOverlay<>(this) {
        @Override
        protected void drawShape(RenderContext ctx, MaterialButton widget, Color color) {
            Rectangle bounds = getBounds();
            ctx.roundedRect(
                    bounds.x,
                    bounds.y,
                    bounds.width,
                    bounds.height,
                    Math.min(getCornerSize(), (style().shape() == MaterialButtonShape.Round ? getBounds().height / 2
                            : style().size().squareCornerSize())),
                    color,
                    true);
        }

        @Override
        protected void drawShape(RenderContext ctx, MaterialButton widget, IPaint paint) {
            Rectangle bounds = getBounds();
            ctx.beginPath();
            ctx.paint(paint);
            ctx.roundedRect(
                    bounds.x,
                    bounds.y,
                    bounds.width,
                    bounds.height,
                    Math.min(getCornerSize(), (style().shape() == MaterialButtonShape.Round ? getBounds().height / 2
                            : style().size().squareCornerSize())));
            ctx.endPath();
        }
    };

    public MaterialButton(MaterialButtonSize size) {
        this.style = new MaterialButtonStyle();
        style().size(size);
        getLayoutNode().setDebugName("MaterialButton");
        updateRectangle();
        float cornerSize = style().shape() == MaterialButtonShape.Round ? getBounds().height / 2
                : style().size().squareCornerSize();
        initAnimators();
        pressAnimator.fromTo(cornerSize, cornerSize).duration(1).start();
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
                size.x * fontSizeRatio);
        return new MaterialButton(buttonSize);
    }

    public static MaterialButton of(String text, MaterialButtonSize size, MaterialButtonVariant variant,
                                    MaterialScheme scheme) {
        MaterialButton button = new MaterialButton(size);
        button.style().variant(variant);
        button.text(text);
        button.scheme(scheme);
        return button;
    }

    public static MaterialButton filled(String text, MaterialScheme scheme) {
        return of(text, MaterialButtonSize.Medium, MaterialButtonVariant.Filled, scheme);
    }

    public static MaterialButton elevated(String text, MaterialScheme scheme) {
        return of(text, MaterialButtonSize.Medium, MaterialButtonVariant.Elevated, scheme);
    }

    public static MaterialButton tonal(String text, MaterialScheme scheme) {
        return of(text, MaterialButtonSize.Medium, MaterialButtonVariant.Tonal, scheme);
    }

    public static MaterialButton outlined(String text, MaterialScheme scheme) {
        return of(text, MaterialButtonSize.Medium, MaterialButtonVariant.Outlined, scheme);
    }

    public static MaterialButton text(String text, MaterialScheme scheme) {
        return of(text, MaterialButtonSize.Medium, MaterialButtonVariant.Text, scheme);
    }

    @Override
    public MaterialButtonStyle style() {
        return (MaterialButtonStyle) style;
    }

    public MaterialButtonSize size() {
        return style().size();
    }

    public MaterialButton size(MaterialButtonSize size) {
        style().size(size);
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

    public MaterialButton variant(MaterialButtonVariant variant) {
        if (this.style instanceof MaterialButtonStyle) {
            ((MaterialButtonStyle) this.style).variant(variant);
        }
        return this;
    }

    public MaterialButton shape(MaterialButtonShape shape) {
        if (this.style instanceof MaterialButtonStyle) {
            ((MaterialButtonStyle) this.style).shape(shape);
        }
        return this;
    }

    public MaterialButton configure(String text, MaterialSymbol icon, MaterialButtonVariant variant,
                                    MaterialButtonShape shape) {
        this.textContextSupplier = () -> text;
        this.iconContextSupplier = () -> icon;
        if (this.style instanceof MaterialButtonStyle buttonStyle) {
            buttonStyle.variant(variant);
            buttonStyle.shape(shape);
        }
        updateRectangle();
        return this;
    }

    @Override
    protected void init() {
        onHover((event) -> onHover(event.getMousePosition(), event.isHovering()));
        onMouseMove((event) -> onMouseMove(event.getMousePosition()));
        onMouseRelease((event) -> onRelease(event.getMousePosition()));
        onMousePress((event) -> onPress(event.getMousePosition()));
    }

    @Override
    public void destroy() {
        if (overlay != null) {
            overlay.destroy();
            overlay = null;
        }
        if (pressAnimator != null) {
            if (pressAnimator.isRunning()) {
                pressAnimator.cancel();
            }
            pressAnimator = null;
        }
    }

    @Override
    protected boolean isInteractive() {
        return true;
    }

    @Override
    public void render(RenderContext ctx, UIInputState inputState) {
        updateRectangle();
        if (pressAnimator != null)
            pressAnimator.update();
        overlay.update();
        Rectangle bounds = getBounds();
        ButtonColors colors = getButtonColors();

        float cornerSize = getCornerSize();
        if (colors.backgroundColor != null) {
            ctx.roundedRect(
                    bounds.x,
                    bounds.y,
                    bounds.width,
                    bounds.height,
                    Math.min(cornerSize, bounds.height / 2),
                    colors.backgroundColor,
                    true);
        }

        overlay.renderHoverOverlay(
                ctx,
                colors.coverColor);

        if (colors.borderColor != null) {
            ctx.strokeWidth(1);
            ctx.roundedRect(
                    bounds.x,
                    bounds.y,
                    bounds.width,
                    bounds.height,
                    cornerSize,
                    colors.borderColor,
                    false);
        }

        overlay.renderRippleOverlay(
                ctx,
                style().variant() == MaterialButtonVariant.Elevated ? scheme.primary()
                        : style().variant() == MaterialButtonVariant.Filled ? scheme.onPrimary()
                        : style().variant() == MaterialButtonVariant.Tonal ? scheme.onSecondaryContainer()
                        : style().variant() == MaterialButtonVariant.Text ? scheme.primary()
                        : scheme.onSurfaceVariant());

        float iconContextWidth = 0;
        if (iconContextSupplier.get() != null && colors.iconColor != null) {
            iconContextWidth = style().size().iconSize();
            iconContextSupplier.get().render(
                    ctx,
                    colors.iconColor,
                    style().size().iconSize(),
                    new Vector2f(
                            bounds.x + size().padding() + (style().size().iconSize() / 2),
                            bounds.getCenterY()));
        }

        ctx.drawAlignedText(
                ctx.font(),
                size().fontSize(),
                textContextSupplier.get(),
                bounds.x + size().padding() + iconContextWidth
                        + (iconContextWidth == 0 ? 0 : style().size().iconPadding()),
                bounds.getCenterY(),
                bounds.width,
                20,
                colors.textColor,
                TextAlign.of(TextAlignType.ALIGN_LEFT, TextAlignType.ALIGN_MIDDLE),
                false);
    }

    private void updateRectangle() {
        NanoVG.context.save();
        NanoVG.context.fontSize(size().fontSize());
        float textContextWidth = NanoVG.RENDERER.TEXT.measureTextWidth(textContextSupplier.get(), size().fontSize());
        NanoVG.context.restore();

        float iconContextWidth = 0;
        if (iconContextSupplier.get() != null) {
            iconContextWidth = style().size().iconSize();
        }
        float width = style().size().padding() +
                iconContextWidth +
                (iconContextWidth == 0 ? 0 : style().size().iconPadding()) +
                textContextWidth +
                style().size().padding();
        setElementSize(width, style().size().height());
    }

    private float getCornerSize() {
        return pressAnimator == null ? (style().shape() == MaterialButtonShape.Round ? getBounds().height / 2
                : style().size().squareCornerSize()) : pressAnimator.get();
    }

    private ButtonColors getButtonColors() {
        ButtonColors colors = new ButtonColors();

        switch (style().variant()) {
            case Elevated:
                colors.coverColor = scheme.primary();
                colors.backgroundColor = isDisabled() ? scheme.onSurface().copy().alpha((int) (255 * 0.1f))
                        : scheme.surfaceContainerLow();
                colors.textColor = isDisabled() ? scheme.onSurface().copy().alpha((int) (255 * 0.38f))
                        : scheme.primary();
                colors.iconColor = isDisabled() ? scheme.onSurface().copy().alpha((int) (255 * 0.38f))
                        : scheme.primary();
                break;

            case Filled:
                colors.coverColor = scheme.onPrimary();
                colors.backgroundColor = isDisabled() ? scheme.onSurface().copy().alpha((int) (255 * 0.1f))
                        : scheme.primary();
                colors.textColor = isDisabled() ? scheme.onSurface().copy().alpha((int) (255 * 0.38f))
                        : scheme.onPrimary();
                colors.iconColor = isDisabled() ? scheme.onSurface().copy().alpha((int) (255 * 0.38f))
                        : scheme.onPrimary();
                break;

            case Tonal:
                colors.coverColor = scheme.onSecondaryContainer();
                colors.backgroundColor = isDisabled() ? scheme.onSurface().copy().alpha((int) (255 * 0.1f))
                        : scheme.secondaryContainer();
                colors.textColor = isDisabled() ? scheme.onSurface().copy().alpha((int) (255 * 0.38f))
                        : scheme.onSecondaryContainer();
                colors.iconColor = isDisabled() ? scheme.onSurface().copy().alpha((int) (255 * 0.38f))
                        : scheme.onSecondaryContainer();
                break;

            case Text:
                colors.coverColor = scheme.primary();
                colors.backgroundColor = isDisabled() ? scheme.onSurface().copy().alpha((int) (255 * 0.1f)) : null;
                colors.textColor = isDisabled() ? scheme.onSurface().copy().alpha((int) (255 * 0.38f))
                        : scheme.primary();
                colors.iconColor = isDisabled() ? scheme.onSurface().copy().alpha((int) (255 * 0.38f))
                        : scheme.primary();
                break;

            case Outlined:
                colors.coverColor = scheme.onSurfaceVariant();
                colors.backgroundColor = isDisabled() ? scheme.onSurface().copy().alpha((int) (255 * 0.1f)) : null;
                colors.borderColor = scheme.outlineVariant();
                colors.textColor = isDisabled() ? scheme.onSurface().copy().alpha((int) (255 * 0.38f))
                        : scheme.onSurfaceVariant();
                colors.iconColor = isDisabled() ? scheme.onSurface().copy().alpha((int) (255 * 0.38f))
                        : scheme.onSurfaceVariant();
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
    }

    private void onPress(Vector2f mousePosition) {
        pendingRelease = false;
        pressAnimator.timeInterpolator(TimeInterpolator.easeInCubic());
        pressAnimator.fromTo(pressAnimator.get(), style().size().pressedCornerSize());
        pressAnimator.duration(PRESS_ANIMATION_DURATION);
        pressAnimator.start();
        eventBus.post(new WidgetEvent.ClickEvent<>(this));
    }

    private void onRelease(Vector2f mousePosition) {
        float cornerSize = style().shape() == MaterialButtonShape.Round ? getBounds().height / 2
                : style().size().squareCornerSize();
        if (pressAnimator.isRunning() && Math.abs(pressAnimator.targetValue() - style().size().pressedCornerSize()) < 0.01f) {
            pendingRelease = true;
        } else {
            pressAnimator.timeInterpolator(TimeInterpolator.easeOutCubic());
            pressAnimator.fromTo(pressAnimator.get(), cornerSize);
            pressAnimator.duration(PRESS_ANIMATION_DURATION);
            pressAnimator.start();
        }
    }

    private void initAnimators() {
        pressAnimator = new Animator.FloatAnimator(
                (style().shape() == MaterialButtonShape.Round ? getBounds().height / 2
                        : style().size().squareCornerSize()),
                (style().shape() == MaterialButtonShape.Round ? getBounds().height / 2
                        : style().size().squareCornerSize()));
        pressAnimator.duration(100);
        pressAnimator.timeInterpolator(TimeInterpolator.easeInOutQuart());
        pressAnimator.onLifecycle(new Animator.AnimatorLifecycleListener() {
            @Override
            public void onEnd() {
                if (pendingRelease) {
                    pendingRelease = false;
                    float cornerSize = style().shape() == MaterialButtonShape.Round ? getBounds().height / 2
                            : style().size().squareCornerSize();
                    pressAnimator.timeInterpolator(TimeInterpolator.easeOutCubic());
                    pressAnimator.fromTo(pressAnimator.get(), cornerSize);
                    pressAnimator.duration(PRESS_ANIMATION_DURATION);
                    pressAnimator.start();
                }
            }
        });
    }

    private void animatePressTo(float targetValue, long duration) {
        if (pressAnimator.isRunning()) {
            pressAnimator.cancel();
        }

        pressAnimator.fromTo(pressAnimator.get(), targetValue);
        pressAnimator.duration(duration);
        pressAnimator.start();
    }

    private static class ButtonColors {
        Color coverColor;
        Color backgroundColor;
        Color textColor;
        Color borderColor;
        Color iconColor;
    }
}