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

package io.homo.superresolution.core.gui.widgets.sliders;

import io.homo.superresolution.core.gui.core.animator.Animator;

public class MaterialSliderAnimationSet {
    public Animator.FloatAnimator hover;
    public Animator.FloatAnimator press;
    public Animator.FloatAnimator change;

    public Animator.FloatAnimator handleSize;
    public Animator.FloatAnimator handlePosition;

    public void init() {
        hover = new Animator.FloatAnimator();
        press = new Animator.FloatAnimator();
        handleSize = new Animator.FloatAnimator();
        handlePosition = new Animator.FloatAnimator();
        change = new Animator.FloatAnimator();

        hover.set(0f);
        press.set(0f);
        handleSize.set(0f);
        handlePosition.set(0f);
        change.set(0f);
    }

    public void update() {
        Animator.updateAll(hover, press, handleSize, handlePosition, change);
    }
}
