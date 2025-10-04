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

package io.homo.superresolution.core.gui.core.impl;

import io.homo.superresolution.core.math.Vector2f;

public class Rectangle {
    public float x;
    public float y;
    public float width;
    public float height;

    public Rectangle(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public Rectangle() {
        this(0, 0, 0, 0);
    }

    public Vector2f getPosition() {
        return new Vector2f(x, y);
    }

    public void setBounds(float x, float y, float width, float height) {
        this.reshape(x, y, width, height);
    }

    public void setBounds(Rectangle rectangle) {
        this.reshape(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
    }

    public void reshape(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public boolean in(int x, int y) {
        return x >= this.x && x < (this.x + this.width) && y >= this.y && y < (this.y + this.height);
    }

    public boolean in(double x, double y) {
        return x >= this.x && x < (this.x + this.width) && y >= this.y && y < (this.y + this.height);
    }

    public float getLimitX() {
        return this.x + this.width;
    }

    public float getLimitY() {
        return this.y + this.height;
    }

    public float getCenterX() {
        return this.x + this.width / 2;
    }

    public float getCenterY() {
        return this.y + this.height / 2;
    }

    public Rectangle clone() {
        return new Rectangle(this.x, this.y, this.width, this.height);
    }

    public boolean isEmpty() {
        return this.width <= 0 || this.height <= 0;
    }
}
