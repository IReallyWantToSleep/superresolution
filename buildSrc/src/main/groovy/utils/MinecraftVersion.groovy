/*
 * Super Resolution
 * Copyright (c) 2025. 187J3X1-114514
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

package utils

class MinecraftVersion implements Comparable<MinecraftVersion> {
    final List<Integer> parts

    MinecraftVersion(String ver) {
        if (ver == null || ver.trim().isEmpty()) {
            throw new IllegalArgumentException("version must not be empty")
        }
        this.parts = ver.findAll(/\d+/).collect { it.toInteger() }
        if (parts.isEmpty()) {
            throw new IllegalArgumentException("no numeric parts found in version: $ver")
        }
    }

    static MinecraftVersion of(String v) {
        return new MinecraftVersion(v)
    }

    @Override
    int compareTo(MinecraftVersion o) {
        int max = Math.max(this.parts.size(), o.parts.size())
        for (int i = 0; i < max; i++) {
            int a = (i < this.parts.size()) ? this.parts[i] : 0
            int b = (i < o.parts.size()) ? o.parts[i] : 0
            if (a != b) {
                return a <=> b
            }
        }
        return 0
    }

    @Override
    boolean equals(Object o) {
        if (this.is(o)) return true
        if (!(o instanceof MinecraftVersion)) return false
        return this.compareTo((MinecraftVersion) o) == 0
    }

    @Override
    int hashCode() {
        return parts.hashCode()
    }

    @Override
    String toString() {
        return parts.join('.')
    }
}
