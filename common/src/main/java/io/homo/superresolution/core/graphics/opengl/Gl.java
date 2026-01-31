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

package io.homo.superresolution.core.graphics.opengl;

import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.core.graphics.GraphicsCapabilities;
import io.homo.superresolution.core.graphics.opengl.dsa.CompatDirectStateAccessImpl;
import io.homo.superresolution.core.graphics.opengl.dsa.GL45OrEXTDirectStateAccessImpl;
import io.homo.superresolution.core.graphics.opengl.dsa.IGlDirectStateAccess;

public class Gl {
    public static final IGlDirectStateAccess DSA;

    static {
        if (!isSupportDSA()) {
            SuperResolution.LOGGER.info("不支持DSA 使用 CompatDirectStateAccessImpl");
            DSA = new CompatDirectStateAccessImpl();
        } else {
            SuperResolution.LOGGER.info("支持DSA 使用 GL45OrEXTDirectStateAccessImpl");
            DSA = new GL45OrEXTDirectStateAccessImpl();
        }
    }

    public static boolean isLegacy() {
        return GraphicsCapabilities.getGLVersion()[0] >= 4 && GraphicsCapabilities.getGLVersion()[1] < 3;
    }

    public static boolean isSupportDSA() {
        return GraphicsCapabilities.getGLVersion()[0] >= 4 && GraphicsCapabilities.getGLVersion()[1] >= 5;
    }
}
