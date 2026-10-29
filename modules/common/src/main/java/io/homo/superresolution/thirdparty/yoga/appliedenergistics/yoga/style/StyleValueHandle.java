/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.style;

/**
 * StyleValueHandle is a small (16-bit) handle to a length or number in a style.
 * The value may be embedded directly in the handle if simple, or the handle may
 * instead point to an index within a StyleValuePool.
 * <p>
 * To read or write a value from a StyleValueHandle, use
 * `StyleValuePool.store()`, and `StyleValuePool.getLength()`/
 * `StyleValuePool.getNumber()`.
 */
public class StyleValueHandle {

    static final short HANDLE_TYPE_MASK = 0b0000_0000_0000_0111;
    static final short HANDLE_INDEXED_MASK = 0b0000_0000_0000_1000;
    static final short HANDLE_VALUE_MASK = (short) 0b1111_1111_1111_0000;

    static final byte TYPE_UNDEFINED = 0;
    static final byte TYPE_POINT = 1;
    static final byte TYPE_PERCENT = 2;
    static final byte TYPE_NUMBER = 3;
    static final byte TYPE_AUTO = 4;
    static final byte TYPE_KEYWORD = 5;
    private short repr;

    public StyleValueHandle() {
        this.repr = 0;
    }

    public static StyleValueHandle ofAuto() {
        StyleValueHandle handle = new StyleValueHandle();
        handle.setType(TYPE_AUTO);
        return handle;
    }

    public boolean isUndefined() {
        return type() == TYPE_UNDEFINED;
    }

    public boolean isDefined() {
        return !isUndefined();
    }

    public boolean isAuto() {
        return type() == TYPE_AUTO;
    }

    boolean isKeyword(Keyword keyword) {
        return type() == TYPE_KEYWORD && value() == keyword.ordinal();
    }

    byte type() {
        return (byte) (repr & HANDLE_TYPE_MASK);
    }

    void setType(byte handleType) {
        repr &= ~HANDLE_TYPE_MASK;
        repr |= handleType;
    }

    short value() {
        return (short) (repr >> 4);
    }

    void setValue(short value) {
        repr &= ~HANDLE_VALUE_MASK;
        repr |= (short) (value << 4);
    }

    boolean isValueIndexed() {
        return (repr & HANDLE_INDEXED_MASK) != 0;
    }

    void setValueIsIndexed() {
        repr |= HANDLE_INDEXED_MASK;
    }

    // Intentionally leaving out auto as a fast path
    enum Keyword {
        MAX_CONTENT,
        FIT_CONTENT,
        STRETCH
    }
}
