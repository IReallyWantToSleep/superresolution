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
 * M3 Expressive linear progress indicator.
 * The track is always flat; only the active segment can be wavy. Determinate value changes
 * are animated with the M3 fastSpatial spring, indeterminate mode uses the AndroidX
 * dual-segment keyframe model.
 */
public class MaterialLinearProgressIndicator extends MaterialWidget<MaterialLinearProgressIndicator> {
    public static final float DEFAULT_TRACK_THICKNESS = 4f;
    public static final float DEFAULT_WAVE_AMPLITUDE = 3f;
    public static final float DEFAULT_WAVELENGTH = 40f;
    public static final float INDETERMINATE_WAVELENGTH = 20f;
    public static final float TRACK_GAP = 4f;
    public static final float STOP_INDICATOR_SIZE = 4f;
    private static final long INDETERMINATE_PERIOD_MS = 1750L;
    private static final float FIRST_HEAD_DURATION_MS = 1000f;
    private static final float FIRST_TAIL_DELAY_MS = 250f;
    private static final float FIRST_TAIL_DURATION_MS = 1000f;
    private static final float SECOND_HEAD_DELAY_MS = 650f;
    private static final float SECOND_HEAD_DURATION_MS = 850f;
    private static final float SECOND_TAIL_DELAY_MS = 900f;
    private static final float SECOND_TAIL_DURATION_MS = 850f;
    private static final float INDETERMINATE_PHASE_OFFSET = 0.40f;
    private static final float WAVE_SAMPLE_LENGTH = 4f;

    protected float beginProgress;
    protected float endProgress;
    protected boolean indeterminate = false;
    protected float trackThickness = DEFAULT_TRACK_THICKNESS;
    protected MaterialProgressShape shape = MaterialProgressShape.FLAT;
    protected float waveAmplitude = DEFAULT_WAVE_AMPLITUDE;
    protected float wavelength = DEFAULT_WAVELENGTH;
    protected float waveSpeed = 0f;
    private final ProgressMotion.Spring beginSpring = new ProgressMotion.Spring();
    private final ProgressMotion.Spring endSpring = new ProgressMotion.Spring();

    public MaterialLinearProgressIndicator setProgress(float begin, float end) {
        this.beginProgress = begin;
        this.endProgress = end;
        return this;
    }

    public float getProgress() {
        return endProgress;
    }

    public MaterialLinearProgressIndicator setProgress(float progress) {
        this.beginProgress = 0f;
        this.endProgress = progress;
        return this;
    }

    public float getBeginProgress() {
        return beginProgress;
    }

    public float getEndProgress() {
        return endProgress;
    }

    public boolean isIndeterminate() {
        return indeterminate;
    }

    public MaterialLinearProgressIndicator setIndeterminate(boolean indeterminate) {
        this.indeterminate = indeterminate;
        return this;
    }

    public float getTrackThickness() {
        return trackThickness;
    }

    public MaterialLinearProgressIndicator setTrackThickness(float trackThickness) {
        this.trackThickness = Math.max(1f, trackThickness);
        return this;
    }

    public MaterialProgressShape getShape() {
        return shape;
    }

    public MaterialLinearProgressIndicator setShape(MaterialProgressShape shape) {
        this.shape = shape == null ? MaterialProgressShape.FLAT : shape;
        return this;
    }

    public float getWaveAmplitude() {
        return waveAmplitude;
    }

    public MaterialLinearProgressIndicator setWaveAmplitude(float waveAmplitude) {
        this.waveAmplitude = Math.max(0f, waveAmplitude);
        return this;
    }

    public float getWavelength() {
        return wavelength;
    }

    public MaterialLinearProgressIndicator setWavelength(float wavelength) {
        this.wavelength = Math.max(4f, wavelength);
        return this;
    }

    /**
     * Custom wave crest speed in px/s along the track. 0 (default) uses the M3 motion
     * token of one wavelength per second.
     */
    public float getWaveSpeed() {
        return waveSpeed;
    }

    public MaterialLinearProgressIndicator setWaveSpeed(float waveSpeed) {
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
        float width = bounds.width;
        float height = bounds.height;
        if (width <= 0f || height <= 0f) {
            return;
        }
        float x = bounds.x;
        float centerY = bounds.y + height / 2f;

        float amplitude = 0f;
        if (shape == MaterialProgressShape.WAVY) {
            amplitude = Math.min(waveAmplitude, Math.max(0f, (height - trackThickness) / 2f));
        }
        float thickness = Math.min(trackThickness, height - 2f * amplitude);
        if (thickness <= 0f) {
            return;
        }

        Color activeColor = isDisabled()
                ? scheme().onSurface().copy().alpha((int) (255 * 0.38f))
                : scheme().primary();
        Color trackColor = isDisabled()
                ? scheme().onSurface().copy().alpha((int) (255 * 0.12f))
                : scheme().secondaryContainer();

        float effectiveGap = TRACK_GAP + thickness / 2f;
        long now = System.currentTimeMillis();

        if (indeterminate) {
            renderIndeterminate(ctx, x, width, centerY, thickness, amplitude, effectiveGap,
                    activeColor, trackColor, now);
            return;
        }

        beginSpring.setTarget(ProgressMotion.clamp01(beginProgress), inputState.frameTime());
        endSpring.setTarget(ProgressMotion.clamp01(endProgress), inputState.frameTime());
        float begin = Math.min(beginSpring.get(), endSpring.get());
        float end = Math.max(beginSpring.get(), endSpring.get());
        renderDeterminate(ctx, x, width, centerY, thickness, amplitude, begin, end, effectiveGap,
                activeColor, trackColor, now);
    }

    private void renderDeterminate(RenderContext ctx, float x, float width, float centerY, float thickness,
                                   float amplitude, float begin, float end, float effectiveGap,
                                   Color activeColor, Color trackColor, long now) {
        float beginX = x + begin * width;
        float endX = x + end * width;
        float activeW = endX - beginX;

        float stopDiameter = Math.min(thickness, STOP_INDICATOR_SIZE);
        float remaining = width - end * width;
        float visibleStop = Math.max(0f, Math.min(stopDiameter, remaining - effectiveGap));
        float stopLeft = Math.max(0f, width - visibleStop);
        float trackStart = activeW > 0f ? Math.min(x + stopLeft, endX + effectiveGap) : x;
        float trackEnd = x + stopLeft - effectiveGap;

        boolean wavy = amplitude > 0f;
        if (wavy) {
            if (activeW > 0f) {
                float phase = wavePhase(now);
                strokeWave(ctx, beginX, endX, centerY, thickness, amplitude, wavelength, phase, 0, activeColor);
            }
        } else if (activeW > 0f) {
            fillRoundedSegment(ctx, beginX, centerY, activeW, thickness, activeColor);
        }

        float leftTrackW = beginX - effectiveGap - x;
        if (leftTrackW > 0f) {
            fillRoundedSegment(ctx, x, centerY, leftTrackW, thickness, trackColor);
        }
        if (trackEnd - trackStart > 0f) {
            fillRoundedSegment(ctx, trackStart, centerY, trackEnd - trackStart, thickness, trackColor);
        }
        if (visibleStop > 0f) {
            ctx.arc(x + width - visibleStop / 2f, centerY, visibleStop / 2f, activeColor, true);
        }
    }

    private void renderIndeterminate(RenderContext ctx, float x, float width, float centerY, float thickness,
                                     float amplitude, float effectiveGap,
                                     Color activeColor, Color trackColor, long now) {
        float t = (now % INDETERMINATE_PERIOD_MS) / (float) INDETERMINATE_PERIOD_MS;
        float shifted = t + INDETERMINATE_PHASE_OFFSET;
        if (shifted >= 1f) {
            shifted -= 1f;
        }

        float firstStart = timedProgress(shifted, FIRST_TAIL_DELAY_MS, FIRST_TAIL_DURATION_MS);
        float firstEnd = timedProgress(shifted, 0f, FIRST_HEAD_DURATION_MS);
        float secondStart = timedProgress(shifted, SECOND_TAIL_DELAY_MS, SECOND_TAIL_DURATION_MS);
        float secondEnd = timedProgress(shifted, SECOND_HEAD_DELAY_MS, SECOND_HEAD_DURATION_MS);
        if (firstEnd <= firstStart && secondEnd > secondStart) {
            firstStart = secondStart;
            firstEnd = secondEnd;
            secondStart = 0f;
            secondEnd = 0f;
        }

        float fs = ProgressMotion.clamp01(firstStart) * width;
        float fe = ProgressMotion.clamp01(firstEnd) * width;
        float ss = ProgressMotion.clamp01(secondStart) * width;
        float se = ProgressMotion.clamp01(secondEnd) * width;
        boolean firstVisible = fe > fs;
        boolean secondVisible = se > ss;

        if (!firstVisible && !secondVisible) {
            fillRoundedSegment(ctx, x, centerY, width, thickness, trackColor);
            return;
        }
        if (!firstVisible) {
            fs = ss;
            fe = se;
            secondVisible = false;
        } else if (secondVisible && ss < fs) {
            float tmpS = fs;
            float tmpE = fe;
            fs = ss;
            fe = se;
            ss = tmpS;
            se = tmpE;
        }
        if (secondVisible && ss <= fe + effectiveGap * 2f) {
            fe = Math.max(fe, se);
            secondVisible = false;
        }

        fillRoundedSegment(ctx, x, centerY, Math.max(0f, fs - effectiveGap), thickness, trackColor);
        if (!secondVisible) {
            float trailingStart = Math.min(width, fe + effectiveGap);
            fillRoundedSegment(ctx, x + trailingStart, centerY, width - trailingStart, thickness, trackColor);
        } else {
            float middleStart = Math.min(width, fe + effectiveGap);
            float middleEnd = Math.max(middleStart, ss - effectiveGap);
            fillRoundedSegment(ctx, x + middleStart, centerY, middleEnd - middleStart, thickness, trackColor);
            float trailingStart = Math.min(width, se + effectiveGap);
            fillRoundedSegment(ctx, x + trailingStart, centerY, width - trailingStart, thickness, trackColor);
        }

        boolean wavy = amplitude > 0f;
        if (wavy) {
            float phase = indeterminateWavePhase(shifted);
            int steps = Math.max(2, (int) Math.ceil(width / WAVE_SAMPLE_LENGTH));
            strokeWave(ctx, x + fs, x + fe, centerY, thickness, amplitude, INDETERMINATE_WAVELENGTH, phase, steps, activeColor);
            if (secondVisible) {
                strokeWave(ctx, x + ss, x + se, centerY, thickness, amplitude, INDETERMINATE_WAVELENGTH, phase, steps, activeColor);
            }
        } else {
            fillRoundedSegment(ctx, x + fs, centerY, fe - fs, thickness, activeColor);
            if (secondVisible) {
                fillRoundedSegment(ctx, x + ss, centerY, se - ss, thickness, activeColor);
            }
        }
    }

    /**
     * AndroidX keyframe evaluation with the emphasized accelerate easing.
     */
    private static float timedProgress(float cycleFraction, float delayMs, float durationMs) {
        float elapsed = ProgressMotion.clamp01(cycleFraction) * INDETERMINATE_PERIOD_MS;
        float f = ProgressMotion.clamp01((elapsed - delayMs) / durationMs);
        return ProgressMotion.EMPHASIZED_ACCELERATE.interpolation(f);
    }

    /**
     * AndroidX advances three wavelengths per indeterminate cycle with emphasized accelerate easing.
     */
    private static float indeterminateWavePhase(float cycleFraction) {
        if (cycleFraction <= 0f || cycleFraction >= 1f) {
            return 0f;
        }
        float phase = ProgressMotion.EMPHASIZED_ACCELERATE.interpolation(cycleFraction) * 3f;
        return phase - (float) Math.floor(phase);
    }

    private float wavePhase(long now) {
        if (waveSpeed > 0f) {
            float cycles = (now / 1000f) * waveSpeed / Math.max(1f, wavelength);
            return cycles - (float) Math.floor(cycles);
        }
        return ProgressMotion.wavePhaseCycles(now);
    }

    private void fillRoundedSegment(RenderContext ctx, float x, float centerY, float width, float thickness, Color color) {
        if (width <= 0f) {
            return;
        }
        float radius = Math.min(thickness / 2f, width / 2f);
        ctx.roundedRect(x, centerY - thickness / 2f, width, thickness, radius, color, true);
    }

    private void strokeWave(RenderContext ctx, float x0, float x1, float centerY, float thickness,
                            float amplitude, float wavelength, float phaseCycles, int fixedSteps, Color color) {
        if (x1 - x0 <= 0f) {
            return;
        }
        float safeWavelength = Math.max(1f, wavelength);
        // Round caps sit inside [x0, x1] so a wave flush with the track edge keeps a rounded end.
        float capInset = Math.min(thickness / 2f, (x1 - x0) / 2f);
        float startX = x0 + capInset;
        float endX = x1 - capInset;
        int steps = fixedSteps > 0
                ? fixedSteps
                : Math.max(2, (int) Math.ceil((endX - startX) / WAVE_SAMPLE_LENGTH));
        ctx.beginPath();
        ctx.strokeWidth(thickness);
        ctx.strokeColor(color);
        float firstX = 0f;
        float firstY = 0f;
        float lastX = 0f;
        float lastY = 0f;
        for (int i = 0; i <= steps; i++) {
            float f = i / (float) steps;
            float px = startX + (endX - startX) * f;
            float py = centerY + amplitude * (float) Math.sin((px / safeWavelength - phaseCycles) * 2.0 * Math.PI);
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
        ctx.arc(firstX, firstY, thickness / 2f, color, true);
        ctx.arc(lastX, lastY, thickness / 2f, color, true);
    }
}
