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

package io.homo.superresolution.core.gui.core.impl;

import org.joml.Vector2f;

public class Rectangle {
    public float x;
    public float y;
    public float width;
    public float height;

    public Rectangle(Vector2f position, Vector2f size) {
        this(position.x, position.y, size.x, size.y);

    }

    public Rectangle(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public Rectangle() {
        this(0, 0, 0, 0);
    }

    public Rectangle(Rectangle region) {
        this(region.x, region.y, region.width, region.height);
    }

    public Rectangle setX(float x) {
        this.x = x;
        return this;
    }

    public Rectangle setY(float y) {
        this.y = y;
        return this;
    }

    public Rectangle setWidth(float width) {
        this.width = width;
        return this;
    }

    public Rectangle setHeight(float height) {
        this.height = height;
        return this;
    }

    public Vector2f getPosition() {
        return new Vector2f(x, y);
    }

    public Vector2f getSize() {
        return new Vector2f(width, height);
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

    public boolean in(Vector2f point) {
        return point.x >= this.x && point.x < (this.x + this.width) &&
                point.y >= this.y && point.y < (this.y + this.height);
    }

    public boolean intersect(Rectangle rectangle) {
        return this.x < rectangle.x + rectangle.width &&
                this.x + this.width > rectangle.x &&
                this.y < rectangle.y + rectangle.height &&
                this.y + this.height > rectangle.y;
    }

    public Rectangle intersection(Rectangle rectangle) {
        float x1 = Math.max(this.x, rectangle.x);
        float y1 = Math.max(this.y, rectangle.y);
        float x2 = Math.min(this.x + this.width, rectangle.x + rectangle.width);
        float y2 = Math.min(this.y + this.height, rectangle.y + rectangle.height);

        if (x2 > x1 && y2 > y1) {
            return new Rectangle(x1, y1, x2 - x1, y2 - y1);
        }
        return new Rectangle();
    }

    public boolean contains(Rectangle rectangle) {
        return this.x <= rectangle.x &&
                this.y <= rectangle.y &&
                this.x + this.width >= rectangle.x + rectangle.width &&
                this.y + this.height >= rectangle.y + rectangle.height;
    }

    public void grow(float horizontal, float vertical) {
        this.x -= horizontal;
        this.y -= vertical;
        this.width += horizontal * 2;
        this.height += vertical * 2;
    }

    public void translate(float dx, float dy) {
        this.x += dx;
        this.y += dy;
    }

    public void setLocation(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void setSize(float width, float height) {
        this.width = width;
        this.height = height;
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

    public Vector2f getCenter() {
        return new Vector2f(getCenterX(), getCenterY());
    }

    public float getArea() {
        return this.width * this.height;
    }

    public Rectangle clone() {
        return new Rectangle(this.x, this.y, this.width, this.height);
    }

    public boolean isEmpty() {
        return this.width <= 0 || this.height <= 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Rectangle rectangle = (Rectangle) obj;
        return Float.compare(rectangle.x, x) == 0 &&
                Float.compare(rectangle.y, y) == 0 &&
                Float.compare(rectangle.width, width) == 0 &&
                Float.compare(rectangle.height, height) == 0;
    }

    @Override
    public int hashCode() {
        int result = Float.hashCode(x);
        result = 31 * result + Float.hashCode(y);
        result = 31 * result + Float.hashCode(width);
        result = 31 * result + Float.hashCode(height);
        return result;
    }

    @Override
    public String toString() {
        return "Rectangle[x=" + x + ", y=" + y + ", width=" + width + ", height=" + height + "]";
    }
}