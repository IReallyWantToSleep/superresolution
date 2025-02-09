package io.homo.superresolution.common.gui.options.option;

import io.homo.superresolution.common.gui.Rect;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.util.FastColor;
import org.jetbrains.annotations.NotNull;


public class BooleanOption extends AbstractOption<Boolean> {
    private boolean value;
    private Rect switchRect;
    private Rect rect;

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
        int x1 = rect.width - 32;
        int x2 = rect.width - 5 - 2;
        int y2 = rect.height + 6;
        int y1 = rect.getCenterY() + (y2 / 2) - 9;
        this.drawString(graphics, this.label, rect.x + 4, rect.getCenterY() - 4, color);
        this.switchRect = new Rect(rect.x, rect.y, rect.width, rect.height);
        color = disabled ? this.style.bgDisabledColor : this.style.bgColor;
        graphics.fill(x1, y1, x2, y1 + 1, color);
        graphics.fill(x1, y2 - 1, x2, y2, color);
        graphics.fill(x1, y1, x1 + 1, y2, color);
        graphics.fill(x2 - 1, y1, x2, y2, color);
        if (this.value) {
            this.drawRect(graphics, x1 + 15, y1 - 8, x2 - 3, 6, disabled ? this.style.mainDisabledColor : this.style.mainColor);
        } else {
            this.drawRect(graphics, x1 + 3, y1 - 8, x2 - 15, 6, disabled ? this.style.bgDisabledColor : this.style.bgColor);
        }
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
        if (isVisibleAndNotDisabled() && this.switchRect.in(mouseX, mouseY)) {
            this.value = !this.value;
            this.onChange.accept(this.value);
            this.playClickSound();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public @NotNull ScreenRectangle getRectangle() {
        return new ScreenRectangle(rect.x, rect.y, rect.width, rect.height);
    }
}
