package io.homo.superresolution.common.gui.options.option;

import io.homo.superresolution.common.gui.Rect;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.util.FastColor;
import org.jetbrains.annotations.NotNull;


public class SelectOption extends AbstractOption<Boolean> {
    protected Rect rect;
    protected Rect boxRect;
    private Boolean value = false;

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, Rect rect) {
        this.rect = rect;
        this.boxRect = new Rect(rect.width - 17, rect.getCenterY() - 5, 10, 10);
        if (!this.isVisible()) return;
        hovered = rect.in(mouseX, mouseY);
        if (hovered && !this.disabled) {
            this.drawRect(graphics, rect.x, rect.y, rect.width, rect.height, FastColor.ARGB32.color(120, 0, 0, 0));
            this.renderHovered(graphics, rect);
        }
        int color = disabled ? this.style.textDisabledColor : this.style.textColor;
        this.drawString(graphics, this.label, rect.x + 4, rect.getCenterY() - 4, color);
        this.renderBox(graphics);
        this.renderTooltip();
    }

    @Override
    public Boolean getValue() {
        return this.value;
    }

    @Override
    public void setValue(Boolean value) {
        this.value = value;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isVisibleAndNotDisabled()) return false;
        if (button == 0 && rect.in(mouseX, mouseY) && isVisibleAndNotDisabled()) {
            this.value = !this.value;
            this.onChange.accept(this.value);
            playClickSound();
            return true;
        } else {
            return false;
        }

    }

    protected void renderBox(GuiGraphics graphics) {
        int x1 = boxRect.x;
        int y1 = boxRect.y;
        int x2 = x1 + boxRect.width;
        int y2 = y1 + boxRect.height;
        int color = this.disabled ? this.style.bgDisabledColor : this.style.bgColor;
        graphics.fill(x1, y1, x2, y1 + 1, color); //上
        graphics.fill(x1, y2 - 1, x2, y2, color); // 下
        graphics.fill(x1, y1 + 1, x1 + 1, y2 - 1, color); // 左
        graphics.fill(x2 - 1, y1 + 1, x2, y2 - 1, color);
        color = this.disabled ? this.style.mainDisabledColor : this.style.mainColor;
        if (this.value) {
            graphics.fill(x1 + 2, y1 + 2, x2 - 2, y2 - 2, color);
        }
    }

    @Override
    public @NotNull ScreenRectangle getRectangle() {
        return new ScreenRectangle(rect.x, rect.y, rect.width, rect.height);
    }
}
