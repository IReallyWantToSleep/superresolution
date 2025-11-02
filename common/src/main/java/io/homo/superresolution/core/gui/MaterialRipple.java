package io.homo.superresolution.core.gui;

import io.homo.superresolution.core.gui.core.backends.interfaces.IPaint;
import io.homo.superresolution.core.gui.core.backends.interfaces.IUIDrawContext;
import io.homo.superresolution.core.gui.core.impl.Rectangle;
import io.homo.superresolution.core.utils.Color;
import io.homo.superresolution.thirdparty.icyllis.modernui.animation.*;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MaterialRipple {
    private static final int DEFAULT_MAX_RIPPLES = 5;
    private static final long MAX_RIPPLE_AGE = 2000;
    private final List<SingleRipple> activeRipples = new ArrayList<>();
    private final int maxConcurrentRipples;
    private boolean pressed = false;

    public MaterialRipple() {
        this(DEFAULT_MAX_RIPPLES);
    }

    public MaterialRipple(int maxConcurrentRipples) {
        this.maxConcurrentRipples = Math.max(1, maxConcurrentRipples);
    }

    public void update() {
        Iterator<SingleRipple> iterator = activeRipples.iterator();
        long currentTime = System.currentTimeMillis();

        while (iterator.hasNext()) {
            SingleRipple ripple = iterator.next();

            if (ripple.isDestroy() ||
                    (currentTime - ripple.getCreationTime()) > MAX_RIPPLE_AGE) {
                ripple.destroy();
                iterator.remove();
            }
        }
    }

    public void setPressed(boolean pressed, Vector2f center, Rectangle region) {
        if (center == null || region == null) {
            return;
        }

        if (pressed) {
            addNewRipple(center, region);
            this.pressed = true;
        } else {
            releaseAllRipples();
            this.pressed = false;
        }
    }

    private void addNewRipple(Vector2f center, Rectangle region) {
        if (activeRipples.size() >= maxConcurrentRipples) {
            SingleRipple oldestInactive = findOldestInactiveRipple();
            if (oldestInactive != null) {
                oldestInactive.destroy();
                activeRipples.remove(oldestInactive);
            } else {
                SingleRipple oldest = findOldestRipple();
                if (oldest != null) {
                    oldest.destroy();
                    activeRipples.remove(oldest);
                }
            }
        }

        SingleRipple newRipple = new SingleRipple(center, region);
        activeRipples.add(newRipple);
    }

    private void releaseAllRipples() {
        for (SingleRipple ripple : activeRipples) {
            ripple.setPressed(false);
        }
    }

    private SingleRipple findOldestRipple() {
        SingleRipple oldest = null;
        for (SingleRipple ripple : activeRipples) {
            if (oldest == null || ripple.getCreationTime() < oldest.getCreationTime()) {
                oldest = ripple;
            }
        }
        return oldest;
    }

    private SingleRipple findOldestInactiveRipple() {
        SingleRipple oldest = null;
        for (SingleRipple ripple : activeRipples) {
            if (!ripple.isPressed()) {
                if (oldest == null || ripple.getCreationTime() < oldest.getCreationTime()) {
                    oldest = ripple;
                }
            }
        }
        return oldest;
    }

    public IPaint[] getPaints(Color color, IUIDrawContext drawContext, Vector2f position, Vector2f size) {
        List<IPaint> paints = new ArrayList<>();

        for (SingleRipple ripple : activeRipples) {
            if (ripple.shouldRender() && !ripple.isDestroy()) {
                IPaint paint = ripple.createPaint(color, drawContext);
                if (paint != null) {
                    paints.add(paint);
                }
            }
        }

        return paints.toArray(new IPaint[0]);
    }

    public boolean shouldRender() {
        for (SingleRipple ripple : activeRipples) {
            if (ripple.shouldRender()) {
                return true;
            }
        }
        return false;
    }

    public boolean isPressed() {
        return pressed;
    }

    public void clearAllRipples() {
        for (SingleRipple ripple : activeRipples) {
            ripple.destroy();
        }
        activeRipples.clear();
    }

    public void destroy() {
        clearAllRipples();
    }

    public static class SingleRipple {
        private static final long EXPAND_DURATION = 225;
        private static final long FADE_OUT_DURATION = 375;
        private static final float PRESSED_ALPHA = 0.12f;
        private ValueAnimator radiusAnimator;
        private ValueAnimator alphaAnimator;
        private AnimatorListener radiusListener;
        private AnimatorListener alphaListener;

        private final Vector2f rippleCenter;
        private final long creationTime;
        private final float maxRippleRadius;
        private volatile boolean isProcessingCallback = false;

        private RippleState state = RippleState.IDLE;
        private boolean pressed = true;
        private boolean isDestroy = false;

        public SingleRipple(Vector2f center, Rectangle region) {
            if (center == null || region == null) {
                throw new IllegalArgumentException("Center and region cannot be null");
            }

            this.rippleCenter = new Vector2f(center);
            this.maxRippleRadius = calcMaxDistance(rippleCenter, region);
            this.creationTime = System.currentTimeMillis();

            initAnimators();
            startRipple();
        }

        private void initAnimators() {
            radiusAnimator = ValueAnimator.ofFloat(0f, 0f);
            radiusAnimator.setDuration(EXPAND_DURATION);
            radiusAnimator.setInterpolator(new BezierInterpolator(0.5f, 0.0f, 0.5f, 1f));
            alphaAnimator = ValueAnimator.ofFloat(1f, 1f);
            alphaAnimator.setDuration(FADE_OUT_DURATION);
            alphaAnimator.setInterpolator(TimeInterpolator.LINEAR);
            radiusListener = new AnimatorListener() {
                @Override
                public void onAnimationEnd(@NotNull Animator animation, boolean isReverse) {
                    if (isProcessingCallback || isDestroy || state != RippleState.EXPANDING) {
                        return;
                    }
                    isProcessingCallback = true;
                    try {
                        onExpandComplete();
                    } finally {
                        isProcessingCallback = false;
                    }
                }
            };

            alphaListener = new AnimatorListener() {
                @Override
                public void onAnimationEnd(@NotNull Animator animation, boolean isReverse) {
                    if (isProcessingCallback || isDestroy || state != RippleState.FADING_OUT) {
                        return;
                    }
                    isProcessingCallback = true;
                    try {
                        onFadeOutComplete();
                    } finally {
                        isProcessingCallback = false;
                    }
                }
            };
        }

        public boolean isPressed() {
            return pressed;
        }

        public void setPressed(boolean pressed) {
            if (isDestroy) {
                return;
            }

            if (this.pressed == pressed) {
                return;
            }
            this.pressed = pressed;

            if (!pressed && state == RippleState.EXPANDED) {
                startFadeOut();
            }
        }

        private void startRipple() {
            if (isDestroy) {
                return;
            }

            state = RippleState.EXPANDING;
            if (radiusAnimator.isRunning()) {
                radiusAnimator.cancel();
            }
            if (alphaAnimator.isRunning()) {
                alphaAnimator.cancel();
            }
            radiusAnimator.removeListener(radiusListener);
            alphaAnimator.removeListener(alphaListener);
            radiusAnimator.setValues(PropertyValuesHolder.ofFloat(0f, 1f));
            radiusAnimator.setDuration(EXPAND_DURATION);
            radiusAnimator.setInterpolator(new BezierInterpolator(0.5f, 0.0f, 0.5f, 1f));
            alphaAnimator.setValues(PropertyValuesHolder.ofFloat(0f, 1f));
            alphaAnimator.setDuration((long) (EXPAND_DURATION * 0.3f));
            alphaAnimator.setInterpolator(TimeInterpolator.LINEAR);
            radiusAnimator.addListener(radiusListener);
            alphaAnimator.addListener(alphaListener);
            radiusAnimator.start();
            alphaAnimator.start();
        }

        private void onExpandComplete() {
            if (state != RippleState.EXPANDING) {
                return;
            }

            state = RippleState.EXPANDED;

            if (!pressed) {
                startFadeOut();
            }
        }

        private void startFadeOut() {
            if (state == RippleState.FADING_OUT || isDestroy) {
                return;
            }

            state = RippleState.FADING_OUT;
            if (alphaAnimator.isRunning()) {
                alphaAnimator.cancel();
            }
            alphaAnimator.removeListener(alphaListener);
            alphaAnimator.setValues(PropertyValuesHolder.ofFloat(1f, 0f));
            alphaAnimator.setDuration(FADE_OUT_DURATION);
            alphaAnimator.setInterpolator(TimeInterpolator.LINEAR);
            alphaAnimator.addListener(alphaListener);
            alphaAnimator.start();
        }

        private void onFadeOutComplete() {
            destroy();
        }

        private float calcMaxDistance(Vector2f center, Rectangle region) {
            float maxDistance = 0;
            float centerX = center.x;
            float centerY = center.y;

            maxDistance = Math.max(maxDistance, distance(centerX, centerY, region.x, region.y));
            maxDistance = Math.max(maxDistance, distance(centerX, centerY, region.getLimitX(), region.y));
            maxDistance = Math.max(maxDistance, distance(centerX, centerY, region.getLimitX(), region.getLimitY()));
            maxDistance = Math.max(maxDistance, distance(centerX, centerY, region.x, region.getLimitY()));

            maxDistance = Math.max(maxDistance, Math.max(region.width, region.height) * 0.5f);

            return maxDistance;
        }

        private float distance(float x1, float y1, float x2, float y2) {
            float dx = x2 - x1;
            float dy = y2 - y1;
            return (float) Math.sqrt(dx * dx + dy * dy);
        }

        public IPaint createPaint(Color color, IUIDrawContext drawContext) {
            if (isDestroy || state == RippleState.IDLE) {
                return drawContext.createPaint();
            }
            float radiusProgress = (float) radiusAnimator.getAnimatedValue();
            float alphaProgress = (float) alphaAnimator.getAnimatedValue();

            float currentRadius = maxRippleRadius * radiusProgress;
            float currentAlpha = PRESSED_ALPHA * alphaProgress;

            Color startColor = color.copy().alpha((int) (255 * currentAlpha));
            Color endColor = color.copy().alpha(0);

            return drawContext.radialGradient(
                    rippleCenter.x,
                    rippleCenter.y,
                    0,
                    currentRadius,
                    startColor,
                    endColor
            );
        }

        public boolean shouldRender() {
            return !isDestroy;
        }

        public RippleState getState() {
            return state;
        }

        public boolean isDestroy() {
            return isDestroy;
        }

        public long getCreationTime() {
            return creationTime;
        }

        public Vector2f getCenter() {
            return new Vector2f(rippleCenter);
        }

        public void destroy() {
            if (isDestroy) return;
            isDestroy = true;
            state = RippleState.IDLE;
            if (radiusAnimator != null) {
                if (radiusListener != null) {
                    radiusAnimator.removeListener(radiusListener);
                }
                if (radiusAnimator.isRunning()) {
                    radiusAnimator.cancel();
                }
                radiusAnimator = null;
            }
            if (alphaAnimator != null) {
                if (alphaListener != null) {
                    alphaAnimator.removeListener(alphaListener);
                }
                if (alphaAnimator.isRunning()) {
                    alphaAnimator.cancel();
                }
                alphaAnimator = null;
            }
            radiusListener = null;
            alphaListener = null;
        }

        public enum RippleState {
            IDLE,
            EXPANDING,
            EXPANDED,
            FADING_OUT
        }
    }
}