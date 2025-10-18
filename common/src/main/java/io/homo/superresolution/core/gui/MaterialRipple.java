package io.homo.superresolution.core.gui;

import io.homo.superresolution.core.gui.core.animator.Easing;
import io.homo.superresolution.core.gui.core.animator.NumberAnimator;
import io.homo.superresolution.core.gui.core.backends.interfaces.IPaint;
import io.homo.superresolution.core.gui.core.backends.interfaces.IUIDrawContext;
import io.homo.superresolution.core.gui.core.impl.Rectangle;
import io.homo.superresolution.core.utils.Color;
import org.joml.Vector2f;

public class MaterialRipple {
    private NumberAnimator radiusAnimator = new NumberAnimator();
    private NumberAnimator alphaAnimator = new NumberAnimator();

    private float maxRippleRadius = 0;
    private Vector2f rippleCenter = null;
    private Vector2f rippleSize = null;
    private boolean fading = false;
    private boolean pressed = false;
    private boolean waitingForFade = false;
    private long fadeDuration = 1500;
    private boolean active = false;

    public void update() {
        radiusAnimator.update();
        alphaAnimator.update();

        if (active && alphaAnimator.floatValue() <= 0.01f) {
            reset();
            return;
        }
    }

    public void setPressed(boolean pressed, Vector2f center, Rectangle region) {
        if (fading || center == null || region == null) {
            return;
        }

        if (active && pressed) {
            reset();
        }

        this.pressed = pressed;
        this.rippleCenter = new Vector2f(center);
        this.rippleSize = new Vector2f(region.getSize());
        this.maxRippleRadius = calcMaxDistance(rippleCenter, region);

        long radiusDuration = 400; // Reduced duration for smoother animation
        float beginAlpha = 0.3f;

        if (pressed) {
            resetAnimations();

            active = true;
            fading = false;
            waitingForFade = false;

            radiusAnimator.set(0);
            radiusAnimator
                    .ease(Easing.LINEAR)
                    .animateTo(this.maxRippleRadius, radiusDuration)
                    .onComplete(() -> {
                        if (!this.pressed && !fading) {
                            startFadeOut();
                        }
                    });
            alphaAnimator.set(beginAlpha);
            alphaAnimator
                    .ease(Easing.LINEAR)
                    .animateTo(beginAlpha, radiusDuration);
        } else {
            if (isRippleFilled()) {
                startFadeOut();
            } else {
                waitingForFade = true;
            }
        }
    }

    private float calcMaxDistance(Vector2f center, Rectangle region) {
        float[] dx = {
                region.x,                    // Top-left
                region.getLimitX(),         // Top-right
                region.getLimitX(),         // Bottom-right
                region.x                    // Bottom-left
        };
        float[] dy = {
                region.y,                    // Top-left
                region.y,                    // Top-right
                region.getLimitY(),         // Bottom-right
                region.getLimitY()          // Bottom-left
        };

        float max = 0;
        for (int i = 0; i < 4; i++) {
            float dist = (float) Math.sqrt(
                    Math.pow(dx[i] - center.x, 2) +
                            Math.pow(dy[i] - center.y, 2)
            );
            max = Math.max(max, dist);
        }

        max = Math.max(max, Math.max(region.width, region.height) * 0.5f) * 1.1f;

        return max;
    }

    private boolean isRippleFilled() {
        return radiusAnimator.isCompleted();
    }

    private void startFadeOut() {
        fading = true;
        float currentAlpha = alphaAnimator.floatValue();
        alphaAnimator
                .set(currentAlpha)
                .ease(Easing.LINEAR)
                .animateTo(0, 500)
                .onComplete(() -> {
                    fading = false;
                    if (!pressed) {
                        reset();
                    }
                });
    }

    private void resetAnimations() {
        radiusAnimator.set(0);
        alphaAnimator.set(0);
    }

    private void reset() {
        active = false;
        fading = false;
        waitingForFade = false;
        pressed = false;
        rippleCenter = null;
        rippleSize = null;
        maxRippleRadius = 0;

        radiusAnimator.reset();
        alphaAnimator.reset();
    }

    public IPaint getPaint(
            Color color,
            IUIDrawContext drawContext,
            Vector2f position,
            Vector2f size
    ) {
        if (!active || rippleCenter == null) {
            return drawContext.createPaint();
        }
        float radius = radiusAnimator.floatValue();
        float alphaValue = alphaAnimator.floatValue();
        return drawContext.radialGradient(
                rippleCenter.x,
                rippleCenter.y,
                Math.max(radius - 5, 0),
                radius,
                color.copy().alpha((int) (255 * 0.2 * alphaValue)),
                color.copy().alpha(0)
        );
    }

    public boolean shouldRender() {
        return active || alphaAnimator.floatValue() > 0.01f || fading ||
                radiusAnimator.isRunning() || alphaAnimator.isRunning();
    }
}