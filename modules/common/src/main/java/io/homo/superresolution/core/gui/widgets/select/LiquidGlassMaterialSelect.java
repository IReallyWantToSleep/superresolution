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

package io.homo.superresolution.core.gui.widgets.select;

import io.homo.superresolution.core.gui.core.animator.Animator;
import io.homo.superresolution.core.gui.core.animator.TimeInterpolator;
import io.homo.superresolution.core.gui.core.backends.render.LiquidGlassColorControls;
import io.homo.superresolution.core.gui.core.backends.render.LiquidGlassParameters;
import io.homo.superresolution.core.gui.core.backends.render.LiquidGlassTint;
import io.homo.superresolution.core.gui.core.backends.render.RenderContext;
import io.homo.superresolution.core.gui.core.impl.Rectangle;
import io.homo.superresolution.core.gui.widgets.glass.LiquidGlassStyle;
import io.homo.superresolution.core.gui.widgets.menu.LiquidGlassMaterialMenu;
import io.homo.superresolution.core.gui.widgets.textfield.MaterialTextField;
import io.homo.superresolution.core.gui.widgets.textfield.MaterialTextFieldSize;
import io.homo.superresolution.core.utils.Color;

public class LiquidGlassMaterialSelect<T> extends MaterialSelect<T> {
    public LiquidGlassMaterialSelect() {
        super(LiquidGlassSelectField::new, LiquidGlassMaterialMenu::create);
    }

    public static <T> LiquidGlassMaterialSelect<T> create() {
        return new LiquidGlassMaterialSelect<>();
    }

    private static final class LiquidGlassSelectField extends MaterialTextField {
        private final Animator.FloatAnimator cornerAnimator = Animator.ofFloat(style().size().cornerRadius()*2, style().size().cornerRadius() * 4)
                .duration(160)
                .timeInterpolator(TimeInterpolator.easeOutQuint());

        @Override
        public MaterialTextField setFocused(boolean focused) {
            super.setFocused(focused);
            if (focused) {
                cornerAnimator.to(style().size().cornerRadius() * 4).start();
            } else {
                cornerAnimator.to(style().size().cornerRadius()*2).start();
            }
            return this;
        }

        @Override
        protected void drawContainer(RenderContext ctx, Rectangle bounds, MaterialTextFieldSize size,
                                     TextFieldColors colors, float focusProgress, float labelProgress) {
            cornerAnimator.update();
            if (isDisabled() || hasError()) {
                super.drawContainer(ctx, bounds, size, colors, focusProgress, labelProgress);
                return;
            }
            Color tint = Color.lerp(
                    scheme().surfaceContainerHighest().copy().alpha(Math.round(255f * 0.18f)),
                    scheme().primary().copy().alpha(Math.round(255f * 0.14f)),
                    focusProgress
            );
            ctx.roundedRectLiquidGlass(
                    bounds.x,
                    bounds.y,
                    bounds.width,
                    bounds.height,
                    cornerAnimator.get(),
                    new LiquidGlassParameters(-45f, 0.5f, 0.7f, 70f, 0.73f, 1f),
                    new LiquidGlassColorControls(-0.007f + (0.06f * focusProgress), 1.04f, 1.04f),
                    LiquidGlassTint.DEFAULT
            );
        }

        @Override
        protected float floatingLabelOffset(MaterialTextFieldSize size) {
            if (isDisabled() || hasError()) {
                return super.floatingLabelOffset(size);
            }
            return size.labelFontSize() / 2f + 2f;
        }
    }
}
