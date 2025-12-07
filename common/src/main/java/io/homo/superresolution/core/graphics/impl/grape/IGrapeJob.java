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

package io.homo.superresolution.core.graphics.impl.grape;

import io.homo.superresolution.core.graphics.impl.command.ICommandBuffer;

public interface IGrapeJob {
    /**
     * 把作业提交到命令缓冲区
     *
     * @param commandBuffer 目标命令缓冲区
     */
    void execute(ICommandBuffer commandBuffer);

    /**
     * 销毁作业资源
     */
    void destroy();
}
