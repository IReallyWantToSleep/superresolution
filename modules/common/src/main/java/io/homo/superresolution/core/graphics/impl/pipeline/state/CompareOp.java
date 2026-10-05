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

package io.homo.superresolution.core.graphics.impl.pipeline.state;

public enum CompareOp {
    Never,
    Less,
    Equal,
    LessEqual,
    Greater,
    NotEqual,
    GreaterEqual,
    Always;

    public int gl() {
        return switch (this) {
            case Never -> org.lwjgl.opengl.GL11.GL_NEVER;
            case Less -> org.lwjgl.opengl.GL11.GL_LESS;
            case Equal -> org.lwjgl.opengl.GL11.GL_EQUAL;
            case LessEqual -> org.lwjgl.opengl.GL11.GL_LEQUAL;
            case Greater -> org.lwjgl.opengl.GL11.GL_GREATER;
            case NotEqual -> org.lwjgl.opengl.GL11.GL_NOTEQUAL;
            case GreaterEqual -> org.lwjgl.opengl.GL11.GL_GEQUAL;
            case Always -> org.lwjgl.opengl.GL11.GL_ALWAYS;
        };
    }

    public int vk() {
        return switch (this) {
            case Never -> org.lwjgl.vulkan.VK10.VK_COMPARE_OP_NEVER;
            case Less -> org.lwjgl.vulkan.VK10.VK_COMPARE_OP_LESS;
            case Equal -> org.lwjgl.vulkan.VK10.VK_COMPARE_OP_EQUAL;
            case LessEqual -> org.lwjgl.vulkan.VK10.VK_COMPARE_OP_LESS_OR_EQUAL;
            case Greater -> org.lwjgl.vulkan.VK10.VK_COMPARE_OP_GREATER;
            case NotEqual -> org.lwjgl.vulkan.VK10.VK_COMPARE_OP_NOT_EQUAL;
            case GreaterEqual -> org.lwjgl.vulkan.VK10.VK_COMPARE_OP_GREATER_OR_EQUAL;
            case Always -> org.lwjgl.vulkan.VK10.VK_COMPARE_OP_ALWAYS;
        };
    }
}
