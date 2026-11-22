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

class MinecraftVersion private constructor(val parts: List<Int>) : Comparable<MinecraftVersion> {
    companion object {
        private val number = Regex("""\d+""")

        fun of(version: String): MinecraftVersion {
            val parts = number.findAll(version).map { it.value.toInt() }.toList()
            require(parts.isNotEmpty()) { "No numeric parts found in Minecraft version: $version" }
            return MinecraftVersion(parts)
        }
    }

    override fun compareTo(other: MinecraftVersion): Int {
        for (i in 0 until maxOf(parts.size, other.parts.size)) {
            val diff = parts.getOrElse(i) { 0 }.compareTo(other.parts.getOrElse(i) { 0 })
            if (diff != 0) return diff
        }
        return 0
    }

    override fun equals(other: Any?): Boolean = other is MinecraftVersion && compareTo(other) == 0

    override fun hashCode(): Int = parts.dropLastWhile { it == 0 }.hashCode()

    override fun toString(): String = parts.joinToString(".")
}
