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

package io.homo.superresolution.core.gui;

import io.homo.superresolution.common.minecraft.MinecraftWindow;
import io.homo.superresolution.core.gui.core.UIInputState;
import io.homo.superresolution.core.gui.core.backends.interfaces.TextAlign;
import io.homo.superresolution.core.gui.core.backends.interfaces.TextAlignType;
import io.homo.superresolution.core.gui.core.backends.render.RenderContext;
import io.homo.superresolution.core.gui.test.RenderTest;
import io.homo.superresolution.core.utils.Color;
import net.minecraft.network.chat.Component;
import org.joml.Vector2f;

public class RenderTestScreen extends NanoVGScreen<RenderTestScreen> {
    private int testMode = 0;

    public RenderTestScreen() {
        super(Component.literal("Render Test Screen"));
    }

    @Override
    protected void buildWidgets() {

    }

    @Override
    public void draw(RenderContext ctx, UIInputState inputState) {
        Vector2f screenSize = MinecraftWindow.getWindowSize();
        screenSize.mul(1 / ctx.guiScale());
        ctx.rect(0, 0, screenSize.x, screenSize.y, Color.rgb(30, 30, 30), true);

        if (testMode == 0) {
            RenderTest.drawTestPath(ctx);
        } else if (testMode == 1) {
            RenderTest.drawTestText(ctx);
        } else if (testMode == 2) {
            RenderTest.drawTestAlpha(ctx);
        } else if (testMode == 3) {
            RenderTest.drawTestScissor(ctx);
        }

        String modeName = switch (testMode) {
            case 0 -> "路径";
            case 1 -> "文本";
            case 2 -> "不透明";
            case 3 -> "裁切";
            default -> "？";
        };

        ctx.drawAlignedText(
                ctx.font(),
                12,
                "测试: " + modeName + " (F3)",
                10, screenSize.y - 30, screenSize.x - 20, 20,
                Color.rgb(200, 200, 200),
                null,
                false
        );

        super.draw(ctx, inputState);
    }


    #if MC_VER > MC_1_21_8
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (event.key() == 292) { // GLFW_KEY_F3
            testMode = (testMode + 1) % 4;
            return true;
        }
        return false;
    }

    #else
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 292) { // GLFW_KEY_F3
            testMode = (testMode + 1) % 4;
            return true;
        }
        return false;
    }
    #endif

}
