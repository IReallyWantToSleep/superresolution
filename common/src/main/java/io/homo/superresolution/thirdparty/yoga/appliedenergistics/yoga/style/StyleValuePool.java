/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.style;

import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.numeric.FloatOptional;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.style.SmallValueBuffer;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.style.StyleLength;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.style.StyleSizeLength;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.style.StyleValueHandle;

/**
 * StyleValuePool allows compact storage for a sparse collection of assigned
 * lengths and numbers. Values are referred to using StyleValueHandle. In most
 * cases StyleValueHandle can embed the value directly, but if not, the value is
 * stored within a buffer provided by the pool. The pool contains a fixed number
 * of inline slots before falling back to heap allocating additional slots.
 */
public class StyleValuePool {

    private final SmallValueBuffer buffer;

    public StyleValuePool() {
        this.buffer = new SmallValueBuffer();
    }

    public void store(StyleValueHandle handle, io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.style.StyleLength length) {
        if (length.isUndefined()) {
            handle.setType(StyleValueHandle.TYPE_UNDEFINED);
        } else if (length.isAuto()) {
            handle.setType(StyleValueHandle.TYPE_AUTO);
        } else {
            var type = length.isPoints()
                    ? StyleValueHandle.TYPE_POINT
                    : StyleValueHandle.TYPE_PERCENT;
            storeValue(handle, length.value().unwrap(), type);
        }
    }

    public void store(StyleValueHandle handle, StyleSizeLength sizeValue) {
        if (sizeValue.isUndefined()) {
            handle.setType(StyleValueHandle.TYPE_UNDEFINED);
        } else if (sizeValue.isAuto()) {
            handle.setType(StyleValueHandle.TYPE_AUTO);
        } else if (sizeValue.isMaxContent()) {
            storeKeyword(handle, StyleValueHandle.Keyword.MAX_CONTENT);
        } else if (sizeValue.isStretch()) {
            storeKeyword(handle, StyleValueHandle.Keyword.STRETCH);
        } else if (sizeValue.isFitContent()) {
            storeKeyword(handle, StyleValueHandle.Keyword.FIT_CONTENT);
        } else {
            var type = sizeValue.isPoints()
                    ? StyleValueHandle.TYPE_POINT
                    : StyleValueHandle.TYPE_PERCENT;
            storeValue(handle, sizeValue.value().unwrap(), type);
        }
    }

    public void store(StyleValueHandle handle, FloatOptional number) {
        if (number.isUndefined()) {
            handle.setType(StyleValueHandle.TYPE_UNDEFINED);
        } else {
            storeValue(handle, number.unwrap(), StyleValueHandle.TYPE_NUMBER);
        }
    }

    public io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.style.StyleLength getLength(StyleValueHandle handle) {
        if (handle.isUndefined()) {
            return io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.style.StyleLength.undefined();
        } else if (handle.isAuto()) {
            return io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.style.StyleLength.ofAuto();
        } else {
            assert handle.type() == StyleValueHandle.TYPE_POINT ||
                    handle.type() == StyleValueHandle.TYPE_PERCENT;

            float value = handle.isValueIndexed()
                    ? Float.intBitsToFloat(buffer.get32(handle.value()))
                    : unpackInlineInteger(handle.value());

            return handle.type() == StyleValueHandle.TYPE_POINT
                    ? io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.style.StyleLength.points(value)
                    : StyleLength.percent(value);
        }
    }

    public StyleSizeLength getSize(StyleValueHandle handle) {
        if (handle.isUndefined()) {
            return StyleSizeLength.undefined();
        } else if (handle.isAuto()) {
            return StyleSizeLength.ofAuto();
        } else if (handle.isKeyword(StyleValueHandle.Keyword.MAX_CONTENT)) {
            return StyleSizeLength.ofMaxContent();
        } else if (handle.isKeyword(StyleValueHandle.Keyword.FIT_CONTENT)) {
            return StyleSizeLength.ofFitContent();
        } else if (handle.isKeyword(StyleValueHandle.Keyword.STRETCH)) {
            return StyleSizeLength.ofStretch();
        } else {
            assert handle.type() == StyleValueHandle.TYPE_POINT ||
                    handle.type() == StyleValueHandle.TYPE_PERCENT;

            float value = handle.isValueIndexed()
                    ? Float.intBitsToFloat(buffer.get32(handle.value()))
                    : unpackInlineInteger(handle.value());

            return handle.type() == StyleValueHandle.TYPE_POINT
                    ? StyleSizeLength.points(value)
                    : StyleSizeLength.percent(value);
        }
    }

    public FloatOptional getNumber(StyleValueHandle handle) {
        if (handle.isUndefined()) {
            return FloatOptional.of();
        } else {
            assert handle.type() == StyleValueHandle.TYPE_NUMBER;

            float value = handle.isValueIndexed()
                    ? Float.intBitsToFloat(buffer.get32(handle.value()))
                    : unpackInlineInteger(handle.value());

            return FloatOptional.of(value);
        }
    }

    private void storeValue(
            StyleValueHandle handle,
            float value,
            byte type) {
        handle.setType(type);

        if (handle.isValueIndexed()) {
            short newIndex = buffer.replace(handle.value(), Float.floatToRawIntBits(value));
            handle.setValue(newIndex);
        } else if (isIntegerPackable(value)) {
            handle.setValue(packInlineInteger(value));
        } else {
            short newIndex = buffer.push(Float.floatToRawIntBits(value));
            handle.setValue(newIndex);
            handle.setValueIsIndexed();
        }
    }

    private void storeKeyword(
            StyleValueHandle handle,
            StyleValueHandle.Keyword keyword) {
        handle.setType(StyleValueHandle.TYPE_KEYWORD);

        if (handle.isValueIndexed()) {
            short newIndex = buffer.replace(handle.value(), keyword.ordinal());
            handle.setValue(newIndex);
        } else {
            handle.setValue((short) keyword.ordinal());
        }
    }

    private static boolean isIntegerPackable(float f) {
        final short MAX_INLINE_ABS_VALUE = (1 << 11) - 1;

        int i = (int) f;
        return (float) i == f && i >= -MAX_INLINE_ABS_VALUE && i <= +MAX_INLINE_ABS_VALUE;
    }

    private static short packInlineInteger(float value) {
        short isNegative = value < 0 ? (short) 1 : (short) 0;
        return (short) ((isNegative << 11) |
                ((int) value * (isNegative != 0 ? -1 : 1)));
    }

    private static float unpackInlineInteger(short value) {
        final short VALUE_SIGN_MASK = 0b0000_1000_0000_0000;
        final short VALUE_MAGNITUDE_MASK = 0b0000_0111_1111_1111;

        final boolean isNegative = (value & VALUE_SIGN_MASK) != 0;
        return (value & VALUE_MAGNITUDE_MASK) * (isNegative ? -1 : 1);
    }
}
