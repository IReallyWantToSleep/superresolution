/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
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

package io.homo.superresolution.core.gui.widgets.progress;

import io.homo.superresolution.core.gui.core.UIInputState;
import io.homo.superresolution.core.gui.core.backends.render.RenderContext;
import io.homo.superresolution.core.gui.core.impl.Rectangle;
import io.homo.superresolution.core.gui.widgets.MaterialWidget;
import io.homo.superresolution.core.utils.Color;

public class MaterialLinearProgressIndicator extends MaterialWidget<MaterialLinearProgressIndicator> {
    protected float beginProgress;
    protected float endProgress;

    public MaterialLinearProgressIndicator setProgress(float begin, float end) {
        this.beginProgress = begin;
        this.endProgress = end;
        return this;
    }

    public float getProgress() {
        return endProgress;
    }

    public MaterialLinearProgressIndicator setProgress(float progress) {
        this.beginProgress = 0f;
        this.endProgress = progress;
        return this;
    }

    public float getBeginProgress() {
        return beginProgress;
    }

    public float getEndProgress() {
        return endProgress;
    }

    @Override
    protected void init() {
    }

    @Override
    public void layouting(RenderContext ctx) {
    }

    @Override
    public void render(RenderContext ctx, UIInputState inputState) {
        Rectangle bounds = getBounds();
        float x = bounds.x;
        float y = bounds.y;
        float width = bounds.width;
        float height = bounds.height;
        float radius = height / 2f;
        float centerY = y + height / 2f;
        float stopRadius = 2f;
        float gap = 4f;

        float begin = Math.max(0f, Math.min(1f, beginProgress));
        float end = Math.max(0f, Math.min(1f, endProgress));

        float stopXOffset = height > 4f ? (height - 4f) / 2 : 0f;
        if (end < begin) {
            float t = begin;
            begin = end;
            end = t;
        }

        Color activeColor = isDisabled()
                ? scheme().onSurface().copy().alpha((int) (255 * 0.38f))
                : scheme().primary();
        Color trackColor = isDisabled()
                ? scheme().onSurface().copy().alpha((int) (255 * 0.12f))
                : scheme().secondaryContainer();
        Color stopColor = isDisabled()
                ? scheme().onSurface().copy().alpha((int) (255 * 0.38f))
                : scheme().primary();

        boolean hasActive = (end - begin) > 0.001f;
        boolean hasLeftTrack = begin > 0.001f;
        boolean hasRightTrack = end < 0.999f;

        if (!hasActive) {
            ctx.roundedRect(x, y, width, height, radius, trackColor, true);
            ctx.arc(
                    x + width - stopRadius - stopXOffset,
                    centerY,
                    stopRadius,
                    stopColor,
                    true
            );
            return;
        }


        if (!hasLeftTrack && !hasRightTrack) {
            ctx.roundedRect(x, y, width, height, radius, activeColor, true);
            return;
        }

        float beginPx = begin * width;
        float endPx = end * width;

        if (hasLeftTrack) {
            float leftTrackW = beginPx - x;
            if (leftTrackW > 0) {
                ctx.roundedRectComplex(
                        x,
                        y,
                        leftTrackW,
                        height,
                        radius,
                        radius,
                        radius,
                        radius,
                        trackColor,
                        true
                );
            }
        }

        {
            float activeX = hasLeftTrack ? (x + beginPx + gap) : x;
            float activeRight = hasRightTrack ? (x + endPx - gap) : (x + width);
            float activeW = activeRight - activeX;
            if (activeW > 0) {
                ctx.roundedRectComplex(
                        activeX,
                        y,
                        activeW,
                        height,
                        radius,
                        radius,
                        radius,
                        radius,
                        activeColor,
                        true
                );
            }
        }

        if (hasRightTrack) {
            float rightTrackX = x + endPx;
            float endStopCX = x + width - stopRadius;
            float rightTrackW = x + width - rightTrackX;
            if (rightTrackW > 0) {
                ctx.roundedRectComplex(
                        rightTrackX,
                        y,
                        rightTrackW,
                        height,
                        radius,
                        radius,
                        radius,
                        radius,
                        trackColor,
                        true
                );
            }

            ctx.arc(endStopCX - stopXOffset, centerY, stopRadius, stopColor, true);
        }
    }
}
