package io.homo.superresolution.core.gui.core.animator;

import java.util.function.Consumer;

public class NumberAnimator implements IAnimator<Number> {
    private double currentValue;
    private double targetValue;
    private long duration;
    private long startTime;
    private boolean running;
    private boolean reset;
    private Easing.EasingMethod easingMethod;
    private Runnable onCompleteCallback;
    private Consumer<Number> onUpdateCallback;
    private long delay;
    private long delayStartTime;
    private AnimationState state;

    public NumberAnimator() {
        this.state = AnimationState.IDLE;
        this.running = false;
        this.reset = false;
        this.easingMethod = Easing.LINEAR;
    }

    @Override
    public IAnimator<Number> animateTo(Number target, long duration, boolean reset) {
        this.targetValue = target.doubleValue();
        this.duration = duration;
        this.reset = reset;
        this.state = AnimationState.RUNNING;
        this.startTime = System.currentTimeMillis();
        this.running = true;
        this.delay = 0;
        return this;
    }

    @Override
    public IAnimator<Number> animateTo(Number target, long duration) {
        return animateTo(target, duration, false);
    }

    @Override
    public IAnimator<Number> smoothUpdateTo(Number target, long duration) {
        return animateTo(target, duration, false);
    }

    @Override
    public IAnimator<Number> set(Number value) {
        this.currentValue = value.doubleValue();
        return this;
    }

    @Override
    public IAnimator<Number> delay(long delayMs) {
        this.delay = delayMs;
        this.delayStartTime = System.currentTimeMillis();
        return this;
    }

    @Override
    public IAnimator<Number> ease(Easing.EasingMethod easing) {
        this.easingMethod = easing;
        return this;
    }

    @Override
    public IAnimator<Number> onComplete(Runnable callback) {
        this.onCompleteCallback = callback;
        return this;
    }

    @Override
    public IAnimator<Number> onUpdate(Consumer<Number> callback) {
        this.onUpdateCallback = callback;
        return this;
    }

    @Override
    public float progress() {
        if (state == AnimationState.RUNNING) {
            long currentTime = System.currentTimeMillis();
            if (delay > 0 && currentTime - delayStartTime < delay) {
                return 0;
            }

            double progress = (currentTime - startTime) / (double) duration;
            return (float) progress;
        }
        return state == AnimationState.IDLE ? 0 : 1;
    }

    @Override
    public void update() {
        if (state == AnimationState.RUNNING) {
            long currentTime = System.currentTimeMillis();

            if (delay > 0 && currentTime - delayStartTime < delay) {
                return;
            }

            double progress = (currentTime - startTime) / (double) duration;
            if (progress >= 1) {
                currentValue = targetValue;
                state = AnimationState.COMPLETED;
                if (onCompleteCallback != null) {
                    onCompleteCallback.run();
                }
                running = false;
            } else {
                progress = Math.min(1, Math.max(0, progress));
                currentValue = easingMethod.apply((float) progress) * (targetValue - currentValue) + currentValue;
                if (onUpdateCallback != null) {
                    onUpdateCallback.accept(currentValue);
                }
            }
        }
    }

    @Override
    public IAnimator<Number> reverse() {
        double temp = targetValue;
        targetValue = currentValue;
        currentValue = temp;
        startTime = System.currentTimeMillis();
        state = AnimationState.RUNNING;
        running = true;
        return this;
    }

    @Override
    public void cancel() {
        state = AnimationState.IDLE;
        running = false;
    }

    @Override
    public void reset() {
        state = AnimationState.IDLE;
        running = false;
        currentValue = 0;
    }

    @Override
    public Number value() {
        return currentValue;
    }

    @Override
    public Number targetValue() {
        return targetValue;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean isCompleted() {
        return state == AnimationState.COMPLETED;
    }

    public int intValue() {
        return value().intValue();
    }

    public long longValue() {
        return value().longValue();
    }

    public float floatValue() {
        return value().floatValue();
    }

    public double doubleValue() {
        return value().doubleValue();
    }
}
