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

package io.homo.superresolution.core.gui.core;

import io.homo.superresolution.core.gui.MaterialElevation;
import io.homo.superresolution.core.gui.MaterialScheme;
import io.homo.superresolution.core.gui.MaterialUI;
import io.homo.superresolution.core.gui.core.animator.Animator;
import io.homo.superresolution.core.gui.core.animator.TimeInterpolator;
import io.homo.superresolution.core.gui.core.backends.interfaces.IFont;
import io.homo.superresolution.core.gui.core.backends.interfaces.TextAlign;
import io.homo.superresolution.core.gui.core.backends.interfaces.TextAlignType;
import io.homo.superresolution.core.gui.core.backends.interfaces.TextMetrics;
import io.homo.superresolution.core.gui.core.backends.render.RenderContext;
import io.homo.superresolution.core.gui.core.impl.Tooltip;
import org.joml.Vector2f;

import java.util.Optional;

public class TooltipRenderer {

    private static final float DEFAULT_MAX_WIDTH = 420f;
    private static final float PADDING = 8f;

    private final Animator.FloatAnimator alphaAnimator = Animator.ofFloat(0f, 0f);
    private final Animator.FloatAnimator widthAnimator = Animator.ofFloat(0f, 0f);
    private final Animator.FloatAnimator heightAnimator = Animator.ofFloat(0f, 0f);

    private final Vector2f anchor = new Vector2f();

    private TooltipPosition tooltipPos = TooltipPosition.AUTO;
    private boolean show = false;
    private boolean isHiding = false;

    private float radius = 6f;
    private float fontSize = 13f;
    private float maxWidth = DEFAULT_MAX_WIDTH;

    private Tooltip lastTooltip = Tooltip.empty();

    public TooltipRenderer() {
        alphaAnimator.set(0f);
        widthAnimator.set(0f);
        heightAnimator.set(0f);
    }

    public void setTooltipPosition(TooltipPosition tooltipPos) {
        this.tooltipPos = tooltipPos == null ? TooltipPosition.AUTO : tooltipPos;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }

    public void setFontSize(float fontSize) {
        this.fontSize = fontSize;
    }

    public void setMaxWidth(float maxWidth) {
        this.maxWidth = maxWidth;
    }

    public void render(RenderContext ctx, UIInputState inputState, Tooltip tooltip) {
        Animator.updateAll(alphaAnimator, widthAnimator,heightAnimator);
        if (tooltip.isEmpty()) {
            hideTooltip();
        }

        Vector2f mousePos = inputState.mousePosition();
        anchor.set(mousePos);

        if (!tooltip.isEmpty()){
            lastTooltip = tooltip;
            if (!show) {
                showTooltip();
            }
        }

        float currentAlpha = alphaAnimator.get() == null ? 0f : alphaAnimator.get();
        if (currentAlpha <= 0.001f) {
            isHiding = false;
            lastTooltip = Tooltip.empty();
            return;
        }

        MaterialScheme scheme = MaterialUI.Scheme;
        IFont font = ctx.font();
        float viewportWidth = ctx.viewportWidth();
        float viewportHeight = ctx.viewportHeight();
        float logicalMaxWidth = maxWidth;

        Tooltip targetTooltip = tooltip;
        if (tooltip.isEmpty() && lastTooltip != null){
            targetTooltip = lastTooltip;
        }

        TextMetrics metrics = ctx.measureTextMetrics(
                font,
                fontSize,
                targetTooltip.context(),
                logicalMaxWidth - PADDING * 2,
                fontSize + 2,
                true
        );

        float textWidth = metrics.maxLineWidth;
        float textHeight = metrics.totalHeight;

        float width = textWidth + PADDING * 2;
        float height = textHeight + PADDING * 2;
        if (!tooltip.isEmpty()) {
            widthAnimator
                    .fromTo(widthAnimator.get() == null ? 0f : widthAnimator.get(), width)
                    .duration(250)
                    .timeInterpolator(TimeInterpolator.easeOutQuint())
                    .start();
            heightAnimator
                    .fromTo(heightAnimator.get() == null ? 0f : heightAnimator.get(), height)
                    .duration(250)
                    .timeInterpolator(TimeInterpolator.easeOutQuint())
                    .start();
        }
        width = widthAnimator.get();
        height = heightAnimator.get();
        Vector2f basePos = calculatePosition(
                anchor.x,
                anchor.y,
                width,
                height,
                tooltipPos,
                viewportWidth,
                viewportHeight
        );

        float baseX = Math.max(8, Math.min(basePos.x, viewportWidth - width - 8));
        float baseY = Math.max(8, Math.min(basePos.y, viewportHeight - height - 8));

        float animatedWidth = Math.max(width, 20f);

        ctx.save();
        ctx.pushAlpha(Math.min(currentAlpha, 1.0f));

        MaterialElevation.draw(
                ctx,
                2,
                baseX,
                baseY,
                animatedWidth,
                height,
                radius
        );
        ctx.roundedRect(
                baseX,
                baseY,
                animatedWidth,
                height,
                radius,
                scheme.surfaceContainerHigh(),
                true
        );

        ctx.resetScissor();

        ctx.scissor(
                baseX,
                baseY,
                animatedWidth,
                height
        );

        float textX = baseX + PADDING;
        float textY = baseY + PADDING;

        ctx.drawAlignedText(
                font,
                fontSize,
                metrics,
                textX,
                textY,
                animatedWidth - PADDING * 2,
                fontSize + 2,
                scheme.onSurface(),
                TextAlign.of(TextAlignType.ALIGN_LEFT, TextAlignType.ALIGN_TOP),
                true
        );

        ctx.resetScissor();
        ctx.popAlpha();
        ctx.restore();
    }

    private void showTooltip() {
        if (this.show) {
            return;
        }
        this.show = true;
        this.isHiding = false;
        alphaAnimator
                .fromTo(alphaAnimator.get() == null ? 0f : alphaAnimator.get(), 1f)
                .duration(150)
                .timeInterpolator(TimeInterpolator.linear())
                .start();
    }

    private void hideTooltip() {
        if (!this.show) {
            return;
        }
        this.show = false;
        this.isHiding = true;
        alphaAnimator
                .fromTo(alphaAnimator.get() == null ? 1f : alphaAnimator.get(), 0f)
                .duration(150)
                .timeInterpolator(TimeInterpolator.linear())
                .start();
    }

    private void updateAnimatorTarget() {

    }

    public void reset() {
        alphaAnimator.set(0f);
        widthAnimator.set(0f);
        heightAnimator.set(0f);
        isHiding = false;
        show = false;
        lastTooltip = Tooltip.empty();
    }

    private Vector2f calculatePosition(float targetX, float targetY,
                                       float width, float height,
                                       TooltipPosition pos,
                                       float screenWidth, float screenHeight) {
        final float offset = 12f;
        switch (pos) {
            case AUTO -> {
                if (targetY - height - offset > 0) {
                    return calculatePosition(targetX, targetY, width, height, TooltipPosition.TOP, screenWidth, screenHeight);
                } else if (targetY + offset + height < screenHeight) {
                    return calculatePosition(targetX, targetY, width, height, TooltipPosition.BOTTOM, screenWidth, screenHeight);
                } else if (targetX - width - offset > 0) {
                    return calculatePosition(targetX, targetY, width, height, TooltipPosition.LEFT, screenWidth, screenHeight);
                } else if (targetX + offset + width + 20 < screenWidth) {
                    return calculatePosition(targetX, targetY, width, height, TooltipPosition.RIGHT, screenWidth, screenHeight);
                } else if (targetY - height - offset > 0 && targetX - width - offset > 0) {
                    return calculatePosition(targetX, targetY, width, height, TooltipPosition.LEFT_TOP, screenWidth, screenHeight);
                } else if (targetY - height - offset > 0 && targetX + width + offset < screenWidth) {
                    return calculatePosition(targetX, targetY, width, height, TooltipPosition.RIGHT_TOP, screenWidth, screenHeight);
                } else if (targetY + height + offset < screenHeight && targetX - width - offset > 0) {
                    return calculatePosition(targetX, targetY, width, height, TooltipPosition.LEFT_BOTTOM, screenWidth, screenHeight);
                } else if (targetY + height + offset < screenHeight && targetX + width + offset < screenWidth) {
                    return calculatePosition(targetX, targetY, width, height, TooltipPosition.RIGHT_BOTTOM, screenWidth, screenHeight);
                } else {
                    return calculatePosition(targetX, targetY, width, height, TooltipPosition.LEFT_CENTER, screenWidth, screenHeight);
                }
            }
            case TOP -> {
                return new Vector2f(
                        targetX - (width * 0.5f),
                        targetY - height - offset
                );
            }
            case BOTTOM -> {
                return new Vector2f(
                        targetX,
                        targetY + offset + (height * 0.6f)
                );
            }
            case LEFT -> {
                return new Vector2f(
                        targetX - width - offset,
                        targetY - (height * 0.5f)
                );
            }
            case RIGHT -> {
                return new Vector2f(
                        targetX + offset + 20,
                        targetY - (height * 0.5f)
                );
            }
            case LEFT_TOP -> {
                return new Vector2f(
                        targetX - width - offset,
                        targetY - height - offset
                );
            }
            case RIGHT_TOP -> {
                return new Vector2f(
                        targetX + offset,
                        targetY - height - offset
                );
            }
            case LEFT_BOTTOM -> {
                return new Vector2f(
                        targetX - width - offset,
                        targetY + offset
                );
            }
            case RIGHT_BOTTOM -> {
                return new Vector2f(
                        targetX + offset,
                        targetY + offset
                );
            }
            case LEFT_CENTER -> {
                return new Vector2f(
                        targetX - width - offset,
                        targetY - height / 2f
                );
            }
            default -> {
                return new Vector2f(
                        targetX + offset,
                        targetY + offset
                );
            }
        }
    }

    public enum TooltipPosition {
        AUTO,
        TOP,
        BOTTOM,
        LEFT,
        RIGHT,
        LEFT_TOP,
        RIGHT_TOP,
        LEFT_BOTTOM,
        RIGHT_BOTTOM,
        LEFT_CENTER
    }
}