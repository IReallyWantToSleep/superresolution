/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package io.homo.superresolution.common.presentation.vulkan;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Bounded queue used by the application-managed presentation threads.
 *
 * <p>Closing wakes all blocked producers and consumers. Items already committed
 * remain available to the consumer, which lets shutdown drain submitted work
 * without exposing a half-committed batch.</p>
 */
final class FrameQueue<T> implements AutoCloseable {
    private final int capacity;
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();
    private final Condition hasCapacity = lock.newCondition();
    private final Deque<T> items = new ArrayDeque<>();
    private boolean closed;

    FrameQueue(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.capacity = capacity;
    }

    void put(T item) throws InterruptedException {
        putBatch(List.of(item));
    }

    void putBatch(Collection<? extends T> batch) throws InterruptedException {
        List<? extends T> snapshot = List.copyOf(batch);
        if (snapshot.isEmpty()) {
            throw new IllegalArgumentException("batch cannot be empty");
        }
        if (snapshot.size() > capacity) {
            throw new IllegalArgumentException("batch exceeds queue capacity");
        }

        lock.lockInterruptibly();
        try {
            while (!closed && capacity - items.size() < snapshot.size()) {
                hasCapacity.await();
            }
            requireOpen();
            items.addAll(snapshot);
            notEmpty.signalAll();
        } finally {
            lock.unlock();
        }
    }

    HeadResult<T> awaitHead() throws InterruptedException {
        return awaitHead(() -> false);
    }

    HeadResult<T> awaitHead(BooleanSupplier externalWakeCondition) throws InterruptedException {
        if (externalWakeCondition == null) {
            throw new IllegalArgumentException("externalWakeCondition cannot be null");
        }
        lock.lockInterruptibly();
        try {
            boolean waited = false;
            while (!closed && items.isEmpty() && !externalWakeCondition.getAsBoolean()) {
                waited = true;
                notEmpty.await();
            }
            if (items.isEmpty()) {
                return closed
                        ? HeadResult.closed(waited)
                        : HeadResult.externalWake(waited);
            }
            return new HeadResult<>(items.peekFirst(), waited, false, false);
        } finally {
            lock.unlock();
        }
    }

    TakeResult<T> takeResult() throws InterruptedException {
        lock.lockInterruptibly();
        try {
            boolean waited = false;
            while (!closed && items.isEmpty()) {
                waited = true;
                notEmpty.await();
            }
            if (items.isEmpty()) {
                return TakeResult.closed(waited);
            }
            T item = items.removeFirst();
            hasCapacity.signalAll();
            return new TakeResult<>(item, waited, false);
        } finally {
            lock.unlock();
        }
    }

    boolean removeHead(T expected) {
        lock.lock();
        try {
            if (items.peekFirst() != expected) {
                throw new IllegalStateException("Queue head changed before ownership removal");
            }
            items.removeFirst();
            hasCapacity.signalAll();
            return true;
        } finally {
            lock.unlock();
        }
    }

    int size() {
        lock.lock();
        try {
            return items.size();
        } finally {
            lock.unlock();
        }
    }

    boolean isEmpty() {
        return size() == 0;
    }

    boolean isClosed() {
        lock.lock();
        try {
            return closed;
        } finally {
            lock.unlock();
        }
    }

    void signalConsumer() {
        lock.lock();
        try {
            notEmpty.signalAll();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void close() {
        lock.lock();
        try {
            closed = true;
            notEmpty.signalAll();
            hasCapacity.signalAll();
        } finally {
            lock.unlock();
        }
    }

    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("Frame queue is closed");
        }
    }

    record HeadResult<T>(
            T value,
            boolean waited,
            boolean closedAndEmpty,
            boolean externalWake
    ) {
        private static <T> HeadResult<T> closed(boolean waited) {
            return new HeadResult<>(null, waited, true, false);
        }

        private static <T> HeadResult<T> externalWake(boolean waited) {
            return new HeadResult<>(null, waited, false, true);
        }
    }

    record TakeResult<T>(T value, boolean waited, boolean closedAndEmpty) {
        private static <T> TakeResult<T> closed(boolean waited) {
            return new TakeResult<>(null, waited, true);
        }
    }
}
