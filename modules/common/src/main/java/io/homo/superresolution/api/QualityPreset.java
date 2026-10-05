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

package io.homo.superresolution.api;

import net.minecraft.network.chat.Component;

public class QualityPreset {
    private float upscaleRatio = 1.0f;
    private Component name;
    private String codeName;

    public float getUpscaleRatio() {
        return upscaleRatio;
    }

    public QualityPreset setUpscaleRatio(float upscaleRatio) {
        this.upscaleRatio = upscaleRatio;
        return this;
    }

    public Component getName() {
        return name;
    }

    public QualityPreset setName(Component name) {
        this.name = name;
        return this;
    }

    public String getCodeName() {
        return codeName;
    }

    public QualityPreset setCodeName(String codeName) {
        this.codeName = codeName;
        return this;
    }
}
