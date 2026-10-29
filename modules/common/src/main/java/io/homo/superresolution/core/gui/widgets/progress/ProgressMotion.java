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

package io.homo.superresolution.core.gui.widgets.progress;

import io.homo.superresolution.core.gui.core.animator.BezierInterpolator;
import io.homo.superresolution.core.gui.core.animator.TimeInterpolator;

/**
 * Shared M3 Expressive motion tokens for progress indicators.
 * Easing curves follow the AndroidX / M3 motion scheme; the determinate progress
 * spring is critically damped (AndroidX ProgressAnimationSpec) to avoid overshoot.
 */
final class ProgressMotion {
    static final TimeInterpolator STANDARD = new BezierInterpolator(0.2, 0.0, 0.0, 1.0);
    static final TimeInterpolator EMPHASIZED_ACCELERATE = new BezierInterpolator(0.3, 0.0, 0.8, 0.15);
    static final TimeInterpolator EMPHASIZED_DECELERATE = new BezierInterpolator(0.05, 0.7, 0.1, 1.0);

    /**
     * Time for one wave crest to travel a complete wavelength.
     */
    static final long WAVE_PHASE_CYCLE_MS = 1000L;

    private ProgressMotion() {
    }

    static float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }

    /**
     * Continuously advancing wave phase measured in whole wavelengths (one per second).
     */
    static float wavePhaseCycles(long nowMs) {
        return (nowMs % WAVE_PHASE_CYCLE_MS) / (float) WAVE_PHASE_CYCLE_MS;
    }

    /**
     * 1D spring used for determinate progress changes. Critically damped (like the
     * AndroidX ProgressAnimationSpec NoBouncy spring) so the displayed value never
     * overshoots the target.
     */
    static final class Spring {
        private static final float STIFFNESS = 800f;
        private static final float DAMPING_RATIO = 1.0f;
        private static final float SNAP_THRESHOLD = 0.0005f;
        private static final float MAX_FRAME_DT = 0.064f;
        private static final float MAX_SUBSTEP_DT = 0.008f;

        private float value;
        private float velocity;
        private boolean initialized;

        float get() {
            return value;
        }

        void setTarget(float target, float frameTimeMs) {
            if (!initialized) {
                value = target;
                velocity = 0f;
                initialized = true;
                return;
            }
            if (value == target && velocity == 0f) {
                return;
            }
            float dt = frameTimeMs / 1000f;
            if (dt <= 0f) {
                dt = 1f / 60f;
            }
            dt = Math.min(dt, MAX_FRAME_DT);
            float omega = (float) Math.sqrt(STIFFNESS);
            float damping = 2f * DAMPING_RATIO * omega;
            int substeps = Math.max(1, Math.min(8, (int) Math.ceil(dt / MAX_SUBSTEP_DT)));
            float h = dt / substeps;
            float errorBefore = value - target;
            for (int i = 0; i < substeps; i++) {
                float accel = -omega * omega * (value - target) - damping * velocity;
                velocity += accel * h;
                value += velocity * h;
            }
            float errorAfter = value - target;
            boolean overshot = (errorBefore < 0f) != (errorAfter < 0f) && errorAfter != 0f;
            if (overshot || (Math.abs(errorAfter) < SNAP_THRESHOLD && Math.abs(velocity) < SNAP_THRESHOLD)) {
                value = target;
                velocity = 0f;
            }
        }
    }
}
