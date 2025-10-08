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

package io.homo.superresolution.core.gui.core.backends.interfaces;

import java.util.ArrayDeque;
import java.util.Deque;

public class TransformStack {
    private final Deque<Transform> stack = new ArrayDeque<>();

    public TransformStack() {
        stack.addLast(Transform.identity());
    }

    public void push() {
        stack.addLast(last().copy());
    }

    public void pop() {
        if (stack.size() > 1) {
            stack.removeLast();
        }
    }


    public Transform last() {
        return stack.getLast();
    }

    public void translate(float x, float y) {
        last().translate(x, y);
    }

    public void scale(float sx, float sy) {
        last().scale(sx, sy);
    }

    public void scale(float s) {
        scale(s, s);
    }

    public void rotate(float radians) {
        last().rotate(radians);
    }

    public void scaleAt(float sx, float sy, float cx, float cy) {
        last().scaleAt(sx, sy, cx, cy);
    }

    public void rotateAt(float radians, float cx, float cy) {
        last().rotateAt(radians, cx, cy);
    }

    public void setIdentity() {
        last().setIdentity();
    }

    public float[] currentMatrix() {
        return last().transformMatrix();
    }

    public boolean isClear() {
        return stack.size() == 1;
    }

    @Override
    public String toString() {
        return "TransformStack{" +
                "stack=" + stack +
                '}';
    }
}