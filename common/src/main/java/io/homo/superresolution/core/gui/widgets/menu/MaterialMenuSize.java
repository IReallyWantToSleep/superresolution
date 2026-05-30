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

public record MaterialMenuSize(
        float cornerRadius,

        float bottomCornerRadius,

        float minWidth,

        float maxWidth,

        float verticalPadding
) {
    public static final MaterialMenuSize Standard = new MaterialMenuSize(16, 8, 208, 280, 2);
    public static final MaterialMenuSize Compact = new MaterialMenuSize(
            8f,    // 采用 MD3 Shape Tokens 中的 Medium/Small 级别圆角
            4f,    // 底部圆角按比例减半，保持视觉的一致性
            160f,  // 缩减最小宽度，允许更精致的小型下拉菜单
            240f,  // 缩减最大宽度，防止长文本过度拉伸菜单
            1f     // 微调垂直内边距，使菜单项排列更紧凑
    );
}
