package io.homo.superresolution.common.gui.entries;

import io.homo.superresolution.core.utils.ColorUtil;
import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ClothChartEntry extends TooltipListEntry<Object> {
    public ClothChartEntry setName(String name) {
        this.name = name;
        return this;
    }

    private String name;
    private final Deque<Float> data;
    private float min = Float.MAX_VALUE;
    private float max = Float.MIN_VALUE;
    private float sum = 0;
    private float displayMax = 100;
    private float displayMin = 0;
    private Consumer<ClothChartEntry> renderCallback = null;

    public ClothChartEntry(String name, Component fieldName, @Nullable Supplier<Optional<Component[]>> tooltipSupplier) {
        super(fieldName, tooltipSupplier);
        this.name = name;
        this.data = new ArrayDeque<>();
    }

    public void setDisplayRange(float min, float max) {
        this.displayMin = min;
        this.displayMax = max;
    }

    public void setRenderCallback(Consumer<ClothChartEntry> callback) {
        this.renderCallback = callback;
    }

    public void push(float value, int maxDataPoints) {
        data.addLast(value);
        sum += value;
        if (value > this.max) this.max = value;
        if (value < this.min) this.min = value;
        if (data.size() > maxDataPoints) {
            float removed = data.removeFirst();
            sum -= removed;
            if (removed == this.min || removed == this.max) {
                recalculateMinMax();
            }
        }
    }

    private void recalculateMinMax() {
        if (data.isEmpty()) {
            min = Float.MAX_VALUE;
            max = Float.MIN_VALUE;
            return;
        }
        min = Float.MAX_VALUE;
        max = Float.MIN_VALUE;
        for (float v : data) {
            if (v > max) max = v;
            if (v < min) min = v;
        }
    }

    public int getItemHeight() {
        return 100;
    }

    @Override
    public void render(GuiGraphics graphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta) {
        super.render(graphics, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
        Font font = Minecraft.getInstance().font;

        graphics.drawCenteredString(font, name, x + entryWidth / 2, y + 2, ColorUtil.color(255, 255, 255, 255));

        if (!data.isEmpty()) {
            float avg = sum / data.size();
            String stats = String.format("AVG: %.2f MAX: %.2f MIN: %.2f", avg, max, min);
            graphics.drawCenteredString(font, stats, x + entryWidth / 2, y + 12, ColorUtil.color(255, 255, 255, 255));

            int chartHeight = entryHeight - 24;
            int chartY = y + 24;
            int chartX = x + 4;
            int chartWidth = entryWidth - 8;
            int maxDataPoints = chartWidth;

            while (data.size() > maxDataPoints) {
                float removed = data.removeFirst();
                sum -= removed;
                if (removed == this.min || removed == this.max) {
                    recalculateMinMax();
                }
            }

            float range = (displayMax - displayMin) == 0 ? 1 : (displayMax - displayMin);
            float scaleY = chartHeight / range;

            int i = 0;
            for (float v : data) {
                int barX = chartX + i;
                int barHeight = (int) ((clamp(v) - displayMin) * scaleY);
                int barY = chartY + chartHeight - barHeight;
                graphics.fill(
                        barX,
                        barY,
                        barX + 1,
                        chartY + chartHeight,
                        i % 2 == 0 ? ColorUtil.color(255, 255, 0, 0) : ColorUtil.color(100, 255, 0, 0)
                );
                i++;
            }

            int avgY = chartY + chartHeight - (int) ((clamp(avg) - displayMin) * scaleY);
            graphics.hLine(chartX, chartX + chartWidth, avgY, 0xFF0000FF);
        }

        if (renderCallback != null) {
            renderCallback.accept(this);
        }
    }

    private float clamp(float v) {
        if (v < displayMin) return displayMin;
        if (v > displayMax) return displayMax;
        return v;
    }

    @Override
    public Optional<Object> getDefaultValue() {
        return Optional.empty();
    }

    @Override
    public Object getValue() {
        return null;
    }

    @Override
    public @NotNull List<? extends GuiEventListener> children() {
        return Collections.emptyList();
    }

    @Override
    public List<? extends NarratableEntry> narratables() {
        return Collections.emptyList();
    }
}
