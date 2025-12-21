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

package io.homo.superresolution.api.event;

import net.neoforged.bus.api.Event;

/**
 * 世界渲染结束事件，触发位置随配置中的捕获模式改变<br>
 * 捕获模式为A或C时会在GameRenderer::renderLevel触发<br>
 * 捕获模式为B时会在LevelRenderer::renderLevel触发
 */
public class LevelRenderEndEvent extends Event {
}
