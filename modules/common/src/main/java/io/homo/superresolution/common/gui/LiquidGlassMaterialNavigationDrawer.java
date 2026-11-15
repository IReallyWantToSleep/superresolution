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

package io.homo.superresolution.common.gui;

import io.homo.superresolution.core.gui.MaterialTheme;
import io.homo.superresolution.core.gui.core.animator.Animator;
import io.homo.superresolution.core.gui.core.animator.TimeInterpolator;
import io.homo.superresolution.core.gui.core.backends.render.LiquidGlassColorControls;
import io.homo.superresolution.core.gui.core.backends.render.LiquidGlassParameters;
import io.homo.superresolution.core.gui.core.backends.render.LiquidGlassTint;
import io.homo.superresolution.core.gui.core.backends.render.RenderContext;
import io.homo.superresolution.core.gui.core.impl.Rectangle;
import io.homo.superresolution.core.gui.widgets.glass.LiquidGlassStyle;
import io.homo.superresolution.core.gui.widgets.navigation.drawer.MaterialNavigationDrawer;
import io.homo.superresolution.core.gui.widgets.navigation.drawer.MaterialNavigationDrawerItem;
import io.homo.superresolution.core.utils.Color;

public class LiquidGlassMaterialNavigationDrawer extends MaterialNavigationDrawer {
    private static final LiquidGlassParameters LIGHT_PARAMETERS =
            new LiquidGlassParameters(45f, 0.8f, 0.65f, 60f, 0f, 5f);
    private static final LiquidGlassParameters DARK_PARAMETERS =
            new LiquidGlassParameters(45f, 0.8f, 0.8f, 75f, 0.46f, 5f);
    private final Animator.FloatAnimator selectionAnimator = Animator.ofFloat(1f, 1f)
            .duration(150)
            .timeInterpolator(TimeInterpolator.easeOutExpo());
    private MaterialNavigationDrawerItem indicatorItem;
    private Rectangle selectionStartBounds;
    private Rectangle selectionBounds;

    @Override
    protected boolean managesItemSelectionBackground() {
        return true;
    }

    @Override
    protected void renderBackground(RenderContext ctx, Rectangle bounds) {
        boolean dark = scheme().theme() == MaterialTheme.Dark;
        LiquidGlassStyle.draw(
                ctx,
                bounds.x,
                bounds.y,
                bounds.width,
                bounds.height,
                16f,
                dark ? DARK_PARAMETERS : LIGHT_PARAMETERS,
                dark ? Color.rgba(28, 27, 32, Math.round(255f * 0.45f))
                        : Color.rgba(255, 255, 255, Math.round(255f * 0.35f))
        );
        drawSelectionBackground(ctx, bounds);
    }

    private void drawSelectionBackground(RenderContext ctx, Rectangle drawerBounds) {
        MaterialNavigationDrawerItem selectedItem = getSelectedItem();
        if (selectedItem == null || !selectedItem.isVisible()) {
            selectionAnimator.cancel();
            indicatorItem = null;
            selectionBounds = null;
            selectionStartBounds = null;
            return;
        }
        Rectangle target = new Rectangle(selectedItem.getBounds());
        if (target.isEmpty()) {
            return;
        }
        target.translate(-drawerBounds.x, -drawerBounds.y);
        if (selectionBounds == null) {
            selectionStartBounds = new Rectangle(target);
            indicatorItem = selectedItem;
            selectionAnimator.fromTo(1f, 1f);
        } else if (indicatorItem != selectedItem) {
            selectionStartBounds = new Rectangle(selectionBounds);
            indicatorItem = selectedItem;
            selectionAnimator.fromTo(0f, 1f).start();
        }
        selectionAnimator.update();
        float progress = selectionAnimator.get();
        selectionBounds = new Rectangle(
                lerp(selectionStartBounds.x, target.x, progress),
                lerp(selectionStartBounds.y, target.y, progress),
                lerp(selectionStartBounds.width, target.width, progress),
                lerp(selectionStartBounds.height, target.height, progress)
        );
        Color tint = scheme().secondaryContainer().copy()
                .alpha(Math.round(255f * (scheme().theme() == MaterialTheme.Dark ? 0.35f : 0.65f)));
        ctx.roundedRectLiquidGlass(
                drawerBounds.x + selectionBounds.x,
                drawerBounds.y + selectionBounds.y,
                selectionBounds.width,
                selectionBounds.height,
                selectionBounds.height / 2f,
                new LiquidGlassParameters(-45f, 0.5f, 0.7f, 70f, 0.73f, 1f),
                LiquidGlassColorControls.DEFAULT,
                LiquidGlassTint.from(tint,0.0f)
        );
    }

    @Override
    public void destroy() {
        selectionAnimator.cancel();
        super.destroy();
    }

    private static float lerp(float start, float end, float progress) {
        return start + (end - start) * progress;
    }
}
