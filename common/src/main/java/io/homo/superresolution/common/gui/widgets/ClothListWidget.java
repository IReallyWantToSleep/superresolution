package io.homo.superresolution.common.gui.widgets;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.Tesselator;
import io.homo.superresolution.core.utils.ColorUtil;
import me.shedaniel.clothconfig2.api.AbstractConfigEntry;
import me.shedaniel.clothconfig2.gui.AbstractConfigScreen;
import me.shedaniel.clothconfig2.gui.ClothConfigScreen;
import me.shedaniel.clothconfig2.gui.widget.DynamicEntryListWidget;
import me.shedaniel.math.Color;
import me.shedaniel.math.Rectangle;
import me.shedaniel.math.impl.PointHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class ClothListWidget extends ClothConfigScreen.ListWidget<AbstractConfigEntry<AbstractConfigEntry<?>>> {
    public ClothListWidget(AbstractConfigScreen screen, Minecraft client, int width, int height, int top, int bottom, ResourceLocation backgroundLocation) {
        super(screen, client, width, height, top, bottom, backgroundLocation);
    }

    @Override
    protected int getScrollbarPosition() {
        return this.width - 6;
    }

    public double getScroll() {
        return this.scroll;
    }

    @Override
    #if MC_VER > MC_1_21_1
    protected void renderScrollBar(GuiGraphics graphics, int maxScroll, int scrollbarPositionMinX, int scrollbarPositionMaxX)
    #else
    protected void renderScrollBar(GuiGraphics graphics, Tesselator tessellator, BufferBuilder buffer, int maxScroll, int scrollbarPositionMinX, int scrollbarPositionMaxX)
    #endif {
        if (maxScroll > 0) {
            int height = (this.bottom - this.top) * (this.bottom - this.top) / this.getMaxScrollPosition();
            height = Mth.clamp(height, 32, this.bottom - this.top - 8);
            height = (int) ((double) height - Math.min(this.scroll < (double) 0.0F ? (int) (-this.scroll) : (this.scroll > (double) this.getMaxScroll() ? (int) this.scroll - this.getMaxScroll() : 0), (double) height * 0.95));
            height = Math.max(10, height);
            int minY = Math.min(Math.max((int) this.getScroll() * (this.bottom - this.top - height) / maxScroll + this.top, this.top), this.bottom - height);
            int bottomc = (new Rectangle(scrollbarPositionMinX, minY, scrollbarPositionMaxX - scrollbarPositionMinX, height)).contains(PointHelper.ofMouse()) ? 168 : 128;
            graphics.fill(scrollbarPositionMinX, this.top, scrollbarPositionMaxX, this.bottom, ColorUtil.color(120, 0, 0, 0));
            graphics.fill(scrollbarPositionMinX, minY, scrollbarPositionMaxX, minY + height, Color.ofRGB(bottomc, bottomc, bottomc).getColor());
        }
    }

    @Override
    public void scrollTo(double value, boolean animated, long duration) {
        if (animated) {
            this.scrollAnimator.setTo(value, 200);
        } else {
            this.scrollAnimator.setAs(value);
        }
    }

    @Override
    #if MC_VER > MC_1_20_1
    public boolean mouseScrolled(double mouseX, double mouseY, double x, double y) {
        for (DynamicEntryListWidget.Entry<?> entry : this.visibleChildren()) {
            if (entry.mouseScrolled(mouseX, mouseY, x, y)) {
                return true;
            }
        }
        this.offset(32 * -y, true);
        return true;
    }

    #else
    public boolean mouseScrolled(double mouseX, double mouseY, double x) {
        for (DynamicEntryListWidget.Entry<?> entry : this.visibleChildren()) {
            if (entry.mouseScrolled(mouseX, mouseY, x)) {
                return true;
            }
        }
        this.offset(32 * -x, true);
        return true;
    }

    #endif
    #if MC_VER > MC_1_21_1
    @Override
    protected void renderBackBackground(GuiGraphics graphics)
    #else
    @Override
    protected void renderBackBackground(GuiGraphics graphics, BufferBuilder buffer, Tesselator tessellator)
    #endif {
    }

    @Override
    protected void renderHoleBackground(GuiGraphics graphics, int y1, int y2, int alpha1, int alpha2) {
    }
}
