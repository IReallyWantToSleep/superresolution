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

package io.homo.superresolution.common.presentation.vulkan;

import java.util.concurrent.TimeUnit;

/**
 * Presents real frames on a thread of its own, for the swapchain path that has no frame
 * generation worker. Some drivers (NVIDIA under Wayland) block {@code vkQueuePresentKHR} until
 * the GPU has finished the frame, so a present on the render thread kept Minecraft from
 * recording the next frame in the meantime.
 *
 * <p>The thread holds one frame at a time. {@link #submit} waits for the previous present to
 * return before it hands over the next frame, so the render thread runs at most one frame
 * ahead and the next frame's GPU work queues behind this frame's blit.</p>
 */
final class RealFramePresentThread implements AutoCloseable {
    private static final long JOIN_TIMEOUT_NANOS = TimeUnit.SECONDS.toNanos(2);

    private final Object lock = new Object();
    private final Thread thread;
    private Runnable pending;
    private boolean running;
    private boolean closed;
    private Throwable failure;

    RealFramePresentThread() {
        thread = new Thread(this::run, "SR-Present");
        thread.setDaemon(true);
        thread.start();
    }

    /** Waits for the previous present to finish, then hands this one to the thread. */
    void submit(Runnable present) {
        synchronized (lock) {
            awaitIdleLocked();
            if (closed) {
                throw new IllegalStateException("The present thread is closed");
            }
            pending = present;
            lock.notifyAll();
        }
    }

    /** Waits until no present is pending or running. Rethrows a present failure. */
    void drain() {
        synchronized (lock) {
            awaitIdleLocked();
        }
    }

    /** Drains, then stops the thread. Returns false if it did not stop in time. */
    boolean closeAndJoin() {
        synchronized (lock) {
            closed = true;
            lock.notifyAll();
        }
        try {
            TimeUnit.NANOSECONDS.timedJoin(thread, JOIN_TIMEOUT_NANOS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
        return !thread.isAlive();
    }

    boolean isStopped() {
        return !thread.isAlive();
    }

    @Override
    public void close() {
        if (!closeAndJoin()) {
            throw new IllegalStateException("The present thread did not stop");
        }
        synchronized (lock) {
            throwIfFailedLocked();
        }
    }

    private void awaitIdleLocked() {
        boolean interrupted = false;
        while ((pending != null || running) && failure == null) {
            try {
                lock.wait();
            } catch (InterruptedException exception) {
                // A present is in flight and owns a capture slot; finish waiting for it.
                interrupted = true;
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
        throwIfFailedLocked();
    }

    private void throwIfFailedLocked() {
        if (failure != null) {
            throw new IllegalStateException("The Vulkan present thread failed", failure);
        }
    }

    private void run() {
        while (true) {
            Runnable present;
            synchronized (lock) {
                while (pending == null && !closed) {
                    try {
                        lock.wait();
                    } catch (InterruptedException ignored) {
                        // Only closing stops the thread; it may still be handed a frame.
                    }
                }
                if (pending == null) {
                    return;
                }
                present = pending;
                pending = null;
                running = true;
            }
            Throwable thrown = null;
            try {
                present.run();
            } catch (Throwable throwable) {
                thrown = throwable;
            }
            synchronized (lock) {
                running = false;
                if (thrown != null) {
                    failure = thrown;
                    closed = true;
                }
                lock.notifyAll();
            }
            if (thrown != null) {
                return;
            }
        }
    }
}
