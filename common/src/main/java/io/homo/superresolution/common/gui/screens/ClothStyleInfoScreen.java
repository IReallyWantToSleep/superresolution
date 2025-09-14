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

package io.homo.superresolution.common.gui.screens;

import me.shedaniel.clothconfig2.api.ConfigCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;


public class ClothStyleInfoScreen extends ClothStyleConfigScreen {
    public ClothStyleInfoScreen(Screen parent, Component title, Map<String, ConfigCategory> categoryMap, ResourceLocation backgroundLocation) {
        super(parent, title, categoryMap, backgroundLocation);
    }

    @Override
    protected void init() {
        super.init();
        this.removeWidget(exitButton);
        this.removeWidget(saveButton);
        cancelButton.setMessage(Component.translatable("superresolution.screen.button.label.return"));
        cancelButton.setX((width / 2) - (cancelButton.getWidth() / 2));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
    }
}
