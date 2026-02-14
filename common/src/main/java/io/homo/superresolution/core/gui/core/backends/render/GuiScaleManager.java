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

package io.homo.superresolution.core.gui.core.backends.render;

import io.homo.superresolution.common.minecraft.MinecraftWindow;
import io.homo.superresolution.core.utils.UIScalingCalculator;
import org.joml.Vector2f;

public class GuiScaleManager {

    private static final GuiScaleManager INSTANCE = new GuiScaleManager();

    private float guiScale = 1.0f;
    private float dpiScale = 1.0f;
    private float userScale = 1.0f;
    private boolean autoScale = true;
    private float minScale = 0.5f;
    private float maxScale = 4.0f;

    private GuiScaleManager() {
    }

    public static GuiScaleManager getInstance() {
        return INSTANCE;
    }

    public void update() {
        Vector2f screenSize = MinecraftWindow.getWindowSize();

        if (autoScale) {
            float calculatedScale = (float) UIScalingCalculator.calculateUIScaling(
                    (int) screenSize.x,
                    (int) screenSize.y,
                    1.2f
            );
            this.guiScale = Math.max(calculatedScale, 0.5f) * userScale;
        } else {
            this.guiScale = userScale;
        }
        this.guiScale = Math.max(1.0f, Math.min(maxScale, this.guiScale));
    }

    public float guiScale() {
        return guiScale;
    }

    public void setGuiScale(float scale) {
        this.autoScale = false;
        this.guiScale = Math.max(minScale, Math.min(maxScale, scale));
    }

    public float dpiScale() {
        return dpiScale;
    }

    public void setDpiScale(float dpi) {
        this.dpiScale = dpi;
    }

    public float userScale() {
        return userScale;
    }

    public void setUserScale(float scale) {
        this.userScale = Math.max(0.25f, Math.min(2.0f, scale));
        update();
    }

    public float effectiveScale() {
        return guiScale;
    }

    public boolean isAutoScale() {
        return autoScale;
    }

    public void setAutoScale(boolean auto) {
        this.autoScale = auto;
        if (auto) {
            update();
        }
    }

    public float minScale() {
        return minScale;
    }

    public void setMinScale(float min) {
        this.minScale = min;
    }

    public float maxScale() {
        return maxScale;
    }

    public void setMaxScale(float max) {
        this.maxScale = max;
    }

    public float toPhysical(float logical) {
        return logical * effectiveScale();
    }

    public float toLogical(float physical) {
        return physical / effectiveScale();
    }

    public Vector2f toPhysical(Vector2f logical) {
        return new Vector2f(toPhysical(logical.x), toPhysical(logical.y));
    }

    public Vector2f toLogical(Vector2f physical) {
        return new Vector2f(toLogical(physical.x), toLogical(physical.y));
    }

    public Vector2f logicalScreenSize() {
        Vector2f physical = MinecraftWindow.getWindowSize();
        return new Vector2f(
                physical.x / effectiveScale(),
                physical.y / effectiveScale()
        );
    }

    public Vector2f physicalScreenSize() {
        return MinecraftWindow.getWindowSize();
    }

    public void reset() {
        this.userScale = 1.0f;
        this.dpiScale = 1.0f;
        this.autoScale = true;
        this.minScale = 0.5f;
        this.maxScale = 4.0f;
        update();
    }
}
