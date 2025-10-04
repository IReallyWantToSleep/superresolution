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

package io.homo.superresolution.common.gui.render;

import io.homo.superresolution.core.math.Vector2f;

public class Transform {
    private Vector2f scale;
    private Vector2f translation;
    private float rotation; // 弧度

    public Transform() {
        this.scale = new Vector2f(1.0f, 1.0f);
        this.translation = new Vector2f(0.0f, 0.0f);
        this.rotation = 0.0f;
    }

    public Transform(Vector2f scale, Vector2f translation, float rotation) {
        this.scale = scale;
        this.translation = translation;
        this.rotation = rotation;
    }
    
    public static Transform empty() {
        return new Transform(new Vector2f(1.0f, 1.0f), new Vector2f(0.0f, 0.0f), 0.0f);
    }

    public Vector2f scale() {
        return scale;
    }

    public Transform scale(Vector2f scale) {
        this.scale = scale;
        return this;
    }

    public Vector2f translation() {
        return translation;
    }

    public Transform translation(Vector2f translation) {
        this.translation = translation;
        return this;
    }

    public float rotation() {
        return rotation;
    }

    public Transform rotation(float rotation) {
        this.rotation = rotation;
        return this;
    }

    public Transform rotationDegrees(float angleDegrees) {
        this.rotation = (float) Math.toRadians(angleDegrees);
        return this;
    }

    public float rotationDegrees() {
        return (float) Math.toDegrees(rotation);
    }

    @Override
    public String toString() {
        return String.format("Transform[scale=%s, translation=%s, rotation=%.2f°]",
                scale, translation, rotationDegrees());
    }
}