package io.homo.superresolution.common.gui.entries;

import io.homo.superresolution.core.math.Vector2f;
import io.homo.superresolution.common.mixin.gui.AbstractWidgetAccessor;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class ClothButtonEntry extends AbstractConfigListEntry<Boolean> {
    private final Button button;
    private boolean fullSize;

    public ClothButtonEntry(Component name, Button.OnPress onPress, Vector2f size) {
        super(name, false);
        button = Button.builder(name, onPress)
                .bounds(0, 0, (int) size.x, (int) size.y).build();
        this.fullSize = true;
    }

    public ClothButtonEntry(Component name, Button.OnPress onPress, boolean fullSize) {
        super(name, false);
        this.fullSize = fullSize;
        button = Button.builder(name, onPress)
                .bounds(0, 0, 100, 20).build();
    }

    public ClothButtonEntry setFullSize(boolean fullSize) {
        this.fullSize = fullSize;
        return this;
    }

    @Override
    @NotNull
    public List<? extends GuiEventListener> children() {
        return List.of(button);
    }


    public boolean isDragging() {
        return false;
    }

    public void setDragging(boolean b) {

    }

    public @Nullable GuiEventListener getFocused() {
        return null;
    }

    public void setFocused(@Nullable GuiEventListener guiEventListener) {

    }

    @Override
    public List<? extends NarratableEntry> narratables() {
        return List.of(button);
    }

    @Override
    public Boolean getValue() {
        return true;
    }

    @Override
    public Optional<Boolean> getDefaultValue() {
        return Optional.of(true);
    }

    @Override
    public void render(GuiGraphics graphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta) {
        button.setWidth(entryWidth);
        #if MC_VER > MC_1_20_1
        button.setHeight(entryHeight);
        #else
        ((AbstractWidgetAccessor) button).setHeight_(entryHeight);
        #endif
        button.setX(x + ((entryWidth - button.getWidth()) / 2));
        button.setY(y);
        button.render(graphics, mouseX, mouseY, delta);
        super.render(graphics, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
    }

    @Override
    public NarrationPriority narrationPriority() {
        return null;
    }

    @Override
    public void updateNarration(NarrationElementOutput narrationElementOutput) {

    }
}
