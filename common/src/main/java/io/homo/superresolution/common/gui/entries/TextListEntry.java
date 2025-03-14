package io.homo.superresolution.common.gui.entries;

import io.homo.superresolution.common.gui.InfoBuilder;
import io.homo.superresolution.common.gui.widgets.Line;
import io.homo.superresolution.common.utils.ColorUtil;
import me.shedaniel.clothconfig2.gui.entries.BaseListCell;
import me.shedaniel.clothconfig2.gui.entries.BaseListEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class TextListEntry extends BaseListEntry<String, TextListEntry.TextListCell, TextListEntry> implements InfoBuilder.LineContainer {


    public TextListEntry(Component fieldName, Supplier<Optional<Component[]>> tooltipSupplier) {
        super(fieldName, tooltipSupplier, List::of, (_a) -> null, (a) -> {
        }, Component.empty(), false, false, false);
        this.insertButtonEnabled = false;
        this.resetWidget = new Button(0, 0, 1, 1, Component.empty(), (a) -> {
        }, Supplier::get) {
            @Override
            protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            }

            @Override
            public void renderString(GuiGraphics guiGraphics, Font font, int color) {
            }
        };
    }


    @Override
    public TextListEntry self() {
        return this;
    }

    @Override
    protected TextListCell getFromValue(String s) {
        return null;
    }


    @Override
    public List<String> getValue() {
        return List.of();
    }

    public void addLine(Line line) {
        this.cells.add(new TextListCell(line));
    }

    @Override
    public void render(GuiGraphics graphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta) {
        super.render(graphics, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
        int offsetX = -20;
        int offsetY = 0;

        if (!this.isExpanded()) {
            graphics.fillGradient(x + offsetX, y + offsetY, x + offsetX + 2, y + entryHeight, ColorUtil.color(255, 0, 255, 255), ColorUtil.color(255, 0, 0, 255));
        } else {
            graphics.fillGradient(x + offsetX, y + offsetY, x + offsetX + 2, y + entryHeight, ColorUtil.color(255, 0, 255, 255), ColorUtil.color(255, 0, 0, 255));
        }
    }

    public static class TextListCell extends BaseListCell {
        private final Line line;
        private final Font textRenderer;

        public TextListCell(Line line) {
            this.line = line;
            this.textRenderer = Minecraft.getInstance().font;
        }

        @Override
        public Optional<Component> getError() {
            return Optional.empty();
        }

        @Override
        public int getCellHeight() {
            return line.height(textRenderer);
        }

        @Override
        public void render(GuiGraphics graphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta) {
            x -= 14;
            entryWidth += 14;
            graphics.drawString(textRenderer, line.text, x, y, line.color);
        }

        @Override
        public @NotNull List<? extends GuiEventListener> children() {
            return List.of();
        }

        @Override
        public @NotNull NarrationPriority narrationPriority() {
            return NarrationPriority.NONE;
        }

        @Override
        public void updateNarration(@NotNull NarrationElementOutput narrationElementOutput) {

        }

    }
}
