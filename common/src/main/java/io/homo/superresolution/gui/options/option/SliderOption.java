package io.homo.superresolution.gui.options.option;

import io.homo.superresolution.gui.Rect;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FastColor;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.RoundingMode;


public class SliderOption extends AbstractOption<Double> {
    private final EditBox editBox;
    protected double value = 0.0;
    protected Rect rect;
    protected double min;
    protected double max;
    protected double step;
    protected Rect sliderRect;
    private boolean sliderDragged;
    private int decimalPlaces = 2;

    public SliderOption() {
        this.editBox = new EditBox(this.font, 0, 0, 100, 20, Component.empty());
        this.setEditBoxValue(this.value);
        this.editBox.setResponder(this::onEditBoxTextChange);
    }

    private void onEditBoxTextChange(String string) {
        try {
            Double.parseDouble(string);
            this.editBox.setTextColor(14737632);
        } catch (NumberFormatException e) {
            this.editBox.setTextColor(FastColor.ARGB32.color(255, 255, 0, 0));
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, Rect rect) {
        this.rect = rect;
        if (!this.isVisible()) return;
        hovered = rect.in(mouseX, mouseY);
        this.rect = rect;
        if (hovered && !this.disabled) {
            this.drawRect(graphics, rect.x, rect.y, rect.width, rect.height, FastColor.ARGB32.color(120, 0, 0, 0));
            this.renderHovered(graphics, rect);
        }
        int color = disabled ? this.style.textDisabledColor : this.style.textColor;
        this.drawString(graphics, this.label, rect.x + 4, rect.getCenterY() - 4, color);
        this.renderSlider(graphics, mouseX, mouseY, partialTick, rect);
        this.renderEditBox(graphics, mouseX, mouseY, partialTick, rect);
        this.renderTooltip();
    }

    @Override
    public Double getValue() {
        BigDecimal bd = new BigDecimal(Double.toString(value));
        bd = bd.setScale(this.decimalPlaces, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    @Override
    public void setValue(Double value) {
        this.value = value;
        this.setEditBoxValue(this.value);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isVisibleAndNotDisabled()) return false;
        sliderDragged = false;
        boolean focused = new Rect(
                this.editBox.getX(),
                this.editBox.getY(),
                this.editBox.getWidth(),
                this.editBox.getHeight()
        ).in(mouseX, mouseY) && isVisibleAndNotDisabled();
        if (!focused && this.editBox.isFocused()) {
            this.setValueByEditBox(this.editBox.getValue());
        }
        this.editBox.setFocused(focused);
        this.editBox.active = true;
        if (this.rect.in(mouseX, mouseY) && this.isVisibleAndNotDisabled()) {
            Rect newSliderRect = new Rect(sliderRect.x, rect.y, sliderRect.width, rect.height);
            if (newSliderRect.in(mouseX, mouseY)) {
                sliderDragged = true;
                this.playClickSound();
                this.setValueByMouseX((int) mouseX);
            }
            return this.editBox.mouseClicked(mouseX, mouseY, button);
        } else {
            this.focused = false;
            this.editBox.setFocused(false);
            return false;
        }
    }


    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (!isVisibleAndNotDisabled()) return false;
        return this.editBox.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (!isVisibleAndNotDisabled()) return false;
        return this.editBox.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!isVisibleAndNotDisabled()) return false;
        if (isVisibleAndNotDisabled() && keyCode == 335) {
            this.focused = false;
            this.editBox.setFocused(false);
            this.setValueByEditBox(this.editBox.getValue());
        }
        this.editBox.keyPressed(keyCode, scanCode, modifiers);
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!isVisibleAndNotDisabled()) return false;
        return this.editBox.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!isVisibleAndNotDisabled()) return false;
        this.sliderDragged = false;
        return this.editBox.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (!isVisibleAndNotDisabled()) return;
        this.editBox.mouseMoved(mouseX, mouseY);
    }

    public double getMin() {
        return min;
    }

    public SliderOption setMin(double min) {
        this.min = min;
        return this;
    }

    public double getMax() {
        return max;
    }

    public SliderOption setMax(double max) {
        this.max = max;
        return this;
    }

    public double getStep() {
        return step;
    }

    public SliderOption setStep(double step) {
        this.step = step;
        return this;
    }

    protected void setValueByEditBox(String str) {
        try {
            if (Double.parseDouble(str) != this.value) {
                this.value = clamp(Double.parseDouble(str), min, max);
                this.setEditBoxValue(this.value); //格式化输入
                this.onChange.accept(this.value);
            }
        } catch (NumberFormatException e) {
            this.setEditBoxValue(this.value);
        }
    }

    protected void setValueByMouseX(int x) {
        this.value = progressToValue((double) (x - this.sliderRect.x) / this.sliderRect.width);
        this.setEditBoxValue(this.value);
        this.onChange.accept(this.value);
    }

    protected void renderSlider(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, Rect rect) {
        int sliderWidth = (int) (rect.width * 0.38);
        this.sliderRect = new Rect(
                (int) (rect.width - (rect.width * 0.5) - 6),
                rect.y + rect.height / 2,
                sliderWidth,
                rect.height / 2
        );
        int color = this.disabled ? this.style.bgDisabledColor : this.style.bgColor;
        graphics.fill(sliderRect.x, sliderRect.y, sliderWidth + sliderRect.x, sliderRect.y + 2, color);
        double progress = valueToProgress(this.value);
        int blockX = (int) (sliderRect.x + (sliderRect.width * progress));
        color = this.disabled ? this.style.mainDisabledColor : this.style.mainColor;
        graphics.fill(blockX, sliderRect.y - 3, (4 + blockX), 5 + sliderRect.y, color);
    }

    protected void renderEditBox(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, Rect rect) {
        editBox.setPosition((int) (rect.width - (rect.width * 0.1) - 6), rect.y + 2);
        editBox.setSize((int) (rect.width * 0.1), rect.height - 4);
        editBox.render(graphics, mouseX, mouseY, partialTick);
    }

    protected double clamp(double value, double min, double max) {
        return Math.max(Math.min(value, max), min);
    }

    protected double valueToProgress(double value) {
        if (min == max) return 1;
        return (clamp(value, min, max) - min) / (max - min);
    }

    protected double progressToValue(double progress) {
        if (progress >= 1) return max;
        if (progress <= 0) return min;
        double value = min + clamp(progress, 0, 1) * (max - min);
        return step == 0 ? value : value - (value % step);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!this.isVisibleAndNotDisabled()) return false;
        if (this.isVisibleAndNotDisabled() && button == 0 && this.sliderDragged) {
            this.setValueByMouseX((int) mouseX);
            return true;
        }
        return false;
    }

    @Override
    public SliderOption setDisabled(boolean disabled) {
        super.setDisabled(disabled);
        this.editBox.setEditable(!disabled);
        return this;
    }

    protected void setEditBoxValue(double value) {
        BigDecimal bd = new BigDecimal(Double.toString(value));
        bd = bd.setScale(this.decimalPlaces, RoundingMode.HALF_UP);
        this.editBox.setValue(String.valueOf(bd.doubleValue()));
    }

    public void setDecimalPlaces(int decimalPlaces) {
        this.decimalPlaces = decimalPlaces;
        this.setEditBoxValue(this.value);
    }

    @Override
    public @NotNull ScreenRectangle getRectangle() {
        return new ScreenRectangle(rect.x, rect.y, rect.width, rect.height);
    }
}
