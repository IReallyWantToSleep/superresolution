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

package io.homo.superresolution.common.gui;

#if MC_VER < MC_1_21_4
import java.util.List;
#endif


public class ScissorsHandler {
    #if MC_VER < MC_1_21_4
    public static void clearScissors() {
        me.shedaniel.clothconfig2.api.ScissorsHandler.INSTANCE.clearScissors();
    }

    public static List<Rectangle> getScissorsAreas() {
        //懒得实现了，留个坑
        return List.of();
    }

    public static void scissor(Rectangle rectangle) {
        me.shedaniel.clothconfig2.api.ScissorsHandler.INSTANCE.scissor(new me.shedaniel.math.Rectangle(rectangle.x, rectangle.y, rectangle.width, rectangle.height));

    }

    public static void removeLastScissor() {
        me.shedaniel.clothconfig2.api.ScissorsHandler.INSTANCE.removeLastScissor();

    }

    public static void applyScissors() {
        me.shedaniel.clothconfig2.api.ScissorsHandler.INSTANCE.applyScissors();
    }
    #endif
}
