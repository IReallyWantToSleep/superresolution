package io.homo.superresolution.common.gui.options.option;

import io.homo.superresolution.common.gui.Rect;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.util.FastColor;
import org.jetbrains.annotations.NotNull;


public class EnumOption extends AbstractOption<EnumData.EnumInfo<?>> {
    protected EnumData enumData;
    protected Rect rect;
    protected int currentIndex = 0;
    private EnumData.EnumInfo<?> value;

    public EnumOption(EnumData enumData) {
        this.enumData = enumData;
        this.value = this.enumData.getEnum(this.currentIndex);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, Rect rect) {
        this.rect = rect;
        if (!this.isVisible()) return;
        hovered = rect.in(mouseX, mouseY);
        if (hovered && !this.disabled) {
            this.drawRect(graphics, rect.x, rect.y, rect.width, rect.height, FastColor.ARGB32.color(120, 0, 0, 0));
            this.renderHovered(graphics, rect);
        }
        int color = disabled ? this.style.textDisabledColor : this.style.textColor;
        this.drawString(graphics, this.label, rect.x + 4, rect.getCenterY() - 4, color);
        if (!this.enumData.enums.isEmpty() && this.enumData.getEnum(this.currentIndex) != null && this.value != null) {
            color = disabled ? this.style.mainDisabledColor : this.style.mainColor;
            String text = this.value.getDisplayName();
            this.drawString(graphics, text, rect.width - this.font.width(text) - 7, rect.getCenterY() - 4, color);
        }
        this.renderTooltip();
    }

    @Override
    public EnumData.EnumInfo<?> getValue() {
        return value;
    }

    @Override
    public void setValue(EnumData.EnumInfo<?> value) {
        this.value = value;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isVisibleAndNotDisabled()) return false;
        if (this.isVisibleAndNotDisabled() && rect.in(mouseX, mouseY) && button == 0) {
            if (!this.enumData.enums.isEmpty()) {
                this.currentIndex = (this.currentIndex + 1) % this.enumData.enums.size();
                if (this.enumData.getEnum(this.currentIndex) != null) {
                    this.value = this.enumData.getEnum(this.currentIndex);
                }
            }
            return true;
        }
        return false;
    }

    public EnumData getEnumData() {
        return enumData;
    }

    public void setEnumData(EnumData enumData) {
        this.enumData = enumData;
    }

    @Override
    public @NotNull ScreenRectangle getRectangle() {
        return new ScreenRectangle(rect.x, rect.y, rect.width, rect.height);
    }
}
