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

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AsyncTimer {
    private final ScheduledExecutorService scheduler;

    public AsyncTimer() {
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    public AsyncTimer schedule(Runnable task, long delay, TimeUnit unit) {
        scheduler.schedule(() -> {
            try {
                task.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, delay, unit);
        return this;
    }

    public AsyncTimer scheduleAtFixedRate(Runnable task, long initialDelay, long period, TimeUnit unit) {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                task.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, initialDelay, period, unit);
        return this;
    }

    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }
}