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

import io.homo.superresolution.core.gui.MaterialTheme;
import io.homo.superresolution.core.gui.core.backends.render.RenderContext;
import io.homo.superresolution.core.gui.core.impl.Rectangle;
import io.homo.superresolution.core.gui.widgets.glass.LiquidGlassStyle;
import io.homo.superresolution.core.utils.Color;

public class LiquidGlassMaterialSlider extends MaterialSlider {
    public LiquidGlassMaterialSlider(MaterialSliderSize size, float width) {
        super(size, width);
        // Follow Yoga's resolved width when the option row needs to shrink.
        setElementWidth(-1);
        layout().setWidth(width);
    }

    public static LiquidGlassMaterialSlider create(MaterialSliderSize size, float width) {
        return new LiquidGlassMaterialSlider(size, width);
    }

    @Override
    protected Color getActiveTrackColor() {
        return isDisabled() ? super.getActiveTrackColor()
                : scheme().primary().copy().alpha(Math.round(255 * 0.6f));
    }

    @Override
    protected Color getInactiveTrackColor() {
        return isDisabled() ? super.getInactiveTrackColor()
                : scheme().secondaryContainer().copy().alpha(Math.round(255 * 0.6f));
    }

    @Override
    protected Color getHandleColor() {
        return isDisabled() ? super.getHandleColor()
                : scheme().primary().copy().alpha(Math.round(255 * (scheme().theme() == MaterialTheme.Dark ? 0.8f : 1f)));
    }

    @Override
    protected float drawTrack(RenderContext ctx, Rectangle bounds, SliderColors colors, float progress, float handleWidth, float availableWidth) {
        if (isDisabled()) {
            return super.drawTrack(ctx, bounds, colors, progress, handleWidth, availableWidth);
        }
        float activeTrackWidth = (availableWidth * progress) - (handleWidth / 2)
                - style().size().handleHorizontalPadding() + style().size().stepsSize();
        if (activeTrackWidth > 1) {
            float outerRadius = Math.min(style().size().trackCornerSize(), activeTrackWidth / 2);
            float innerRadius = Math.min(2, activeTrackWidth / 2);
            LiquidGlassStyle.draw(ctx, bounds.x, trackY(bounds), activeTrackWidth, style().size().trackHeight(),
                    outerRadius, innerRadius, outerRadius, innerRadius,
                    LiquidGlassStyle.CONTROL, colors.activeTrackColor);
        }
        return activeTrackWidth;
    }

    @Override
    protected void drawInactiveTrack(RenderContext ctx, Rectangle bounds, SliderColors colors, float activeTrackWidth, float handleWidth) {
        if (isDisabled()) {
            super.drawInactiveTrack(ctx, bounds, colors, activeTrackWidth, handleWidth);
            return;
        }
        float inactiveTrackWidth = bounds.width - activeTrackWidth - handleWidth
                - style().size().handleHorizontalPadding() * 2;
        if (inactiveTrackWidth > 1) {
            float outerRadius = Math.min(style().size().trackCornerSize(), inactiveTrackWidth / 2);
            float innerRadius = Math.min(2, inactiveTrackWidth / 2);
            LiquidGlassStyle.draw(ctx, bounds.getLimitX() - inactiveTrackWidth, trackY(bounds),
                    inactiveTrackWidth, style().size().trackHeight(),
                    innerRadius, outerRadius, innerRadius, outerRadius,
                    LiquidGlassStyle.CONTROL, colors.inactiveTrackColor);
        }
    }

    @Override
    protected void drawHandle(RenderContext ctx, Rectangle bounds, SliderColors colors, float handleXPosition, float handleWidth) {
        if (isDisabled()) {
            super.drawHandle(ctx, bounds, colors, handleXPosition, handleWidth);
            return;
        }
        LiquidGlassStyle.draw(ctx, handleXPosition - handleWidth / 2 + style().size().stepsSize(), bounds.y,
                handleWidth, style().size().handleHeight(), Math.min(2, handleWidth / 2),
                LiquidGlassStyle.CONTROL, colors.handleColor);
    }

    @Override
    protected void drawStopIndicator(RenderContext ctx, Rectangle bounds, SliderColors colors, float handleXPosition, float handleWidth) {
        if (isDisabled()) {
            return;
        }
        float stopX = bounds.getLimitX() - style().size().stepsHorizontalPadding();
        float handleCenterX = handleXPosition + style().size().stepsSize();
        if (stopX - handleCenterX > handleWidth / 2 + style().size().handleHorizontalPadding() + 2) {
            ctx.arc(stopX, bounds.getCenterY(), 2, colors.stepInactiveIndicatorsColor, true);
        }
    }

    @Override
    protected void drawValueIndicatorBackground(RenderContext ctx, Rectangle region, SliderColors colors) {
        Color tint = colors.valueIndicatorColor.copy().alpha(Math.round(255 * 0.52f));
        LiquidGlassStyle.draw(ctx, region.x, region.y, region.width, region.height,
                region.height / 2, LiquidGlassStyle.CONTROL, tint);
    }

    private float trackY(Rectangle bounds) {
        return bounds.y + (style().size().handleHeight() - style().size().trackHeight()) / 2;
    }
}
