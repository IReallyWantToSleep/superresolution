package io.homo.superresolution.common.gui.widgets;

import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2i;
import org.joml.Vector2ic;

public class TooltipPositioner implements ClientTooltipPositioner {
    private final AbstractWidget widget;

    public TooltipPositioner(AbstractWidget widget) {
        this.widget = widget;
    }

    private static int getOffset(int mouseY, int widgetY, int widgetHeight) {
        int i = Math.min(Math.abs(mouseY - widgetY), widgetHeight);
        return Math.round(Mth.lerp((float) i / (float) widgetHeight, (float) (widgetHeight - 3), 5.0F));
    }

    @Override
    public @NotNull Vector2ic positionTooltip(int screenWidth, int screenHeight, int mouseX, int mouseY, int tooltipWidth, int tooltipHeight) {
        Vector2i vector2i = new Vector2i(mouseX + 12, mouseY);
        if (vector2i.x + tooltipWidth > screenWidth - 5) {
            vector2i.x = Math.max(mouseX - 12 - tooltipWidth, 9);
        }

        vector2i.y += 3;
        int i = tooltipHeight + 3 + 3;
        int j = this.widget.rect.y + this.widget.rect.height + 3 + getOffset(0, 0, this.widget.rect.height);
        int k = screenHeight - 5;
        if (j + i <= k) {
            vector2i.y += getOffset(vector2i.y, this.widget.rect.y, this.widget.rect.height);
        } else {
            vector2i.y -= i + getOffset(vector2i.y, this.widget.rect.y + this.widget.rect.height, this.widget.rect.height);
        }

        return vector2i;
    }
}
