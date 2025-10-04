package io.homo.superresolution.core.gui;

import io.homo.superresolution.core.gui.core.UIDrawContext;
import io.homo.superresolution.core.gui.core.animator.Easing;
import io.homo.superresolution.core.gui.core.animator.NumberAnimator;
import io.homo.superresolution.core.math.Vector2f;
import io.homo.superresolution.core.utils.Color;
import org.lwjgl.nanovg.NVGPaint;

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
            System.out.println("[MaterialRipple][update] alpha过低，重置ripple");
            reset();
            return;
        }

        if (waitingForFade && isRippleFilled() && !fading) {
            System.out.println("[MaterialRipple][update] ripple已填满，准备淡出");
            startFadeOut();
            waitingForFade = false;
        }
    }

    public void setPressed(boolean pressed, Vector2f center, Vector2f size) {
        System.out.println("[MaterialRipple][setPressed] pressed=" + pressed + ", fading=" + fading + ", active=" + active);
        if (fading) {
            System.out.println("[MaterialRipple][setPressed] 当前正在淡出，忽略本次按下事件");
            return;
        }

        if (active) {
            System.out.println("[MaterialRipple][setPressed] ripple处于激活状态，重置");
            reset();
        }

        this.pressed = pressed;
        this.rippleCenter = center == null ? new Vector2f(size.x / 2, size.y / 2) : center.copy();
        this.rippleSize = size.copy();
        this.maxRippleRadius = calcMaxDistance(rippleCenter, size);

        long radiusDuration = 1500;
        float beginAlpha = 0.3f;

        if (pressed) {
            System.out.println("[MaterialRipple][setPressed] 开始新的ripple动画");
            resetAnimations();

            active = true;
            fading = false;
            waitingForFade = false;

            radiusAnimator.set(0);
            radiusAnimator
                    .ease(Easing.LINEAR)
                    .animateTo(1, radiusDuration)
                    .onComplete(() -> {
                        System.out.println("[MaterialRipple][radiusAnimator.onComplete] 动画完成，pressed=" + this.pressed + ", fading=" + fading);
                        if (!this.pressed && !fading) {
                            startFadeOut();
                        }
                    });
            alphaAnimator.set(beginAlpha);
        } else {
            System.out.println("[MaterialRipple][setPressed] 松开ripple");
            if (isRippleFilled()) {
                System.out.println("[MaterialRipple][setPressed] ripple已填满，直接淡出");
                startFadeOut();
            } else {
                System.out.println("[MaterialRipple][setPressed] ripple未填满，等待淡出");
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
        System.out.println("[MaterialRipple][startFadeOut] 淡出动画开始");
        fading = true;
        float currentAlpha = alphaAnimator.floatValue();
        alphaAnimator
                .set(currentAlpha)
                .ease(Easing.LINEAR)
                .animateTo(0, fadeDuration)
                .onComplete(() -> {
                    System.out.println("[MaterialRipple][alphaAnimator.onComplete] 淡出动画完成，pressed=" + pressed);
                    fading = false;
                    if (!pressed) {
                        reset();
                    }
                });
    }

    private void resetAnimations() {
        System.out.println("[MaterialRipple][resetAnimations] 重置动画");
        radiusAnimator.set(0);
        alphaAnimator.set(0);
    }

    private void reset() {
        System.out.println("[MaterialRipple][reset] ripple状态重置");
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

    public NVGPaint getPaint(
            Color color,
            UIDrawContext drawContext,
            Vector2f position,
            Vector2f size
    ) {
        if (!active) {
            return NVGPaint.calloc();
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