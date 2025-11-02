/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.style;

import java.util.BitSet;

/**
 * Container which allows storing 32 or 64 bit integer values, whose index may
 * never change. Values are first stored in a fixed buffer of `bufferSize`
 * 32-bit chunks, before falling back to heap allocation.
 */

/**
 * Container which allows storing 32 or 64 bit integer values, whose index may
 * never change.
 */
public class SmallValueBuffer {

    private int count;
    private int[] buffer;
    private final BitSet wideElements;
    private static final int INITIAL_CAPACITY = 16;

    public SmallValueBuffer() {
        this.count = 0;
        this.buffer = new int[INITIAL_CAPACITY];
        this.wideElements = new BitSet();
    }

    /**
     * Add a new element to the buffer, returning the index of the element
     */
    public short push(int value) {
        final int index = count++;
        assert index < 4096 : "SmallValueBuffer can only hold up to 4096 chunks";

        ensureCapacity(index);
        buffer[index] = value;
        return (short) index;
    }

    /**
     * Add a new 64-bit element to the buffer, returning the index of the element
     */
    public short push(long value) {
        final int lsb = (int) (value & 0xFFFFFFFFL);
        final int msb = (int) (value >>> 32);

        final short lsbIndex = push(lsb);
        final short msbIndex = push(msb);
        assert msbIndex < 4096 : "SmallValueBuffer can only hold up to 4096 chunks";

        wideElements.set(lsbIndex);
        return lsbIndex;
    }

    /**
     * Replace an existing element in the buffer with a new value. A new index
     * may be returned, e.g. if a new value is wider than the previous.
     */
    public short replace(short index, int value) {
        ensureCapacity(index);
        buffer[index] = value;
        return index;
    }

    /**
     * Replace an existing element in the buffer with a new 64-bit value.
     */
    public short replace(short index, long value) {
        boolean isWide = wideElements.get(index);

        if (isWide) {
            final int lsb = (int) (value & 0xFFFFFFFFL);
            final int msb = (int) (value >>> 32);

            replace(index, lsb);
            replace((short) (index + 1), msb);
            return index;
        } else {
            return push(value);
        }
    }

    /**
     * Get a 32-bit value
     */
    public int get32(short index) {
        if (index >= count) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + count);
        }
        return buffer[index];
    }

    /**
     * Get a 64-bit value
     */
    public long get64(short index) {
        final long lsb = get32(index) & 0xFFFFFFFFL;
        final long msb = get32((short) (index + 1)) & 0xFFFFFFFFL;
        return (msb << 32) | lsb;
    }

    /**
     * Ensures that the buffer has enough capacity to store the element at the specified index
     */
    private void ensureCapacity(int index) {
        if (index >= buffer.length) {
            int newCapacity = Math.max(buffer.length * 2, index + 1);
            // Cap at 4096 as per the assertion in push()
            newCapacity = Math.min(newCapacity, 4096);
            int[] newBuffer = new int[newCapacity];
            System.arraycopy(buffer, 0, newBuffer, 0, buffer.length);
            buffer = newBuffer;
        }
    }
}
