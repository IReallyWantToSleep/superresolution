/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package io.homo.superresolution.core.graphics.vulkan;

import java.util.concurrent.atomic.AtomicLong;

final class SubmissionTicketSequence {
    private final AtomicLong nextTicket = new AtomicLong();

    long next() {
        long ticket = nextTicket.incrementAndGet();
        if (ticket <= 0L) {
            throw new IllegalStateException("Vulkan submission ticket sequence overflowed");
        }
        return ticket;
    }

    long current() {
        return nextTicket.get();
    }
}
