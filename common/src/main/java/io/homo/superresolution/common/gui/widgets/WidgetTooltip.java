package io.homo.superresolution.common.gui.widgets;

import net.minecraft.client.gui.components.Tooltip;

public class WidgetTooltip {
    private final AbstractWidget widget;
    private final TooltipPositioner positioner;
    private Tooltip tooltip;

    public WidgetTooltip(AbstractWidget widget) {
        this.widget = widget;
        this.positioner = new TooltipPositioner(widget);
    }

    public Tooltip getTooltip() {
        return tooltip;
    }

    public void setTooltip(Tooltip tooltip) {
        this.tooltip = tooltip;
    }

    public TooltipPositioner getPositioner() {
        return positioner;
    }
}
