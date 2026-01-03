/*
 * Super Resolution
 * Copyright (c) 2025-2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.homo.superresolution.common.gui.entries;

import me.shedaniel.clothconfig2.gui.AbstractConfigScreen;
import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

public class ClothTextListEntry extends TooltipListEntry<Object> {
    public static final int LINE_HEIGHT = 12;
    public static final int DISABLED_COLOR;

    static {
        DISABLED_COLOR = Objects.requireNonNull(ChatFormatting.DARK_GRAY.getColor());
    }

    protected final Font textRenderer;
    protected final int color;
    protected final Supplier<Component> textSupplier;
    protected int savedWidth;
    protected int savedX;
    protected int savedY;
    protected List<FormattedCharSequence> wrappedLines;

    public ClothTextListEntry(Component fieldName, Supplier<Component> textSupplier, int color, Supplier<Optional<Component[]>> tooltipSupplier) {
        super(fieldName, tooltipSupplier);
        this.textRenderer = Minecraft.getInstance().font;
        this.textSupplier = textSupplier;
        this.color = color;
        this.wrappedLines = Collections.emptyList();
    }

    public void render(GuiGraphics graphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta) {
        super.render(graphics, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
        this.wrappedLines = this.textRenderer.split(this.textSupplier.get(), entryWidth);
        this.savedWidth = entryWidth;
        this.savedX = x;
        this.savedY = y;
        int yy = y + 7;
        int textColor = this.isEnabled() ? this.color : DISABLED_COLOR;

        for (FormattedCharSequence string : this.wrappedLines) {
            graphics.drawString(Minecraft.getInstance().font, string, x, yy, textColor);
            Objects.requireNonNull(Minecraft.getInstance().font);
            yy += 9 + 3;
        }

        Style style = this.getTextAt(mouseX, mouseY);
        AbstractConfigScreen configScreen = this.getConfigScreen();
        if (style != null && configScreen != null) {
            graphics.renderComponentHoverEffect(Minecraft.getInstance().font, style, mouseX, mouseY);
        }

    }

    public int getItemHeight() {
        int lineCount = this.wrappedLines.size();
        return lineCount == 0 ? 0 : 14 + lineCount * 12;
    }

    #if MC_VER > MC_1_21_6 && false
    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean idkThis) {
        if (event.button() == 0) {
            Style style = this.getTextAt(event.x(), event.y());
            AbstractConfigScreen configScreen = this.getConfigScreen();
            if (configScreen != null && configScreen.handleComponentClicked(style)) {
                return true;
            }
        }

        return super.mouseClicked(event, idkThis);
    }

    #else
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            Style style = this.getTextAt(mouseX, mouseY);
            AbstractConfigScreen configScreen = this.getConfigScreen();
            if (configScreen != null && configScreen.handleComponentClicked(style)) {
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }
    #endif


    protected @Nullable Style getTextAt(double x, double y) {
        int lineCount = this.wrappedLines.size();
        if (lineCount > 0) {
            int textX = Mth.floor(x - (double) this.savedX);
            int textY = Mth.floor(y - (double) 7.0F - (double) this.savedY);
            if (textX >= 0 && textY >= 0 && textX <= this.savedWidth && textY < 12 * lineCount + lineCount) {
                int line = textY / 12;
                if (line < this.wrappedLines.size()) {
                    FormattedCharSequence orderedText = this.wrappedLines.get(line);
                    return this.textRenderer.getSplitter().componentStyleAtWidth(orderedText, textX);
                }
            }
        }

        return null;
    }

    public Object getValue() {
        return null;
    }

    public Optional<Object> getDefaultValue() {
        return Optional.empty();
    }

    public @NotNull List<? extends GuiEventListener> children() {
        return Collections.emptyList();
    }

    public List<? extends NarratableEntry> narratables() {
        return Collections.emptyList();
    }
}
