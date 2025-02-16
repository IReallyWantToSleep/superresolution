package io.homo.superresolution.common.gui.widgets;

import io.homo.superresolution.common.gui.Rect;
import net.minecraft.client.InputType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Tooltip;
#if MC_VER > MC_1_20_1
import net.minecraft.client.gui.components.WidgetTooltipHolder;
#endif
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractWidget implements Renderable, GuiEventListener, NarratableEntry {
    protected final Font font;
    #if MC_VER > MC_1_20_1
    private final WidgetTooltipHolder tooltip = new WidgetTooltipHolder();
    #else
    private final WidgetTooltip tooltip = new WidgetTooltip(this);
    #endif
    protected boolean focused;
    protected boolean hovered;
    protected Rect rect;

    protected AbstractWidget() {
        this.font = Minecraft.getInstance().font;
    }

    public void setRect(Rect rect) {
        this.rect = rect;
    }

    public boolean isHovered() {
        return this.hovered;
    }

    protected void playClickSound() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI((SoundEvent) SoundEvents.UI_BUTTON_CLICK.value(), 1.0F));
    }

    public NarratableEntry.@NotNull NarrationPriority narrationPriority() {
        if (this.focused) {
            return NarrationPriority.FOCUSED;
        } else {
            return this.hovered ? NarrationPriority.HOVERED : NarrationPriority.NONE;
        }
    }

    public void updateNarration(NarrationElementOutput builder) {
        if (this.focused) {
            builder.add(NarratedElementType.USAGE, Component.translatable("narration.button.usage.focused"));
        } else if (this.hovered) {
            builder.add(NarratedElementType.USAGE, Component.translatable("narration.button.usage.hovered"));
        }

    }

    public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent event) {
        return !this.isFocused() ? ComponentPath.leaf(this) : null;
    }

    public boolean isFocused() {
        return this.focused;
    }

    public void setFocused(boolean focused) {
        if (!focused) {
            this.focused = false;
        } else {
            InputType inputType = Minecraft.getInstance().getLastInputType();
            if (inputType == InputType.KEYBOARD_TAB || inputType == InputType.KEYBOARD_ARROW) {
                this.focused = true;
            }
        }
    }

    protected void drawString(GuiGraphics graphics, String text, int x, int y, int color) {
        graphics.drawString(this.font, text, x, y, color);
    }

    protected void drawString(GuiGraphics graphics, Component text, int x, int y, int color) {
        graphics.drawString(this.font, text, x, y, color);
    }

    protected void drawRect(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, width, height, color);
    }

    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        #if MC_VER > MC_1_20_1
        this.tooltip.refreshTooltipForNextRenderPass(this.isHovered(), this.isFocused(), this.getRectangle());
        #else
        if ((hovered || focused) && this.tooltip.getTooltip() != null) {
            graphics.renderTooltip(this.font,
                    this.tooltip.getTooltip().toCharSequence(Minecraft.getInstance()),
                    mouseX, mouseY
            );
        }
        #endif
    }

    public AbstractWidget setTooltip(Tooltip tooltip) {
        #if MC_VER > MC_1_20_1
        this.tooltip.set(tooltip);
        #else
        this.tooltip.setTooltip(tooltip);
        #endif
        return this;
    }
}
