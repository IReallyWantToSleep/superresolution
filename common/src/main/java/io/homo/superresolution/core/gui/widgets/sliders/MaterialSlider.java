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

package io.homo.superresolution.core.gui.widgets.sliders;

import io.homo.superresolution.core.gui.core.UIInputState;
import io.homo.superresolution.core.gui.core.animator.BezierInterpolator;
import io.homo.superresolution.core.gui.core.animator.TimeInterpolator;
import io.homo.superresolution.core.gui.core.backends.render.RenderContext;
import io.homo.superresolution.core.gui.core.backends.interfaces.TextAlign;
import io.homo.superresolution.core.gui.core.backends.interfaces.TextAlignType;
import io.homo.superresolution.core.gui.core.event.events.WidgetEvent;
import io.homo.superresolution.core.gui.core.impl.Rectangle;
import io.homo.superresolution.core.gui.widgets.MaterialWidget;
import io.homo.superresolution.core.utils.Color;
import io.homo.superresolution.core.utils.MouseCursor;
import org.joml.Vector2f;

import java.util.function.Function;

public class MaterialSlider extends MaterialWidget<MaterialSlider> {
    private Number value = 0.0;
    private Number step = 0.0;
    private Number max = 1.0;
    private Number min = 0.0;
    private Function<Number, String> valueIndicatorTextFormater = Object::toString;
    protected MaterialSliderAnimationSet animationSet;

    public MaterialSlider(MaterialSliderSize size, float width) {
        this.style = new MaterialSliderStyle();
        style().size(size);
        setElementWidth(width);
        updateRectangle();
        getLayoutNode().setDebugName("MaterialSlider");

    }

    public static MaterialSlider create() {
        return new MaterialSlider(MaterialSliderSize.Medium, 354);
    }

    public static MaterialSlider create(float width) {
        return new MaterialSlider(MaterialSliderSize.Medium, width);
    }

    public static MaterialSlider create(MaterialSliderSize size, float width) {
        return new MaterialSlider(size, width);
    }

    public static MaterialSlider of(Number min, Number max, Number value, Number step, float width) {
        MaterialSlider slider = new MaterialSlider(MaterialSliderSize.Medium, width);
        slider.min = min;
        slider.max = max;
        slider.value = value;
        slider.step = step;
        return slider;
    }

    @Override
    public MaterialSliderStyle style() {
        return (MaterialSliderStyle) style;
    }

    public Function<Number, String> getValueIndicatorTextFormater() {
        return valueIndicatorTextFormater;
    }

    public MaterialSlider setValueIndicatorTextFormater(Function<Number, String> valueIndicatorTextFormater) {
        this.valueIndicatorTextFormater = valueIndicatorTextFormater;
        return this;
    }

    public MaterialSlider usePercentageFormatter() {
        this.valueIndicatorTextFormater = v -> String.format("%.0f%%", v.doubleValue() * 100);
        return this;
    }

    public MaterialSlider useDecimalFormatter(int decimalPlaces) {
        this.valueIndicatorTextFormater = v -> String.format("%." + decimalPlaces + "f", v.doubleValue());
        return this;
    }

    public MaterialSlider useIntegerFormatter() {
        this.valueIndicatorTextFormater = v -> String.valueOf(v.intValue());
        return this;
    }

    public Number value() {
        return value;
    }

    public MaterialSlider setValue(Number value) {
        Number oldValue = this.value;
        this.value = value;
        eventBus.post(new WidgetEvent.ChangeEvent<>(oldValue, value));
        return this;
    }

    public Number step() {
        return step;
    }

    public MaterialSlider setStep(Number step) {
        this.step = step;
        return this;
    }

    public Number max() {
        return max;
    }

    public MaterialSlider setMax(Number max) {
        this.max = max;
        return this;
    }

    public Number min() {
        return min;
    }

    public MaterialSlider setMin(Number min) {
        this.min = min;
        return this;
    }

    public MaterialSlider range(Number min, Number max) {
        this.min = min;
        this.max = max;
        return this;
    }

    public float width() {
        return getBounds().width;
    }

    @Override
    protected boolean isInteractive() {
        return true;
    }


    @Override
    protected void init() {
        this.animationSet = new MaterialSliderAnimationSet();
        this.animationSet.init();
        onHover((event) -> onHover(event.getMousePosition(), event.isHovering()));
        onMouseRelease((event) -> onRelease(event.getMousePosition()));
        onMousePress((event) -> onPress(event.getMousePosition()));
        onMouseMove((event) -> onMouseMove(event.getMousePosition()));
        onMouseDrag((event) -> onMouseDrag(event.getMousePosition(), event.getDragDelta()));
    }

    private void updateRectangle() {
        setElementHeight(style().size().handleHeight());
    }

    @Override
    public void render(RenderContext ctx, UIInputState inputState) {
        if (animationSet.handleSize.get() < style().size().handleWidthPress()) {
            animationSet.handleSize.set(style().size().handleWidth());
        }
        animationSet.update();

        updateRectangle();

        SliderColors colors = getSliderColors();
        Rectangle bounds = getBounds();

        float progress = (float) ((value.doubleValue() - min.doubleValue()) / (max.doubleValue() - min.doubleValue()));
        float availableWidth = bounds.width - style().size().stepsHorizontalPadding() * 2;
        float handleXPosition = bounds.x + availableWidth * progress;
        float handleWidth = animationSet.handleSize.get();
        ctx.save();
        float activeTrackWidth = drawTrack(ctx, bounds, colors, progress, handleWidth, availableWidth);
        drawHandle(ctx, bounds, colors, handleXPosition, handleWidth);
        drawInactiveTrack(ctx, bounds, colors, activeTrackWidth, handleWidth);

        if (step.doubleValue() > 0 && style().steps()) {
            drawSteps(ctx, bounds, colors, handleXPosition, availableWidth);
        }
        ctx.restore();
        if (style().valueIndicator() && animationSet.hover.get() > 0.01) {
            drawValueIndicator(ctx, bounds, colors, handleXPosition);
        }
    }

    private float drawTrack(RenderContext ctx, Rectangle bounds, SliderColors colors, float progress, float handleWidth, float availableWidth) {
        float activeTrackWidth = (availableWidth * progress) - (handleWidth / 2) - style().size().handleHorizontalPadding() + style().size().stepsSize();
        if (activeTrackWidth > 1) {
            ctx.beginPath();
            ctx.fillColor(colors.activeTrackColor);
            ctx.roundedRectComplex(
                    bounds.x,
                    bounds.y + ((style().size().handleHeight() - style().size().trackHeight()) / 2),
                    activeTrackWidth,
                    style().size().trackHeight(),
                    Math.min(style().size().trackCornerSize(), activeTrackWidth / 2),
                    Math.min(2, activeTrackWidth / 2),
                    Math.min(style().size().trackCornerSize(), activeTrackWidth / 2),
                    Math.min(2, activeTrackWidth / 2)
            );
            ctx.endPath();
        }
        return activeTrackWidth;
    }

    private void drawHandle(RenderContext ctx, Rectangle bounds, SliderColors colors, float handleXPosition, float handleWidth) {
        ctx.roundedRect(
                handleXPosition - (handleWidth / 2) + style().size().stepsSize(),
                bounds.y,
                handleWidth,
                style().size().handleHeight(),
                Math.min(2, handleWidth / 2),
                colors.handleColor,
                true
        );
    }

    private void drawInactiveTrack(RenderContext ctx, Rectangle bounds, SliderColors colors, float activeTrackWidth, float handleWidth) {
        float inactiveTrackWidth = bounds.width - activeTrackWidth - handleWidth - style().size().handleHorizontalPadding() - style().size().handleHorizontalPadding();
        if (inactiveTrackWidth > 1) {
            ctx.beginPath();
            ctx.fillColor(colors.inactiveTrackColor);
            ctx.roundedRectComplex(
                    bounds.getLimitX() - inactiveTrackWidth,
                    bounds.y + ((style().size().handleHeight() - style().size().trackHeight()) / 2),
                    inactiveTrackWidth,
                    style().size().trackHeight(),
                    Math.min(2, inactiveTrackWidth / 2),
                    Math.min(style().size().trackCornerSize(), inactiveTrackWidth / 2),
                    Math.min(2, inactiveTrackWidth / 2),
                    Math.min(style().size().trackCornerSize(), inactiveTrackWidth / 2)
            );
            ctx.endPath();
        }
    }

    private void drawSteps(RenderContext ctx, Rectangle bounds, SliderColors colors, float handleXPosition, float availableWidth) {
        int steps = (int) Math.floor((max.doubleValue() - min.doubleValue()) / step().doubleValue());
        float stepSpacing = availableWidth / (steps);

        for (int stepIndex = 0; stepIndex <= steps; stepIndex++) {
            float stepXPosition = bounds.x + style().size().stepsHorizontalPadding() + stepSpacing * stepIndex;
            if (Math.abs(stepXPosition - handleXPosition) < stepSpacing / 2) continue;
            double stepValue = min.doubleValue() + stepIndex * step.doubleValue();
            Color stepColor = value.doubleValue() >= stepValue ?
                    colors.stepActiveIndicatorsColor : colors.stepInactiveIndicatorsColor;
            ctx.arc(
                    stepXPosition,
                    bounds.y + (style().size().trackHeight() / 2) + ((style().size().handleHeight() - style().size().trackHeight()) / 2),
                    style().size().stepsSize() / 2,
                    stepColor,
                    true
            );
        }
    }

    private void drawValueIndicator(RenderContext ctx, Rectangle bounds, SliderColors colors, float handleXPosition) {
        String valueIndicatorString = valueIndicatorTextFormater == null ? String.valueOf(value()) : valueIndicatorTextFormater.apply(value());
        Vector2f valueIndicatorStringRegion = ctx.measureText(valueIndicatorString, 14f);
        Rectangle valueIndicatorRegion = new Rectangle();
        valueIndicatorRegion.width = style().size().valueIndicatorTextHorizontalPadding() * 2 + valueIndicatorStringRegion.x;
        valueIndicatorRegion.height = style().size().valueIndicatorTextVerticalPadding() * 2 + 20;
        valueIndicatorRegion.x = handleXPosition - valueIndicatorRegion.width / 2;
        valueIndicatorRegion.y = bounds.y + ((style().size().handleHeight() - style().size().trackHeight()) / 2) - ((style().size().handleHeight() - style().size().trackHeight()) / 2) - style().size().valueIndicatorBottomPadding() - bounds.height;
        ctx.save();
        float pivotX = valueIndicatorRegion.getCenterX();
        float pivotY = valueIndicatorRegion.getLimitY();
        float scaleValue = animationSet.hover.get();
        ctx.translate(pivotX, pivotY);
        ctx.scale(scaleValue, scaleValue);
        ctx.translate(-pivotX, -pivotY);
        ctx.roundedRect(
                valueIndicatorRegion.x,
                valueIndicatorRegion.y,
                valueIndicatorRegion.width,
                valueIndicatorRegion.height,
                valueIndicatorRegion.height / 2,
                colors.valueIndicatorColor,
                true
        );
        ctx.drawAlignedText(
                ctx.font(),
                14,
                valueIndicatorString,
                valueIndicatorRegion.getCenterX(),
                valueIndicatorRegion.getCenterY(),
                valueIndicatorRegion.width,
                14,
                colors.valueIndicatorTextColor,
                TextAlign.of(TextAlignType.ALIGN_CENTER, TextAlignType.ALIGN_MIDDLE),
                false
        );
        ctx.restore();
    }

    private SliderColors getSliderColors() {
        SliderColors colors = new SliderColors();
        colors.inactiveTrackColor = isDisabled() ? scheme.onSurface().copy().alpha((int) (255 * 0.1)) : scheme.secondaryContainer();
        colors.activeTrackColor = isDisabled() ? scheme.onSurface() : scheme.primary();
        colors.handleColor = isDisabled() ? scheme.onSurface().copy().alpha((int) (255 * 0.38)) : scheme.primary();
        colors.stepInactiveIndicatorsColor = isDisabled() ? scheme.onSurface() : scheme.onSecondaryContainer();
        colors.stepActiveIndicatorsColor = isDisabled() ? scheme.inverseOnSurface() : scheme.onPrimary();
        colors.insetIconColor = isDisabled() ? scheme.inverseOnSurface() : scheme.onPrimary();
        colors.valueIndicatorColor = scheme.inverseSurface();
        colors.valueIndicatorTextColor = scheme.inverseOnSurface();
        return colors;
    }

    private void onHover(Vector2f mousePosition, boolean hover) {
        if (hover) {
            animationSet.hover
                    .timeInterpolator(TimeInterpolator.easeOutQuart())
                    .duration(150)
                    .to(1f)
                    .start();
            MouseCursor.HAND.use();
        } else {
            if (!isPressed()) animationSet.hover
                    .timeInterpolator(TimeInterpolator.easeOutQuart())
                    .duration(150)
                    .to(0f)
                    .start();
            MouseCursor.ARROW.use();
        }

    }

    private float calcProgressFromMouse(Vector2f position) {
        Rectangle bounds = getBounds();
        float relativeX = position.x - bounds.x;
        float clampedX = clamp(relativeX, 0, bounds.width);
        return clampedX / bounds.width;
    }

    private float clamp(float value, float min, float max) {
        return Math.min(max, Math.max(value, min));
    }

    private double clamp(double value, double min, double max) {
        return Math.min(max, Math.max(value, min));
    }

    private void updateValueFromMouse(Vector2f mousePosition) {
        float progress = calcProgressFromMouse(mousePosition);
        double range = max.doubleValue() - min.doubleValue();
        double newValue = min.doubleValue() + (range * progress);

        if (step.doubleValue() > 0) {
            double steps = (newValue - min.doubleValue()) / step.doubleValue();
            newValue = min.doubleValue() + (Math.round(steps) * step.doubleValue());
        }

        newValue = clamp(newValue, min.doubleValue(), max.doubleValue());
        setValue(newValue);
    }

    private void onPress(Vector2f mousePosition) {
        animationSet.hover
                .timeInterpolator(TimeInterpolator.linear())
                .duration(150)
                .to(1f)
                .start();
        animationSet.press
                .timeInterpolator(TimeInterpolator.linear())
                .duration(200)
                .to(1f)
                .start();
        animationSet.handleSize
                .timeInterpolator(TimeInterpolator.linear())
                .duration(100)
                .to(style().size().handleWidthPress())
                .start();
        updateValueFromMouse(mousePosition);
    }

    private void onMouseDrag(Vector2f mousePosition, Vector2f mousePositionDelta) {
        if (isPressed()) {
            updateValueFromMouse(mousePosition);
        }
    }

    private void onMouseMove(Vector2f mousePosition) {
        if (isPressed()) {
            updateValueFromMouse(mousePosition);
        }
    }

    private void onRelease(Vector2f mousePosition) {
        animationSet.hover
                .timeInterpolator(BezierInterpolator.of(0.2f, 0, 0, 1))
                .duration(200)
                .to(isHovered() ? 1f : 0f)
                .start();
        animationSet.press
                .timeInterpolator(TimeInterpolator.linear())
                .duration(200)
                .to(0f)
                .start();
        animationSet.handleSize
                .timeInterpolator(TimeInterpolator.linear())
                .duration(100)
                .to(style().size().handleWidth())
                .start();
    }

    private static class SliderColors {
        Color insetIconColor;
        Color stepActiveIndicatorsColor;
        Color stepInactiveIndicatorsColor;
        Color valueIndicatorColor;
        Color valueIndicatorTextColor;
        Color activeTrackColor;
        Color inactiveTrackColor;
        Color handleColor;
    }
}