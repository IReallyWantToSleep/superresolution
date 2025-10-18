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

package io.homo.superresolution.core.gui.widgets.sliders;

import io.homo.superresolution.core.gui.core.UIInputState;
import io.homo.superresolution.core.gui.core.animator.Easing;
import io.homo.superresolution.core.gui.core.backends.interfaces.IUIDrawContext;
import io.homo.superresolution.core.gui.core.backends.interfaces.TextAlign;
import io.homo.superresolution.core.gui.core.backends.interfaces.TextAlignType;
import io.homo.superresolution.core.gui.core.event.EventListener;
import io.homo.superresolution.core.gui.core.event.events.WidgetEvent;
import io.homo.superresolution.core.gui.core.impl.Rectangle;
import io.homo.superresolution.core.gui.widgets.MaterialWidget;
import org.joml.Vector2f;
import io.homo.superresolution.core.utils.Color;
import io.homo.superresolution.core.utils.MouseCursor;

import java.util.function.Function;

public class MaterialSlider extends MaterialWidget<MaterialSlider, MaterialSliderStyle, MaterialSliderAnimationSet> {
    private Rectangle rectangle = new Rectangle();

    private Number value = 0.0;
    private Number step = 0.0;
    private Number max = 1.0;
    private Number min = 0.0;

    public Function<Number, String> getValueIndicatorTextFormater() {
        return valueIndicatorTextFormater;
    }

    public void setBounds(float x, float y, float width, float height) {
        rectangle.setLocation(
                x,
                y
        );
    }

    public MaterialSlider setValueIndicatorTextFormater(Function<Number, String> valueIndicatorTextFormater) {
        this.valueIndicatorTextFormater = valueIndicatorTextFormater;
        return this;
    }

    private Function<Number, String> valueIndicatorTextFormater = Object::toString;


    public Number value() {
        return value;
    }

    public MaterialSlider setValue(Number value) {
        this.value = value;
        eventHandler.fire(new WidgetEvent.ChangeEvent<>(value));
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

    public MaterialSlider(MaterialSliderSize size, float width) {
        this.style = new MaterialSliderStyle();
        this.style.size(size);
        rectangle.width = width;
        updateRectangle();
    }

    public float width() {
        return rectangle.width;
    }

    public static MaterialSlider create() {
        return new MaterialSlider(MaterialSliderSize.Medium, 354);
    }

    public void onChange(EventListener<WidgetEvent.ChangeEvent> listener) {
        eventHandler.on(WidgetEvent.ChangeEvent.class, listener);
    }

    @Override
    protected boolean isInteractive() {
        return true;
    }

    @Override
    protected void init() {
        this.animationSet = new MaterialSliderAnimationSet();
        onHover((event) -> onHover(event.getMousePosition(), event.isHovering()));
        onMouseRelease((event) -> onRelease(event.getMousePosition()));
        onMousePress((event) -> onPress(event.getMousePosition()));
        onMouseMove((event) -> onMouseMove(event.getMousePosition()));
        onMouseDrag((event) -> onMouseDrag(event.getMousePosition(), event.getDragDelta()));
    }

    private void updateRectangle() {
        rectangle.x = getAbsolutePosition().x;
        rectangle.y = getAbsolutePosition().y;
        rectangle.height = style.size().handleHeight();
    }

    @Override
    public Rectangle getBounds() {
        return rectangle;
    }

    @Override
    public void render(IUIDrawContext drawContext, UIInputState inputState) {
        if (animationSet.handleSize.floatValue() < style.size().handleWidthPress())
            animationSet.handleSize.set(style.size().handleWidth());
        updateRectangle();
        drawContext.beginBatch();
        animationSet.update();
        SliderColors colors = getSliderColors();

        float progress = (float) ((value.doubleValue() - min.doubleValue()) / (max.doubleValue() - min.doubleValue()));
        //滑块中间X
        float availableWidth = rectangle.width - style.size().stepsHorizontalPadding() * 2;
        float handleXPosition = rectangle.x + availableWidth * progress;
        float handleWidth = animationSet.handleSize.floatValue();
        float activeTrackWidth = (availableWidth * progress) - (handleWidth / 2) - style.size().handleHorizontalPadding() + style.size().stepsSize();
        if (activeTrackWidth > 1) {
            drawContext.beginPath();
            drawContext.fillColor(colors.activeTrackColor);
            drawContext.roundedRectComplex(
                    rectangle.x,
                    rectangle.y + ((style.size().handleHeight() - style.size().trackHeight()) / 2),
                    activeTrackWidth,
                    style.size().trackHeight(),
                    Math.min(style.size().trackCornerSize(), activeTrackWidth / 2),
                    Math.min(2, activeTrackWidth / 2),
                    Math.min(style.size().trackCornerSize(), activeTrackWidth / 2),
                    Math.min(2, activeTrackWidth / 2)
            );
            drawContext.endPath();
        }
        drawContext.drawRoundedRect(
                handleXPosition - (handleWidth / 2) + style.size().stepsSize(),
                rectangle.y,
                handleWidth,
                style.size().handleHeight(),
                Math.min(2, handleWidth / 2),
                colors.handleColor,
                true
        );
        float inactiveTrackWidth = rectangle.width - activeTrackWidth - handleWidth - style.size().handleHorizontalPadding() - style.size().handleHorizontalPadding();
        if (inactiveTrackWidth > 1) {
            drawContext.beginPath();
            drawContext.fillColor(colors.inactiveTrackColor);
            drawContext.roundedRectComplex(
                    rectangle.getLimitX() - inactiveTrackWidth,
                    rectangle.y + ((style.size().handleHeight() - style.size().trackHeight()) / 2),
                    inactiveTrackWidth,
                    style.size().trackHeight(),
                    Math.min(2, inactiveTrackWidth / 2),
                    Math.min(style.size().trackCornerSize(), inactiveTrackWidth / 2),
                    Math.min(2, inactiveTrackWidth / 2),
                    Math.min(style.size().trackCornerSize(), inactiveTrackWidth / 2)
            );
            drawContext.endPath();
        }
        if (step.doubleValue() > 0 && style.steps()) {
            int steps = (int) Math.floor((max.doubleValue() - min.doubleValue()) / step().doubleValue());
            //float availableWidth = rectangle.width - style.size().stepsHorizontalPadding() * 2;
            float stepSpacing = availableWidth / (steps);

            for (int stepIndex = 0; stepIndex <= steps; stepIndex++) {
                float stepXPosition = rectangle.x + style.size().stepsHorizontalPadding() + stepSpacing * stepIndex;
                if (Math.abs(stepXPosition - handleXPosition) < stepSpacing / 2) continue;
                double stepValue = min.doubleValue() + stepIndex * step.doubleValue();
                Color stepColor = value.doubleValue() >= stepValue ?
                        colors.stepActiveIndicatorsColor : colors.stepInactiveIndicatorsColor;
                drawContext.drawArc(
                        stepXPosition,
                        rectangle.y + (style.size().trackHeight() / 2) + ((style.size().handleHeight() - style.size().trackHeight()) / 2),
                        style.size().stepsSize() / 2,
                        stepColor,
                        true
                );
            }
        }
        drawContext.endBatch(getZIndex());

        drawContext.beginBatch();
        if (style.valueIndicator() && animationSet.hover.doubleValue() > 0.01) {
            String valueIndicatorString = valueIndicatorTextFormater == null ? String.valueOf(value()) : valueIndicatorTextFormater.apply(value());
            Vector2f valueIndicatorStringRegion = drawContext.measureText(valueIndicatorString, 14f);
            Rectangle valueIndicatorRegion = new Rectangle();
            valueIndicatorRegion.width = style.size().valueIndicatorTextHorizontalPadding() * 2 + valueIndicatorStringRegion.x;
            valueIndicatorRegion.height = style.size().valueIndicatorTextVerticalPadding() * 2 + 20;
            valueIndicatorRegion.x = handleXPosition - valueIndicatorRegion.width / 2;
            valueIndicatorRegion.y = rectangle.y + ((style.size().handleHeight() - style.size().trackHeight()) / 2) - ((style.size().handleHeight() - style.size().trackHeight()) / 2) - style.size().valueIndicatorBottomPadding() - rectangle.height;
            drawContext.transform().push();
            drawContext.transform().last().scaleAt(
                    animationSet.hover.floatValue(),
                    animationSet.hover.floatValue(),
                    valueIndicatorRegion.getCenterX(),
                    valueIndicatorRegion.getLimitY()
            );
            drawContext.transform().translate(0, 0);
            drawContext.drawRoundedRect(
                    valueIndicatorRegion.x,
                    valueIndicatorRegion.y,
                    valueIndicatorRegion.width,
                    valueIndicatorRegion.height,
                    valueIndicatorRegion.height / 2,
                    colors.valueIndicatorColor,
                    true
            );
            drawContext.drawAlignedText(
                    drawContext.font(),
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
            drawContext.transform().pop();
        }
        drawContext.endBatch(1000);
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
                    .ease(Easing.LINEAR)
                    .animateTo(1, 150);
            MouseCursor.HAND.use();
        } else {
            if (!isPressed()) animationSet.hover
                    .ease(Easing.LINEAR)
                    .animateTo(0, 150);
            MouseCursor.ARROW.use();
        }

    }

    private float calcProgressFromMouse(Vector2f position) {
        float relativeX = position.x - rectangle.x;
        float clampedX = clamp(relativeX, 0, rectangle.width);
        return clampedX / rectangle.width;
    }

    private float clamp(float value, float min, float max) {
        return Math.min(max, Math.max(value, min));
    }

    private double clamp(double value, double min, double max) {
        return Math.min(max, Math.max(value, min));
    }

    private void onPress(Vector2f mousePosition) {
        animationSet.hover
                .ease(Easing.LINEAR)
                .animateTo(1, 150);
        animationSet.press
                .ease(Easing.LINEAR)
                .animateTo(1, 200);
        animationSet.handleSize
                .ease(Easing.LINEAR)
                .animateTo(style.size().handleWidthPress(), 200);
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

    private void onMouseDrag(Vector2f mousePosition, Vector2f mousePositionDelta) {
        if (isPressed()) {
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
    }

    private void onMouseMove(Vector2f mousePosition) {
        if (isPressed()) {
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
    }

    private void onRelease(Vector2f mousePosition) {
        animationSet.hover
                .ease(Easing.cubicBezier(0.2f, 0, 0, 1))
                .animateTo(isHovered() ? 1 : 0, 200);
        animationSet.press
                .ease(Easing.LINEAR)
                .animateTo(0, 200);
        animationSet.handleSize
                .ease(Easing.LINEAR)
                .animateTo(style.size().handleWidth(), 200);
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