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

package io.homo.superresolution.core.gui;

import io.homo.superresolution.core.gui.core.backends.interfaces.IPaint;
import io.homo.superresolution.core.gui.core.backends.interfaces.IUIDrawContext;
import io.homo.superresolution.core.gui.core.impl.Rectangle;
import io.homo.superresolution.core.utils.Color;
import io.homo.superresolution.core.gui.core.animator.Animator;
import io.homo.superresolution.core.gui.core.animator.TimeInterpolator;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MaterialRipple {
    private static final int DEFAULT_MAX_RIPPLES = 5;
    private static final long MAX_RIPPLE_AGE = 2000;

    private static final long RIPPLE_ENTER_DURATION = 300;
    private static final long RIPPLE_ORIGIN_DURATION = 300;
    private static final long OPACITY_ENTER_DURATION = 100;
    private static final long OPACITY_EXIT_DURATION = 250;
    private static final long OPACITY_HOLD_DURATION = OPACITY_ENTER_DURATION + 100;

    private static final float RIPPLE_SMOOTHNESS = 0.5f;

    private static final float START_RADIUS_RATIO = 0.0f;

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
            ripple.update();

            if (ripple.isDestroy() &&
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
        private static final float PRESSED_ALPHA = 0.08f;
        private Animator.FloatAnimator radiusAnimator;
        private Animator.FloatAnimator originAnimator;
        private Animator.FloatAnimator alphaAnimator;

        private Animator.AnimatorLifecycleListener radiusListener;
        private Animator.AnimatorLifecycleListener alphaListener;

        private final Vector2f rippleStartCenter;
        private final Vector2f rippleTargetCenter;
        private final Vector2f currentCenter;
        private final Rectangle region;
        private final long creationTime;
        private final long enterStartedAtMillis;
        private final float maxRippleRadius;
        private final float startRippleRadius;
        private volatile boolean isProcessingCallback = false;

        private RippleState state = RippleState.IDLE;
        private boolean pressed = true;
        private boolean isDestroy = false;

        public SingleRipple(Vector2f center, Rectangle region) {
            if (center == null || region == null) {
                throw new IllegalArgumentException("Center and region cannot be null");
            }

            this.region = region;
            this.rippleStartCenter = new Vector2f(center);
            this.rippleTargetCenter = new Vector2f(
                    region.x + region.width / 2f,
                    region.y + region.height / 2f
            );
            this.currentCenter = new Vector2f(rippleStartCenter);

            this.maxRippleRadius = calcMaxDistance(rippleTargetCenter, region);
            this.startRippleRadius = maxRippleRadius * START_RADIUS_RATIO;
            this.creationTime = System.currentTimeMillis();
            this.enterStartedAtMillis = creationTime;

            initAnimators();
            startRipple();
        }

        private void initAnimators() {
            radiusAnimator = new Animator.FloatAnimator(0f, 1f);
            radiusAnimator.duration(RIPPLE_ENTER_DURATION);
            radiusAnimator.timeInterpolator(TimeInterpolator.linear());

            originAnimator = new Animator.FloatAnimator(0f, 1f);
            originAnimator.duration(RIPPLE_ORIGIN_DURATION);
            originAnimator.timeInterpolator(TimeInterpolator.linear());
            originAnimator.onUpdate(progress -> {
                currentCenter.x = lerp(rippleStartCenter.x, rippleTargetCenter.x, progress);
                currentCenter.y = lerp(rippleStartCenter.y, rippleTargetCenter.y, progress);
            });

            alphaAnimator = new Animator.FloatAnimator(0f, 1f);
            alphaAnimator.duration(OPACITY_ENTER_DURATION);
            alphaAnimator.timeInterpolator(TimeInterpolator.linear());

            radiusListener = new Animator.AnimatorLifecycleListener() {
                @Override
                public void onEnd() {
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

            alphaListener = new Animator.AnimatorLifecycleListener() {
                @Override
                public void onEnd() {
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

        private float lerp(float start, float end, float t) {
            return start + (end - start) * t;
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

            if (radiusAnimator.isRunning()) radiusAnimator.cancel();
            if (originAnimator.isRunning()) originAnimator.cancel();
            if (alphaAnimator.isRunning()) alphaAnimator.cancel();

            radiusAnimator.removeLifecycleListener(radiusListener);
            alphaAnimator.removeLifecycleListener(alphaListener);

            radiusAnimator.fromTo(0f, 1f);
            radiusAnimator.duration(RIPPLE_ENTER_DURATION);

            originAnimator.fromTo(0f, 1f);
            originAnimator.duration(RIPPLE_ORIGIN_DURATION);

            alphaAnimator.fromTo(0f, 1f);
            alphaAnimator.duration(OPACITY_ENTER_DURATION);

            radiusAnimator.onLifecycle(radiusListener);

            radiusAnimator.start();
            originAnimator.start();
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

            long timeSinceEnter = System.currentTimeMillis() - enterStartedAtMillis;
            long delay = 0;
            if (timeSinceEnter > 0 && timeSinceEnter < OPACITY_HOLD_DURATION) {
                delay = OPACITY_HOLD_DURATION - timeSinceEnter;
            }

            if (alphaAnimator.isRunning()) {
                alphaAnimator.cancel();
            }
            alphaAnimator.removeLifecycleListener(alphaListener);
            alphaAnimator.fromTo(1f, 0f);
            alphaAnimator.duration(OPACITY_EXIT_DURATION);
            alphaAnimator.startDelay(delay);
            alphaAnimator.timeInterpolator(TimeInterpolator.linear());
            alphaAnimator.onLifecycle(alphaListener);
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

            return maxDistance;
        }

        private float distance(float x1, float y1, float x2, float y2) {
            float dx = x2 - x1;
            float dy = y2 - y1;
            return (float) Math.sqrt(dx * dx + dy * dy);
        }

        public IPaint createPaint(Color color, IUIDrawContext drawContext) {
            if (isDestroy || state == RippleState.IDLE) {
                return null;
            }

            float radiusProgress = radiusAnimator.get();
            float alphaProgress = alphaAnimator.get();

            float currentRadius = lerp(startRippleRadius, maxRippleRadius, radiusProgress);
            float currentAlpha = PRESSED_ALPHA * alphaProgress;
            if (true) {
                float maxGradientRadius = currentRadius / (1 - RIPPLE_SMOOTHNESS);
                float innerRadius = currentRadius * RIPPLE_SMOOTHNESS;

                Color centerColor = color.copy().alpha((int) (255 * currentAlpha));
                Color edgeColor = color.copy().alpha(0);

                return drawContext.radialGradient(
                        currentCenter.x,
                        currentCenter.y,
                        innerRadius,
                        maxGradientRadius,
                        centerColor,
                        edgeColor
                );
            } else {
                Color centerColor = color.copy().alpha((int) (255 * currentAlpha));
                Color edgeColor = color.copy().alpha(0);

                return drawContext.radialGradient(
                        currentCenter.x,
                        currentCenter.y,
                        0,
                        currentRadius,
                        centerColor,
                        edgeColor
                );
            }
        }

        public boolean shouldRender() {
            return !isDestroy && state != RippleState.IDLE;
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
            return new Vector2f(currentCenter);
        }

        public void update() {
            if (radiusAnimator != null) radiusAnimator.update();
            if (originAnimator != null) originAnimator.update();
            if (alphaAnimator != null) alphaAnimator.update();
        }

        public void destroy() {
            if (isDestroy) return;
            isDestroy = true;
            state = RippleState.IDLE;

            if (radiusAnimator != null) {
                if (radiusListener != null) {
                    radiusAnimator.removeLifecycleListener(radiusListener);
                }
                if (radiusAnimator.isRunning()) {
                    radiusAnimator.cancel();
                }
                radiusAnimator = null;
            }

            if (originAnimator != null) {
                if (originAnimator.isRunning()) {
                    originAnimator.cancel();
                }
                originAnimator = null;
            }

            if (alphaAnimator != null) {
                if (alphaListener != null) {
                    alphaAnimator.removeLifecycleListener(alphaListener);
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