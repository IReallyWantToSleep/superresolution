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

package io.homo.superresolution.core.graphics.impl.command;

public enum CommandBufferBehavior {
    ReusableSequential,
    /**
     * 一次性提交模式。
     * <p>
     * 当使用此行为时，命令缓冲区不会被 Java 侧的 {@link ICommandPool} 管理（仅限 Vulkan）。
     * 提交后会自动等待执行完成并释放底层原生资源，以避免 Java 侧的内存泄漏。
     * </p>
     */
    OneTimeSubmit
}
