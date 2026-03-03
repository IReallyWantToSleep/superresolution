package io.homo.superresolution.core.gui.core;

import io.homo.superresolution.core.gui.MaterialScheme;
import io.homo.superresolution.core.gui.MaterialUI;
import io.homo.superresolution.core.gui.core.animator.Animator;
import io.homo.superresolution.core.gui.core.animator.TimeInterpolator;
import io.homo.superresolution.core.gui.core.backends.interfaces.IFont;
import io.homo.superresolution.core.gui.core.backends.interfaces.TextAlign;
import io.homo.superresolution.core.gui.core.backends.interfaces.TextAlignType;
import io.homo.superresolution.core.gui.core.backends.interfaces.TextMetrics;
import io.homo.superresolution.core.gui.core.backends.render.RenderContext;
import org.joml.Vector2f;

public class TooltipRenderer {

    private static final float DEFAULT_MAX_WIDTH = 320f;
    private static final float PADDING = 8f;

    private final Animator.FloatAnimator alphaAnimator = Animator.ofFloat(0f, 0f);
    private final Animator.FloatAnimator widthAnimator = Animator.ofFloat(0f, 0f);

    private final Vector2f anchor = new Vector2f();

    private TooltipPosition tooltipPos = TooltipPosition.AUTO;
    private boolean show = false;
    private boolean isHiding = false;

    private float radius = 6f;
    private float fontSize = 13f;
    private float maxWidth = DEFAULT_MAX_WIDTH;

    private String lastTooltip = "";

    public TooltipRenderer() {
        alphaAnimator.set(0f);
        widthAnimator.set(0f);
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

    public void render(RenderContext ctx, UIInputState inputState, String tooltip) {
        if (tooltip == null || tooltip.isEmpty()) {
            hideTooltip();
            Animator.updateAll(alphaAnimator, widthAnimator);
            return;
        }

        Vector2f mousePos = inputState.mousePosition();
        anchor.set(mousePos);

        if (!tooltip.equals(lastTooltip)) {
            lastTooltip = tooltip;
            showTooltip();
        } else if (!show) {
            showTooltip();
        }

        Animator.updateAll(alphaAnimator, widthAnimator);

        float currentAlpha = alphaAnimator.get() == null ? 0f : alphaAnimator.get();
        float currentWidth = widthAnimator.get() == null ? 0f : widthAnimator.get();

        if (currentAlpha <= 0.01f && currentWidth <= 0f) {
            return;
        }

        MaterialScheme scheme = MaterialUI.Scheme;
        IFont font = ctx.font();

        float viewportWidth = ctx.viewportWidth();
        float viewportHeight = ctx.viewportHeight();

        float logicalMaxWidth = maxWidth > 0 ? maxWidth : viewportWidth * 0.7f;

        TextMetrics metrics = ctx.measureTextMetrics(
                font,
                fontSize,
                tooltip,
                logicalMaxWidth - PADDING * 2,
                fontSize + 2,
                true
        );

        float textWidth = metrics.maxLineWidth;
        float textHeight = metrics.totalHeight;

        float width = textWidth + PADDING * 2;
        float height = textHeight + PADDING * 2;

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

        float animatedWidth = Math.max(width * currentWidth, 20f);

        ctx.save();
        ctx.pushAlpha(Math.min(currentAlpha * 1.2f, 1.0f));

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
        updateAnimatorTarget();
    }

    private void hideTooltip() {
        if (!this.show && !isHiding) {
            return;
        }
        this.show = false;
        this.isHiding = true;
        updateAnimatorTarget();
    }

    private void updateAnimatorTarget() {
        if (show) {
            widthAnimator
                    .fromTo(widthAnimator.get() == null ? 0f : widthAnimator.get(), 1f)
                    .duration(250)
                    .timeInterpolator(TimeInterpolator.easeOutQuint())
                    .start();
            alphaAnimator
                    .fromTo(alphaAnimator.get() == null ? 0f : alphaAnimator.get(), 1f)
                    .duration(250)
                    .timeInterpolator(TimeInterpolator.easeOutCubic())
                    .start();
        } else if (isHiding) {
            alphaAnimator
                    .fromTo(alphaAnimator.get() == null ? 1f : alphaAnimator.get(), 0f)
                    .duration(200)
                    .timeInterpolator(TimeInterpolator.easeInQuad())
                    .onLifecycle(new Animator.AnimatorLifecycleListener() {
                        @Override
                        public void onEnd() {
                            if (!show) {
                                widthAnimator.set(0f);
                            }
                        }
                    })
                    .start();
        }
    }

    public void reset() {
        alphaAnimator.set(0f);
        widthAnimator.set(0f);
        isHiding = false;
        show = false;
        lastTooltip = "";
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
}