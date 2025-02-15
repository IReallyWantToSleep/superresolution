package io.homo.superresolution.common.gui.options;

import io.homo.superresolution.common.gui.Rect;
import io.homo.superresolution.common.gui.options.option.AbstractOption;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.function.Consumer;


public class OptionsList implements Renderable, GuiEventListener, NarratableEntry {
    public ArrayList<AbstractOption<?>> options = new ArrayList<>();
    public Rect rect;
    protected boolean focused;
    protected boolean hovered;
    protected Minecraft minecraft = Minecraft.getInstance();
    protected Font font = minecraft.font;
    protected Consumer<HashMap<String, Object>> onSaving;

    public OptionsList(int x, int y, int width, int height) {
        this.rect = new Rect(x, y, width, height);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        for (AbstractOption<?> option : options) {
            int optionIndex = options.indexOf(option);
            Rect optionRect = getOptionRect(optionIndex);
            option.render(guiGraphics, mouseX, mouseY, partialTick, optionRect);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (AbstractOption<?> option : options) {
            option.mouseClicked(mouseX, mouseY, button);
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        for (AbstractOption<?> option : options) {
            option.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }

        return true;
    }

    @Override
    public boolean isFocused() {
        return false;
    }

    @Override
    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    @Override
    public @NotNull ScreenRectangle getRectangle() {
        return new ScreenRectangle(this.rect.x, this.rect.y, this.rect.width, this.rect.height);
    }

    public void setPosition(int x, int y) {
        this.rect.x = x;
        this.rect.y = y;
    }

    public void resize(int width, int height) {
        this.rect.width = width;
        this.rect.height = height;
    }

    @Override
    public NarratableEntry.@NotNull NarrationPriority narrationPriority() {
        if (this.focused) {
            return NarrationPriority.FOCUSED;
        } else {
            return this.hovered ? NarrationPriority.HOVERED : NarrationPriority.NONE;
        }
    }

    @Override
    public void updateNarration(@NotNull NarrationElementOutput narrationElementOutput) {
    }

    public void addOption(AbstractOption<?> option) {
        option.setFont(this.font);
        option.setMinecraft(this.minecraft);
        this.options.add(option);
    }

    public AbstractOption<?> getOption(String key) {
        for (AbstractOption<?> option : options) {
            if (Objects.equals(option.getKey(), key)) {
                return option;
            }
        }
        return null;
    }

    protected Rect getOptionRect(int index) {
        return new Rect(
                this.rect.x + 4,
                this.rect.y + (index * 26) + 4,
                this.rect.width - 4,
                22
        );
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        for (AbstractOption<?> option : options) {
            option.charTyped(codePoint, modifiers);
        }
        return true;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        for (AbstractOption<?> option : options) {
            option.keyReleased(keyCode, scanCode, modifiers);
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (AbstractOption<?> option : options) {
            option.keyPressed(keyCode, scanCode, modifiers);
        }
        return true;
    }

    @Override
    #if MC_VER > MC_1_20_1
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY)
    #else
    public boolean mouseScrolled(double mouseX, double mouseY, double delta)
    #endif {
        for (AbstractOption<?> option : options) {
            #if MC_VER > MC_1_20_1
            option.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
            #else
            option.mouseScrolled(mouseX, mouseY, delta);
            #endif
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        for (AbstractOption<?> option : options) {
            option.mouseReleased(mouseX, mouseY, button);
        }
        return true;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        for (AbstractOption<?> option : options) {
            option.mouseMoved(mouseX, mouseY);
        }
    }

    public HashMap<String, Object> getData() {
        HashMap<String, Object> map = new HashMap<>();
        for (AbstractOption<?> option : options) {
            map.put(option.getKey(), option.getValue());
        }
        return map;
    }

    public void setSavingRunnable(Consumer<HashMap<String, Object>> f) {
        this.onSaving = f;
    }

    public void save() {
        if (this.onSaving != null) {
            this.onSaving.accept(this.getData());
        }
    }
}
