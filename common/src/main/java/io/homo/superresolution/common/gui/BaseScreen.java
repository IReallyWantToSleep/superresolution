package io.homo.superresolution.common.gui;

import io.homo.superresolution.common.utils.ColorUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public abstract class BaseScreen extends Screen {
    protected BaseScreen(Component title) {
        super(title);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {

        #if MC_VER > MC_1_20_4

        #if MC_VER > MC_1_20_4
        super.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        #else
        super.renderBackground(guiGraphics);
        #endif

        if (getGuiRect() != null)
            guiGraphics.fill(getGuiRect().x, getGuiRect().y, getGuiRect().width, getGuiRect().height, ColorUtil.color(100, 0, 0, 0));

        #endif
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderMain(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, Component.translatable("superresolution.screen.config.name"), this.width / 2, 6, ColorUtil.color(255, 255, 255, 255));
    }

    public abstract void renderMain(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick);

    protected abstract Rectangle getGuiRect();

    #if MC_VER >= MC_1_21_1
    @Override
    public void renderBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (this.minecraft != null) {
            if (this.minecraft.level == null) {
                this.renderPanorama(guiGraphics, partialTick);
                #if MC_VER > MC_1_21_1
                this.renderBlurredBackground();
                #else
                this.renderBlurredBackground(partialTick);
                #endif
                this.renderMenuBackground(guiGraphics);
            }
        }
        if (getGuiRect() != null)
            guiGraphics.fill(getGuiRect().x, getGuiRect().y, getGuiRect().width, getGuiRect().height, ColorUtil.color(100, 0, 0, 0));
    }
    #endif
}
