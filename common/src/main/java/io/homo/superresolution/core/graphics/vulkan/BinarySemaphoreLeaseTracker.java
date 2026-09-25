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

package io.homo.superresolution.core.graphics.vulkan;

final class BinarySemaphoreLeaseTracker {
    private final boolean[] leased;
    private final long[] generations;
    private boolean closed;
    private int activeLeases;

    BinarySemaphoreLeaseTracker(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be greater than zero");
        }
        leased = new boolean[capacity];
        generations = new long[capacity];
    }

    synchronized Token acquire() throws InterruptedException {
        while (true) {
            requireOpen();
            Token token = tryAcquireInternal(-1);
            if (token != null) {
                return token;
            }
            wait();
        }
    }

    synchronized Token tryAcquire() {
        requireOpen();
        return tryAcquireInternal(-1);
    }

    synchronized Token acquireSlot(int slot) {
        requireSlot(slot);
        requireOpen();
        if (leased[slot]) {
            throw new IllegalStateException("Binary semaphore slot " + slot + " is already leased");
        }
        return lease(slot);
    }

    synchronized void release(Token token) {
        requireSlot(token.slot());
        if (!leased[token.slot()] || generations[token.slot()] != token.generation()) {
            throw new IllegalStateException("Stale or duplicate binary semaphore lease");
        }
        leased[token.slot()] = false;
        activeLeases--;
        notifyAll();
    }

    synchronized int activeLeases() {
        return activeLeases;
    }

    synchronized void close() {
        if (activeLeases != 0) {
            throw new IllegalStateException(
                    "Cannot close binary semaphore pool with " + activeLeases + " active lease(s)"
            );
        }
        closed = true;
        notifyAll();
    }

    private Token tryAcquireInternal(int preferredSlot) {
        if (preferredSlot >= 0) {
            return leased[preferredSlot] ? null : lease(preferredSlot);
        }
        for (int slot = 0; slot < leased.length; slot++) {
            if (!leased[slot]) {
                return lease(slot);
            }
        }
        return null;
    }

    private Token lease(int slot) {
        long generation = ++generations[slot];
        if (generation <= 0L) {
            throw new IllegalStateException("Binary semaphore lease generation overflowed");
        }
        leased[slot] = true;
        activeLeases++;
        return new Token(slot, generation);
    }

    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("Binary semaphore pool is closed");
        }
    }

    private void requireSlot(int slot) {
        if (slot < 0 || slot >= leased.length) {
            throw new IndexOutOfBoundsException("Binary semaphore slot " + slot + " is out of range");
        }
    }

    record Token(int slot, long generation) {
    }
}
