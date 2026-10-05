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

import io.homo.superresolution.core.gui.core.UIInputState;
import io.homo.superresolution.core.gui.core.backends.render.RenderContext;
import io.homo.superresolution.core.gui.core.impl.Rectangle;
import io.homo.superresolution.core.gui.widgets.MaterialWidget;
import io.homo.superresolution.core.utils.Color;

/**
 * M3 Expressive circular progress indicator.
 * Positions around the ring are expressed as clockwise fractions starting at 12 o'clock.
 * The track keeps a gap on both sides of the active arc for flat and wavy shapes alike;
 * wavy mode waves the track with the same phase and wave count as the active arc so the
 * whole ring reads as one continuous garland. Determinate value changes are animated with
 * the M3 fastSpatial spring, indeterminate mode uses the AndroidX rotation model
 * (1080 deg per 6 s cycle plus four 90 deg pulses).
 */
public class MaterialCircularProgressIndicator extends MaterialWidget<MaterialCircularProgressIndicator> {
    public static final float SIZE_FLAT_DEFAULT = 40f;
    public static final float SIZE_FLAT_THICK = 44f;
    public static final float SIZE_WAVY_DEFAULT = 48f;
    public static final float SIZE_WAVY_THICK = 52f;
    public static final float DEFAULT_TRACK_THICKNESS = 4f;
    public static final float DEFAULT_WAVE_AMPLITUDE = 1.6f;
    public static final float DEFAULT_WAVELENGTH = 15f;
    public static final float TRACK_GAP = 4f;
    private static final long INDETERMINATE_PERIOD_MS = 6000L;
    private static final float GLOBAL_ROTATION_DEGREES = 1080f;
    private static final float ADDITIONAL_ROTATION_DELAY_MS = 1500f;
    private static final float ADDITIONAL_ROTATION_DURATION_MS = 300f;
    private static final float ADDITIONAL_ROTATION_DEGREES = 90f;
    private static final float MIN_SWEEP_FRACTION = 0.10f;
    private static final float MAX_SWEEP_FRACTION = 0.87f;
    private static final float MAX_GAP_FRACTION = 0.20f;
    private static final int SAMPLE_STEPS = 72;
    private static final int MIN_WAVE_COUNT = 5;
    private static final int MAX_WAVE_COUNT = SAMPLE_STEPS / 4;
    private static final float START_ANGLE = (float) (-Math.PI / 2.0);
    private static final float TWO_PI = (float) (2.0 * Math.PI);

    protected float progress;
    protected boolean indeterminate = false;
    protected float trackThickness = DEFAULT_TRACK_THICKNESS;
    protected MaterialProgressShape shape = MaterialProgressShape.FLAT;
    protected float waveAmplitude = DEFAULT_WAVE_AMPLITUDE;
    protected float wavelength = DEFAULT_WAVELENGTH;
    protected float waveSpeed = 0f;
    private final ProgressMotion.Spring progressSpring = new ProgressMotion.Spring();

    public float getProgress() {
        return progress;
    }

    public MaterialCircularProgressIndicator setProgress(float progress) {
        this.progress = progress;
        return this;
    }

    public boolean isIndeterminate() {
        return indeterminate;
    }

    public MaterialCircularProgressIndicator setIndeterminate(boolean indeterminate) {
        this.indeterminate = indeterminate;
        return this;
    }

    public float getTrackThickness() {
        return trackThickness;
    }

    public MaterialCircularProgressIndicator setTrackThickness(float trackThickness) {
        this.trackThickness = Math.max(1f, trackThickness);
        return this;
    }

    public MaterialProgressShape getShape() {
        return shape;
    }

    public MaterialCircularProgressIndicator setShape(MaterialProgressShape shape) {
        this.shape = shape == null ? MaterialProgressShape.FLAT : shape;
        return this;
    }

    public float getWaveAmplitude() {
        return waveAmplitude;
    }

    public MaterialCircularProgressIndicator setWaveAmplitude(float waveAmplitude) {
        this.waveAmplitude = Math.max(0f, waveAmplitude);
        return this;
    }

    public float getWavelength() {
        return wavelength;
    }

    public MaterialCircularProgressIndicator setWavelength(float wavelength) {
        this.wavelength = Math.max(4f, wavelength);
        return this;
    }

    /**
     * Custom wave crest speed in px/s along the ring. 0 (default) uses the M3 motion
     * token of one wavelength per second.
     */
    public float getWaveSpeed() {
        return waveSpeed;
    }

    public MaterialCircularProgressIndicator setWaveSpeed(float waveSpeed) {
        this.waveSpeed = Math.max(0f, waveSpeed);
        return this;
    }

    @Override
    protected void init() {
    }

    @Override
    public void layouting(RenderContext ctx) {
    }

    @Override
    public void render(RenderContext ctx, UIInputState inputState) {
        Rectangle bounds = getBounds();
        float diameter = Math.min(bounds.width, bounds.height);
        if (diameter <= 0f) {
            return;
        }
        float cx = bounds.x + bounds.width / 2f;
        float cy = bounds.y + bounds.height / 2f;

        float amplitude = 0f;
        if (shape == MaterialProgressShape.WAVY) {
            amplitude = Math.min(waveAmplitude, Math.max(0f, (diameter - trackThickness) / 2f));
        }
        float thickness = Math.min(trackThickness, diameter - 2f * amplitude);
        float radius = (diameter - thickness) / 2f - amplitude;
        if (thickness <= 0f || radius <= 0f) {
            return;
        }

        Color activeColor = isDisabled()
                ? scheme().onSurface().copy().alpha((int) (255 * 0.38f))
                : scheme().primary();
        Color trackColor = isDisabled()
                ? scheme().onSurface().copy().alpha((int) (255 * 0.12f))
                : scheme().secondaryContainer();

        float circumference = TWO_PI * radius;
        float gapFraction = Math.min(MAX_GAP_FRACTION, (TRACK_GAP + thickness) / circumference);
        int waveCount = Math.max(MIN_WAVE_COUNT,
                Math.min(MAX_WAVE_COUNT, (int) Math.floor(circumference / Math.max(1f, wavelength))));
        long now = System.currentTimeMillis();

        float activeStart;
        float activeEnd;
        float phase;
        if (indeterminate) {
            float t = (now % INDETERMINATE_PERIOD_MS) / (float) INDETERMINATE_PERIOD_MS;
            float globalRotation = GLOBAL_ROTATION_DEGREES * t;
            float additionalRotation = additionalRotationDegrees(t);
            activeStart = (globalRotation + additionalRotation) / 360f;
            activeEnd = activeStart + indeterminateSweepFraction(t);
            phase = 0f;
        } else {
            progressSpring.setTarget(ProgressMotion.clamp01(progress), inputState.frameTime());
            float displayed = progressSpring.get();
            activeStart = 0f;
            activeEnd = displayed;
            phase = waveSpeed > 0f ? customWavePhase(now) : ProgressMotion.wavePhaseCycles(now);
        }

        boolean wavy = amplitude > 0f;
        // The track keeps a gap on both sides of the active arc (flat and wavy alike).
        // Wavy mode waves the track with the same phase so the ring reads as one garland.
        float trackStart = activeEnd + gapFraction;
        float trackEnd = activeStart + 1f - gapFraction;
        if (trackEnd - trackStart > 0f) {
            if (wavy) {
                strokeWavyArc(ctx, cx, cy, radius, thickness, amplitude, waveCount,
                        trackColor, trackStart, trackEnd, phase, true);
            } else {
                strokeArc(ctx, cx, cy, radius, thickness, trackColor, trackStart, trackEnd, true);
            }
        }

        if (activeEnd - activeStart > 0.0005f) {
            if (wavy) {
                strokeWavyArc(ctx, cx, cy, radius, thickness, amplitude, waveCount,
                        activeColor, activeStart, activeEnd, phase, true);
            } else {
                strokeArc(ctx, cx, cy, radius, thickness, activeColor, activeStart, activeEnd, true);
            }
        }
    }

    /**
     * Four emphasized-decelerate quarter-turn pulses per AndroidX cycle.
     */
    private static float additionalRotationDegrees(float cycleFraction) {
        float elapsed = ProgressMotion.clamp01(cycleFraction) * INDETERMINATE_PERIOD_MS;
        int pulse = Math.min(3, (int) (elapsed / ADDITIONAL_ROTATION_DELAY_MS));
        float pulseStart = pulse * ADDITIONAL_ROTATION_DELAY_MS;
        float local = ProgressMotion.clamp01((elapsed - pulseStart) / ADDITIONAL_ROTATION_DURATION_MS);
        float eased;
        if (local <= 0f) {
            eased = 0f;
        } else if (local >= 1f) {
            eased = 1f;
        } else {
            eased = ProgressMotion.EMPHASIZED_DECELERATE.interpolation(local);
        }
        return (pulse + eased) * ADDITIONAL_ROTATION_DEGREES;
    }

    /**
     * AndroidX sweep: linear growth during the first half of the cycle,
     * standard-easing shrink during the second half.
     */
    private static float indeterminateSweepFraction(float cycleFraction) {
        float t = ProgressMotion.clamp01(cycleFraction);
        if (t < 0.5f) {
            return MIN_SWEEP_FRACTION + (MAX_SWEEP_FRACTION - MIN_SWEEP_FRACTION) * (t * 2f);
        }
        float local = (t - 0.5f) * 2f;
        return MAX_SWEEP_FRACTION
                + (MIN_SWEEP_FRACTION - MAX_SWEEP_FRACTION) * ProgressMotion.STANDARD.interpolation(local);
    }

    private float customWavePhase(long now) {
        float cycles = (now / 1000f) * waveSpeed / Math.max(1f, wavelength);
        return cycles - (float) Math.floor(cycles);
    }

    private void strokeArc(RenderContext ctx, float cx, float cy, float radius, float thickness,
                           Color color, float fromFraction, float toFraction, boolean caps) {
        // The NanoVG arc binding only sweeps counterclockwise (decreasing angles), so flat arcs
        // are sampled exactly like wavy ones with zero amplitude.
        strokeWavyArc(ctx, cx, cy, radius, thickness, 0f, 1, color, fromFraction, toFraction, 0f, caps);
    }

    private void strokeWavyArc(RenderContext ctx, float cx, float cy, float radius, float thickness,
                               float amplitude, int waveCount, Color color,
                               float fromFraction, float toFraction, float phaseCycles, boolean caps) {
        if (toFraction - fromFraction <= 0f) {
            return;
        }
        ctx.beginPath();
        ctx.strokeWidth(thickness);
        ctx.strokeColor(color);
        float firstX = 0f;
        float firstY = 0f;
        float lastX = 0f;
        float lastY = 0f;
        for (int i = 0; i <= SAMPLE_STEPS; i++) {
            float f = fromFraction + (toFraction - fromFraction) * (i / (float) SAMPLE_STEPS);
            float waveRadius = radius + amplitude
                    * (float) Math.sin((f * waveCount - phaseCycles) * 2.0 * Math.PI);
            float angle = START_ANGLE + f * TWO_PI;
            float px = cx + waveRadius * (float) Math.cos(angle);
            float py = cy + waveRadius * (float) Math.sin(angle);
            if (i == 0) {
                ctx.move(px, py);
                firstX = px;
                firstY = py;
            } else {
                ctx.lineTo(px, py);
            }
            lastX = px;
            lastY = py;
        }
        ctx.endPath(false);
        if (caps) {
            ctx.arc(firstX, firstY, thickness / 2f, color, true);
            ctx.arc(lastX, lastY, thickness / 2f, color, true);
        }
    }
}
