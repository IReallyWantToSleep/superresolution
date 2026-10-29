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

package io.homo.superresolution.common.gui.widgets;

import io.homo.superresolution.common.gui.SponsorService;
import io.homo.superresolution.core.gui.core.backends.interfaces.IPaint;
import io.homo.superresolution.core.gui.widgets.chip.MaterialChip;
import io.homo.superresolution.core.gui.widgets.chip.MaterialChipType;
import io.homo.superresolution.core.gui.core.backends.interfaces.TextAlign;
import io.homo.superresolution.core.gui.core.backends.interfaces.TextAlignType;
import io.homo.superresolution.core.gui.core.backends.render.RenderContext;
import io.homo.superresolution.core.gui.core.impl.Rectangle;
import io.homo.superresolution.core.utils.Color;

public final class SponsorChip extends MaterialChip {
    private static final float RADIUS = 8f;
    private final SponsorService.Sponsor sponsor;

    public SponsorChip(SponsorService.Sponsor sponsor) {
        super(MaterialChipType.Suggestion);
        this.sponsor = sponsor;
        text(sponsor.name());
    }

    @Override
    protected boolean shouldRenderMaterialOverlay() {
        return false;
    }

    @Override
    protected void drawContainerBackground(RenderContext ctx, Rectangle bounds, Color color) {
        if (useOriginalChipColors()) {
            super.drawContainerBackground(ctx, bounds, color);
            return;
        }
        drawPaintOrColor(ctx, bounds, sponsor.backgroundStart(), sponsor.backgroundEnd(), true);
    }

    @Override
    protected void drawText(RenderContext ctx, Rectangle bounds, ChipColors colors, float contentX) {
        if (useOriginalChipColors()) {
            super.drawText(ctx, bounds, colors, contentX);
            return;
        }
        float maxWidth = Math.max(0f, bounds.x + bounds.width - getTrailingContentEndForSponsor() - contentX);
        if (sameColor(sponsor.nameStart(), sponsor.nameEnd())) {
            ctx.drawAlignedText(ctx.font(), 14f, sponsor.name(), contentX, bounds.getCenterY(), maxWidth, 20f, 500f,
                    sponsor.nameStart(), TextAlign.of(TextAlignType.ALIGN_LEFT, TextAlignType.ALIGN_MIDDLE), false);
            return;
        }
        IPaint paint = ctx.linearGradient(contentX, bounds.y, contentX + maxWidth, bounds.y,
                sponsor.nameStart(), sponsor.nameEnd());
        ctx.drawAlignedText(ctx.font(), 14f, sponsor.name(), contentX, bounds.getCenterY(), maxWidth, 20f, 500f,
                paint, TextAlign.of(TextAlignType.ALIGN_LEFT, TextAlignType.ALIGN_MIDDLE), false);
    }

    private void drawPaintOrColor(RenderContext ctx, Rectangle bounds, Color start, Color end, boolean fill) {
        if (sameColor(start, end)) {
            ctx.roundedRect(bounds.x, bounds.y, bounds.width, bounds.height, RADIUS, start, fill);
            return;
        }
        IPaint paint = ctx.linearGradient(bounds.x, bounds.y, bounds.x + bounds.width, bounds.y, start, end);
        ctx.beginPath();
        ctx.paint(paint);
        ctx.roundedRect(bounds.x, bounds.y, bounds.width, bounds.height, RADIUS);
        ctx.endPath(fill);
    }

    private static boolean sameColor(Color first, Color second) {
        return first.red() == second.red() && first.green() == second.green()
                && first.blue() == second.blue() && first.alpha() == second.alpha();
    }

    private boolean useOriginalChipColors() {
        return isTransparent(sponsor.nameStart())
                && isTransparent(sponsor.nameEnd())
                && isTransparent(sponsor.backgroundStart())
                && isTransparent(sponsor.backgroundEnd());
    }

    private static boolean isTransparent(Color color) {
        return color.red() == 0 && color.green() == 0 && color.blue() == 0 && color.alpha() == 0;
    }

    private float getTrailingContentEndForSponsor() {
        return 8f;
    }

    @Override
    protected boolean isInteractive() {
        return false;
    }
}
