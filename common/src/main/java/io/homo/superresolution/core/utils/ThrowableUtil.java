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

package io.homo.superresolution.core.utils;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

public final class ThrowableUtil {
    private ThrowableUtil() {
    }

    public static void rethrowError(Throwable failure) {
        Error error = findError(
                failure,
                Collections.newSetFromMap(new IdentityHashMap<>()));
        if (error != null) {
            throw error;
        }
    }

    private static Error findError(
            Throwable failure,
            Set<Throwable> visited) {
        if (failure == null || !visited.add(failure)) {
            return null;
        }
        if (failure instanceof Error error) {
            return error;
        }
        for (Throwable suppressed : failure.getSuppressed()) {
            Error suppressedError = findError(suppressed, visited);
            if (suppressedError != null) {
                return suppressedError;
            }
        }
        return findError(failure.getCause(), visited);
    }
}
