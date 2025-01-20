package io.homo.superresolution.gui.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import io.homo.superresolution.gui.Rect;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.util.FastColor;

import java.util.Date;

public class ButtonWidget extends AbstractWidget implements Renderable {
    private boolean enabled = true;
    private boolean visible = true;
    private Rect rect;
    private ButtonStyle style = ButtonStyle.defaults();
    private Runnable action;
    private String label = "";

    public ButtonWidget(int x,int y,int width,int height){
        this.rect = new Rect(x,y,width,height);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (this.visible) {
            RenderSystem.enableBlend();
            this.hovered = this.rect.in(mouseX, mouseY);

            int backgroundColor = this.enabled ? (this.hovered ? this.style.bgHovered : this.style.bgDefault) : this.style.bgDisabled;
            int textColor = this.enabled ? this.style.textDefault : this.style.textDisabled;
            int strWidth = this.font.width(this.label);
            this.drawRect(graphics,
                    this.rect.x,
                    this.rect.y,
                    this.rect.getLimitX(),
                    this.rect.getLimitY(),
                    backgroundColor
            );
            if (hovered)this.drawRect(
                    graphics,
                    this.rect.x,
                    this.rect.getLimitY() - 1,
                    this.rect.getLimitX(),
                    this.rect.getLimitY(),
                    FastColor.ARGB32.color(255, 148,228,211)
            );
            this.drawString(
                    graphics,
                    this.label,
                    this.rect.getCenterX() - strWidth / 2,
                    this.rect.getCenterY() - 4,
                    textColor
            );
            RenderSystem.disableBlend();
        }
    }
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.enabled && this.visible) {
            if (button == 0 && this.rect.in(mouseX, mouseY)) {
                this.click();
                this.playClickSound();
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public ButtonWidget setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public ButtonWidget setVisible(boolean visible) {
        this.visible = visible;
        return this;
    }
    private void click(){
        if (this.action != null){
            this.action.run();
        }
    }

    public ButtonWidget setAction(Runnable action) {
        this.action = action;
        return this;
    }

    public ButtonWidget setStyle(ButtonStyle style) {
        this.style = style;
        return this;
    }

    public ButtonWidget setLabel(String label) {
        this.label = label;
        return this;
    }

    public static class ButtonStyle{
        public int bgHovered;
        public int bgDefault;
        public int bgDisabled;
        public int textDefault;
        public int textDisabled;

        public ButtonStyle() {}
        public static ButtonStyle defaults() {
            ButtonStyle style = new ButtonStyle();
            style.bgHovered = FastColor.ARGB32.color(120,0,0,0);
            style.bgDefault = FastColor.ARGB32.color(60,0,0,0);
            style.bgDisabled = FastColor.ARGB32.color(40,0,0,0);
            style.textDefault = FastColor.ARGB32.color(255,255,255,255);
            style.textDisabled = FastColor.ARGB32.color(100,255,255,255);
            return style;
        }
    }
}
