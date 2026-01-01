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

package io.homo.superresolution.core.gui.widgets.menu;

public enum MaterialMenuSelectionMode {
    /**
     * 不允许选择
     */
    None,
    /**
     * 仅允许选择一个（整个菜单只能有一个被选中）
     */
    Single,
    /**
     * 仅允许选择一个，但至少选择一个
     */
    SingleAtLeastOne,
    /**
     * 每个组仅允许选择一个
     */
    SinglePerGroup,
    /**
     * 允许多选
     */
    Multiple,
    /**
     * 允许多选，但至少选择一个
     */
    MultipleAtLeastOne
}
