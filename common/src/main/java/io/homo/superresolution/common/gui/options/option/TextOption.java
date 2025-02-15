package io.homo.superresolution.common.gui.options.option;

import io.homo.superresolution.common.gui.Rect;
import io.homo.superresolution.common.mixin.gui.AbstractWidgetAccessor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FastColor;
import org.jetbrains.annotations.NotNull;


public class TextOption extends AbstractOption<String> {
    private final EditBox editBox;
    private String value;
    private Rect rect;

    public TextOption() {
        this.editBox = new EditBox(this.font, 0, 0, 100, 20, Component.empty());
        this.editBox.setResponder((s) -> {
            this.value = s;
            this.onChange.accept(s);
        });
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, Rect rect) {
        this.rect = rect;
        hovered = false;
        if (!this.isVisible()) return;
        hovered = rect.in(mouseX, mouseY);
        if (hovered && !this.disabled) {
            this.drawRect(graphics, rect.x, rect.y, rect.width, rect.height, FastColor.ARGB32.color(120, 0, 0, 0));
            this.renderHovered(graphics, rect);
        }
        int color = disabled ? this.style.textDisabledColor : this.style.textColor;
        this.drawString(graphics, this.label, rect.x + 4, rect.getCenterY() - 4, color);
        editBox.setPosition((int) (rect.width - (rect.width * 0.5) - 6), rect.y + 2);
        #if MC_VER > MC_1_20_1
        editBox.setSize((int) (rect.width * 0.5), rect.height - 4);
        #else
        editBox.setWidth((int) (rect.width * 0.5));
        ((AbstractWidgetAccessor) editBox).setHeight(rect.height - 4);
        #endif
        editBox.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public String getValue() {
        return this.value;
    }

    @Override
    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isVisibleAndNotDisabled()) return false;
        this.editBox.setFocused(rect.in((int) mouseX, (int) mouseY) && isVisibleAndNotDisabled());
        return this.editBox.mouseClicked(mouseX, mouseY, button);
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
        return this.editBox.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    #if MC_VER > MC_1_20_1
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY)
    #else
    public boolean mouseScrolled(double mouseX, double mouseY, double delta)
    #endif {
        if (!isVisibleAndNotDisabled()) return false;
        #if MC_VER > MC_1_20_1
        return this.editBox.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        #else
        return this.editBox.mouseScrolled(mouseX, mouseY, delta);
        #endif
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!isVisibleAndNotDisabled()) return false;
        return this.editBox.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (!isVisibleAndNotDisabled()) return;
        this.editBox.mouseMoved(mouseX, mouseY);
    }

    @Override
    public @NotNull ScreenRectangle getRectangle() {
        return new ScreenRectangle(rect.x, rect.y, rect.width, rect.height);
    }
}
