package io.homo.superresolution.core.gui;

import io.homo.superresolution.core.gui.core.animator.Easing;
import io.homo.superresolution.core.gui.core.animator.NumberAnimator;
import io.homo.superresolution.core.gui.core.backends.interfaces.IPaint;
import io.homo.superresolution.core.gui.core.backends.interfaces.IUIDrawContext;
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

        if (waitingForFade && isRippleFilled() && !fading) {
            startFadeOut();
            waitingForFade = false;
        }
    }

    public void setPressed(boolean pressed, Vector2f center, Vector2f size) {
        if (fading) {
            return;
        }

        if (active) {
            reset();
        }

        this.pressed = pressed;
        this.rippleCenter = center == null ? new Vector2f(size.x / 2, size.y / 2) : new Vector2f(center);
        this.rippleSize = new Vector2f(size);
        this.maxRippleRadius = calcMaxDistance(rippleCenter, size);

        long radiusDuration = 1500;
        float beginAlpha = 0.3f;

        if (pressed) {
            resetAnimations();

            active = true;
            fading = false;
            waitingForFade = false;

            radiusAnimator.set(0);
            radiusAnimator
                    .ease(Easing.LINEAR)
                    .animateTo(1, radiusDuration)
                    .onComplete(() -> {
                        if (!this.pressed && !fading) {
                            startFadeOut();
                        }
                    });
            alphaAnimator.set(beginAlpha);
        } else {
            if (isRippleFilled()) {
                startFadeOut();
            } else {
                waitingForFade = true;
            }
        }
    }

    private float calcMaxDistance(Vector2f center, Vector2f size) {
        float[] dx = {0, size.x, 0, size.x};
        float[] dy = {0, 0, size.y, size.y};
        float max = 0;
        for (int i = 0; i < 4; i++) {
            float dist = (float) Math.hypot(center.x - dx[i], center.y - dy[i]);
            if (dist > max) max = dist;
        }
        return max;
    }

    private boolean isRippleFilled() {
        boolean filled = radiusAnimator.isCompleted();
        return filled;
    }

    private void startFadeOut() {
        fading = true;
        float currentAlpha = alphaAnimator.floatValue();
        alphaAnimator
                .set(currentAlpha)
                .ease(Easing.LINEAR)
                .animateTo(0, fadeDuration)
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
        if (!active) {
            return drawContext.createPaint();
        }

        float radius = maxRippleRadius * Math.min(1.0f, radiusAnimator.floatValue());
        float alphaValue = alphaAnimator.floatValue();

        return drawContext.radialGradient(
                position.x,
                position.y,
                Math.max(radius - 5, 0),
                radius + 8,
                color.copy().alpha((int) (255 * 0.2 * alphaValue)),
                color.copy().alpha(0)
        );
    }

    public boolean shouldRender() {
        boolean shouldRender = active && (alphaAnimator.floatValue() > 0.01f || fading || radiusAnimator.isRunning() || alphaAnimator.isRunning());
        return shouldRender;
    }
}