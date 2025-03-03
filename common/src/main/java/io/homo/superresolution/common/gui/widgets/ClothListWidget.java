package io.homo.superresolution.common.gui.widgets;

import me.shedaniel.clothconfig2.api.AbstractConfigEntry;
import me.shedaniel.clothconfig2.gui.AbstractConfigScreen;
import me.shedaniel.clothconfig2.gui.ClothConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

public class ClothListWidget extends ClothConfigScreen.ListWidget<AbstractConfigEntry<AbstractConfigEntry<?>>> {
    public ClothListWidget(AbstractConfigScreen screen, Minecraft client, int width, int height, int top, int bottom, ResourceLocation backgroundLocation) {
        super(screen, client, width, height, top, bottom, backgroundLocation);
    }

    @Override
    protected int getScrollbarPosition() {
        return this.width - 6;
    }
}
