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

package io.homo.superresolution.core.gui.core.animator;

import java.util.ArrayList;
import java.util.List;

public abstract class Animator<T extends Animator<T, V>, V> {
    protected final List<AnimatorUpdateListener<V>> updateListeners = new ArrayList<>();
    protected final List<AnimatorLifecycleListener> lifecycleListeners = new ArrayList<>();
    protected State state = State.IDLE;
    protected long durationMs = 1;
    protected long startDelayMs = 0;
    protected TimeInterpolator timeInterpolator = progress -> progress;
    protected ValueInterpolator<V> valueInterpolator;
    protected long startTimeMs = 0;
    protected long pausedElapsedMs = 0;
    protected V startValue;
    protected V targetValue;
    protected V currentValue;
    protected double progress;

    public static FloatAnimator ofFloat(Float from, Float to) {
        return new FloatAnimator(from, to);
    }

    public static void updateAll(Animator<?, ?>... animators) {
        if (animators == null) {
            return;
        }
        for (Animator<?, ?> animator : animators) {
            animator.update();
        }
    }

    @SuppressWarnings("unchecked")
    public T start() {
        if (state == State.RUNNING || state == State.PAUSED) {
            cancel();
        }

        state = State.RUNNING;
        startTimeMs = System.currentTimeMillis();
        pausedElapsedMs = 0;

        if (currentValue != null) {
            startValue = currentValue;
        }

        notifyStart();
        onAnimationStart();

        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T cancel() {
        if (state == State.IDLE || state == State.CANCELLED) {
            return (T) this;
        }

        state = State.CANCELLED;
        notifyCancel();
        onAnimationCancel();

        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T end() {
        if (targetValue != null) {
            currentValue = targetValue;
            notifyUpdate(currentValue);
        }

        state = State.ENDED;
        notifyEnd();
        onAnimationEnd();

        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T pause() {
        if (state != State.RUNNING) {
            return (T) this;
        }

        pausedElapsedMs = System.currentTimeMillis() - startTimeMs;
        state = State.PAUSED;
        notifyPause();
        onAnimationPause();

        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T resume() {
        if (state != State.PAUSED) {
            return (T) this;
        }

        startTimeMs = System.currentTimeMillis() - pausedElapsedMs;
        state = State.RUNNING;
        notifyResume();
        onAnimationResume();

        return (T) this;
    }

    public TimeInterpolator timeInterpolator() {
        return timeInterpolator;
    }

    public ValueInterpolator<V> valueInterpolator() {
        return valueInterpolator;
    }

    @SuppressWarnings("unchecked")
    public T timeInterpolator(TimeInterpolator timeInterpolator) {
        if (timeInterpolator == null) {
            throw new IllegalArgumentException("TimeInterpolator cannot be null");
        }
        this.timeInterpolator = timeInterpolator;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T valueInterpolator(ValueInterpolator<V> valueInterpolator) {
        this.valueInterpolator = valueInterpolator;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T duration(long durationMs) {
        if (durationMs <= 0) {
            throw new IllegalArgumentException("Duration must be positive");
        }
        this.durationMs = durationMs;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T startDelay(long startDelayMs) {
        if (startDelayMs < 0) {
            throw new IllegalArgumentException("Start delay cannot be negative");
        }
        this.startDelayMs = startDelayMs;
        return (T) this;
    }

    public long duration() {
        return durationMs;
    }

    public long startDelay() {
        return startDelayMs;
    }

    public boolean isPaused() {
        return state == State.PAUSED;
    }

    public boolean isRunning() {
        return state == State.RUNNING;
    }

    public boolean isStarted() {
        return state == State.RUNNING || state == State.PAUSED;
    }

    @SuppressWarnings("unchecked")
    public T set(V value) {
        this.currentValue = value;
        notifyUpdate(value);
        return (T) this;
    }

    public V get() {
        return currentValue;
    }

    @SuppressWarnings("unchecked")
    public T onUpdate(AnimatorUpdateListener<V> listener) {
        if (listener != null && !updateListeners.contains(listener)) {
            updateListeners.add(listener);
        }
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T removeUpdateListener(AnimatorUpdateListener<V> listener) {
        updateListeners.remove(listener);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T onLifecycle(AnimatorLifecycleListener listener) {
        if (listener != null && !lifecycleListeners.contains(listener)) {
            lifecycleListeners.add(listener);
        }
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T removeLifecycleListener(AnimatorLifecycleListener listener) {
        lifecycleListeners.remove(listener);
        return (T) this;
    }

    protected void notifyUpdate(V value) {
        for (AnimatorUpdateListener<V> listener : new ArrayList<>(updateListeners)) {
            listener.onUpdate(value);
        }
    }

    protected void notifyStart() {
        for (AnimatorLifecycleListener listener : new ArrayList<>(lifecycleListeners)) {
            listener.onStart();
        }
    }

    protected void notifyCancel() {
        for (AnimatorLifecycleListener listener : new ArrayList<>(lifecycleListeners)) {
            listener.onCancel();
        }
    }

    protected void notifyEnd() {
        for (AnimatorLifecycleListener listener : new ArrayList<>(lifecycleListeners)) {
            listener.onEnd();
        }
    }

    protected void notifyPause() {
        for (AnimatorLifecycleListener listener : new ArrayList<>(lifecycleListeners)) {
            listener.onPause();
        }
    }

    protected void notifyResume() {
        for (AnimatorLifecycleListener listener : new ArrayList<>(lifecycleListeners)) {
            listener.onResume();
        }
    }

    protected void onAnimationStart() {
    }

    protected void onAnimationCancel() {
    }

    protected void onAnimationEnd() {
    }

    protected void onAnimationPause() {
    }

    protected void onAnimationResume() {
    }

    public boolean update() {
        if (state != State.RUNNING) {
            return false;
        }

        progress = calculateProgress();
        float easedProgress = timeInterpolator.interpolation((float) progress);

        if (valueInterpolator != null && startValue != null && targetValue != null) {
            currentValue = valueInterpolator.interpolation(easedProgress, startValue, targetValue);
            notifyUpdate(currentValue);
        }

        if (progress >= 1.0f) {
            if (targetValue != null) {
                currentValue = targetValue;
                notifyUpdate(currentValue);
            }
            state = State.ENDED;
            notifyEnd();
            onAnimationEnd();
            return false;
        }

        return true;
    }

    @SuppressWarnings("unchecked")
    public T fromTo(V from, V to) {
        this.startValue = from;
        this.targetValue = to;
        this.currentValue = from;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T to(V to) {
        this.targetValue = to;
        if (currentValue == null) {
            currentValue = to;
        }
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T from(V from) {
        this.startValue = from;
        if (currentValue == null) {
            currentValue = from;
        }
        return (T) this;
    }

    public V startValue() {
        return startValue;
    }

    public V targetValue() {
        return targetValue;
    }

    protected float calculateProgress() {
        if (state != State.RUNNING) {
            return 0f;
        }

        long elapsed = System.currentTimeMillis() - startTimeMs;

        if (elapsed < startDelayMs) {
            return 0f;
        }

        elapsed -= startDelayMs;

        float progress = Math.min(1f, (float) elapsed / durationMs);

        return progress;
    }

    public float progress() {
        return calculateProgress();
    }

    protected enum State {
        IDLE,
        RUNNING,
        PAUSED,
        CANCELLED,
        ENDED
    }

    @FunctionalInterface
    public interface AnimatorUpdateListener<V> {
        void onUpdate(V value);
    }

    public interface AnimatorLifecycleListener {
        default void onStart() {
        }

        default void onCancel() {
        }

        default void onEnd() {
        }

        default void onPause() {
        }

        default void onResume() {
        }
    }

    public static class FloatAnimator extends Animator<FloatAnimator, Float> {
        public FloatAnimator() {
            this.valueInterpolator = (progress, start, target) ->
                    start + (target - start) * progress;
        }

        public FloatAnimator(Float from, Float to) {
            this();
            fromTo(from, to);
        }
    }
}