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

package io.homo.superresolution.core.graphics.impl.pipeline.state;

public class ColorBlendAttachment {
    private final boolean blendEnable;
    private final BlendFactor srcColorBlendFactor;
    private final BlendFactor dstColorBlendFactor;
    private final BlendOp colorBlendOp;
    private final BlendFactor srcAlphaBlendFactor;
    private final BlendFactor dstAlphaBlendFactor;
    private final BlendOp alphaBlendOp;
    private final int colorWriteMask;

    public ColorBlendAttachment(boolean blendEnable,
                                BlendFactor srcColorBlendFactor, BlendFactor dstColorBlendFactor, BlendOp colorBlendOp,
                                BlendFactor srcAlphaBlendFactor, BlendFactor dstAlphaBlendFactor, BlendOp alphaBlendOp,
                                int colorWriteMask) {
        this.blendEnable = blendEnable;
        this.srcColorBlendFactor = srcColorBlendFactor;
        this.dstColorBlendFactor = dstColorBlendFactor;
        this.colorBlendOp = colorBlendOp;
        this.srcAlphaBlendFactor = srcAlphaBlendFactor;
        this.dstAlphaBlendFactor = dstAlphaBlendFactor;
        this.alphaBlendOp = alphaBlendOp;
        this.colorWriteMask = colorWriteMask;
    }

    public ColorBlendAttachment(boolean blendEnable) {
        this(blendEnable, BlendFactor.One, BlendFactor.Zero, BlendOp.Add,
                BlendFactor.One, BlendFactor.Zero, BlendOp.Add, ColorComponentFlags.ALL);
    }

    public static ColorBlendAttachment noBlend() {
        return new ColorBlendAttachment(false);
    }

    public static ColorBlendAttachment alphaBlend() {
        return new ColorBlendAttachment(
                true,
                BlendFactor.SrcAlpha, BlendFactor.OneMinusSrcAlpha, BlendOp.Add,
                BlendFactor.One, BlendFactor.Zero, BlendOp.Add,
                ColorComponentFlags.ALL
        );
    }

    public static ColorBlendAttachment additive() {
        return new ColorBlendAttachment(
                true,
                BlendFactor.One, BlendFactor.One, BlendOp.Add,
                BlendFactor.One, BlendFactor.One, BlendOp.Add,
                ColorComponentFlags.ALL
        );
    }

    public boolean blendEnable() {
        return blendEnable;
    }

    public BlendFactor srcColorBlendFactor() {
        return srcColorBlendFactor;
    }

    public BlendFactor dstColorBlendFactor() {
        return dstColorBlendFactor;
    }

    public BlendOp colorBlendOp() {
        return colorBlendOp;
    }

    public BlendFactor srcAlphaBlendFactor() {
        return srcAlphaBlendFactor;
    }

    public BlendFactor dstAlphaBlendFactor() {
        return dstAlphaBlendFactor;
    }

    public BlendOp alphaBlendOp() {
        return alphaBlendOp;
    }

    public int colorWriteMask() {
        return colorWriteMask;
    }
}
