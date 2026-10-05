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

package io.homo.superresolution.core.utils;

/**
 * Runs a task on a dedicated thread with a large stack and blocks until it finishes.
 * <p>
 * Some native upscaler initializers (notably NVIDIA NGX's {@code NVSDK_NGX_VULKAN_Init},
 * which reserves a ~1&nbsp;MB buffer on the stack) need far more stack than HotSpot's
 * 1&nbsp;MB default thread stack provides. When such a call runs directly on the render
 * thread it overflows the stack and the process dies with a bare SIGSEGV inside the NVIDIA
 * driver (no {@code hs_err}). JVMs with larger default stacks (e.g. Azul Zing) happen to
 * survive, which is why the crash looked JVM-vendor specific.
 * <p>
 * Running the initializer on a thread created with an explicit large stack size removes the
 * dependency on {@code -Xss} so DLSS/FSR/XeSS init works on any JVM out of the box.
 */
public final class LargeStackExecutor {
    /** 16 MB — matches the {@code -Xss16m} value verified to fix the NGX init overflow. */
    public static final long DEFAULT_STACK_SIZE = 16L * 1024 * 1024;

    private LargeStackExecutor() {
    }

    public static void run(String name, Runnable task) {
        run(name, DEFAULT_STACK_SIZE, task);
    }

    /**
     * Runs {@code task} on a new thread whose stack is at least {@code stackSizeBytes}, waits for
     * it to complete, and re-throws (on the calling thread) any {@link RuntimeException} or
     * {@link Error} the task produced, preserving the caller's original error semantics.
     */
    public static void run(String name, long stackSizeBytes, Runnable task) {
        Throwable[] thrown = new Throwable[1];
        Thread thread = new Thread(null, () -> {
            try {
                task.run();
            } catch (Throwable t) {
                thrown[0] = t;
            }
        }, name, stackSizeBytes);
        thread.start();

        boolean interrupted = false;
        while (true) {
            try {
                thread.join();
                break;
            } catch (InterruptedException e) {
                interrupted = true;
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }

        if (thrown[0] != null) {
            if (thrown[0] instanceof RuntimeException re) {
                throw re;
            }
            if (thrown[0] instanceof Error err) {
                throw err;
            }
            throw new RuntimeException(thrown[0]);
        }
    }
}
