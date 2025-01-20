package io.homo.superresolution.gui.options.option;

import io.homo.superresolution.gui.Rect;
import io.homo.superresolution.gui.options.OptionStyle;
import io.homo.superresolution.gui.widgets.AbstractWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.function.Consumer;


public abstract class AbstractOption<T> extends AbstractWidget {
    protected Consumer<T> onChange = (s) -> {
    };
    protected boolean disabled = false;
    protected boolean visible = true;
    protected String label = "";
    protected String key = "";
    protected OptionStyle style = OptionStyle.defaultStyle();
    protected Font font = Minecraft.getInstance().font;
    protected Minecraft minecraft = Minecraft.getInstance();

    public String getKey() {
        return key;
    }

    public AbstractOption<T> setKey(String key) {
        this.key = key;
        return this;
    }

    public abstract void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, Rect rect);

    @Deprecated
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    }

    public abstract T getValue();

    public abstract void setValue(T value);

    public AbstractOption<T> setOnChange(Consumer<T> onChange) {
        this.onChange = onChange;
        return this;
    }

    @Override
    protected void drawRect(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, width, height + y, color);
    }

    public OptionStyle getStyle() {
        return style;
    }

    public void setStyle(OptionStyle style) {
        this.style = style;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public AbstractOption<T> setDisabled(boolean disabled) {
        this.disabled = disabled;
        return this;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public void setFont(Font font) {
        this.font = font;
    }

    public void setMinecraft(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    public String getLabel() {
        return label;
    }

    public AbstractOption<T> setLabel(String label) {
        this.label = label;
        return this;
    }

    public void renderHovered(GuiGraphics graphics, Rect rect) {
        if (!this.disabled) graphics.fill(
                rect.x,
                rect.y + rect.height - 1,
                rect.width,
                rect.y + rect.height,
                this.style.hoveredColor
        );
    }

    public boolean isVisibleAndNotDisabled() {
        return this.visible && !this.disabled;
    }
}
