/*
 * Super Resolution
 * Copyright (c) 2025. 187J3X1-114514
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


import java.util.function.Consumer;


public interface IAnimator<T> {
    IAnimator<T> animateTo(T target, long duration, boolean reset);

    IAnimator<T> animateTo(T target, long duration);

    IAnimator<T> smoothUpdateTo(T target, long duration);

    IAnimator<T> set(T value);

    IAnimator<T> delay(long delayMs);

    IAnimator<T> ease(Easing.EasingMethod easing);

    IAnimator<T> onComplete(Runnable callback);

    IAnimator<T> onUpdate(Consumer<T> callback);

    float progress();

    void update();

    IAnimator<T> reverse();

    void cancel();

    void reset();

    T value();

    T targetValue();

    boolean isRunning();

    boolean isCompleted();

    enum AnimationState {
        IDLE,
        RUNNING,
        COMPLETED
    }

    interface ValueInterpolator<T> {
        T interpolate(T start, T end, double progress);
    }
}