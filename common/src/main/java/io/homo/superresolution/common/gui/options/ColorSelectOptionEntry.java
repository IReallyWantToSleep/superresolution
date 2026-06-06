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

package io.homo.superresolution.common.gui.options;

import io.homo.superresolution.common.gui.impl.Text;
import io.homo.superresolution.core.gui.core.AbstractWidget;
import io.homo.superresolution.core.gui.core.ContainerWidget;
import io.homo.superresolution.core.gui.core.UIInputState;
import io.homo.superresolution.core.gui.core.backends.render.RenderContext;
import io.homo.superresolution.core.gui.core.impl.Rectangle;
import io.homo.superresolution.core.gui.widgets.label.MaterialLabel;
import io.homo.superresolution.core.gui.widgets.sliders.MaterialSlider;
import io.homo.superresolution.core.gui.widgets.sliders.MaterialSliderSize;
import io.homo.superresolution.core.utils.Color;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaAlign;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaEdge;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaFlexDirection;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaGutter;

import java.util.function.Consumer;

public class ColorSelectOptionEntry extends AbstractOptionEntry<Color, ColorSelectOptionEntry> {
    private static final float SLIDER_WIDTH = 250f;
    protected MaterialSlider redSlider;
    protected MaterialSlider greenSlider;
    protected MaterialSlider blueSlider;
    protected MaterialLabel redValueLabel;
    protected MaterialLabel greenValueLabel;
    protected MaterialLabel blueValueLabel;
    protected ColorPanelWidget colorPanelWidget;
    protected ContainerWidget sliderContainer;
    protected ContainerWidget contentContainer;

    protected Consumer<Color> valueChangeListener = (value) -> {
    };

    private boolean suppressSliderChangeEvent = false;

    public ColorSelectOptionEntry(
            Text name,
            Color value
    ) {
        super(name, value);
    }

    @Override
    protected void init() {
        this.container = new OptionContainerWidget(this);
        initLayout();
        initWidget();
    }

    @Override
    protected void initLayout() {
    }

    @Override
    protected void initWidget() {
        sliderContainer = new ContainerWidget();
        sliderContainer.layout().setFlexDirection(YogaFlexDirection.COLUMN);
        sliderContainer.layout().setAlignItems(YogaAlign.CENTER);
        sliderContainer.layout().setGap(YogaGutter.ALL, 6);
        contentContainer = new ContainerWidget();
        contentContainer.layout().setFlexDirection(YogaFlexDirection.ROW);
        contentContainer.layout().setHeightPercent(100);

        ContainerWidget redContainer = new ContainerWidget();
        redContainer.layout().setFlexDirection(YogaFlexDirection.ROW);
        redContainer.layout().setAlignItems(YogaAlign.CENTER);
        redContainer.layout().setGap(YogaGutter.ALL, 8);
        redValueLabel = MaterialLabel.create()
                .text(() -> "R:%s".formatted(formatValue(redSlider.value())))
                .fontSize(14);
        redValueLabel.layout().setMinWidth(50);

        redSlider = MaterialSlider.create(MaterialSliderSize.ExtraSmall, SLIDER_WIDTH);
        redSlider.style().valueIndicator(false);
        redSlider.setMin(0);
        redSlider.setMax(255);
        redSlider.setValue((Number) (double) value.red());
        redSlider.onInput(event -> {
            valueChangeListener.accept(value());
        });
        redSlider.onChange(event -> {
            if (suppressSliderChangeEvent) {
                return;
            }
            Number oldChannelValue = (Number) event.getOldValue();
            Color oldColor = this.value.copy();
            this.value.red(((Number) event.getNewValue()).intValue());
            if (saveConsumer != null) {
                if (!saveConsumer.apply(this.value)) {
                    redSlider.setValue(oldChannelValue);
                    this.value = oldColor;
                    return;
                }
            }
            if (saveRunnable != null) {
                saveRunnable.run();
            }
        });

        redContainer.addChild(redValueLabel);
        redContainer.addChild(redSlider);
        sliderContainer.addChild(redContainer);

        ContainerWidget greenContainer = new ContainerWidget();
        greenContainer.layout().setFlexDirection(YogaFlexDirection.ROW);
        greenContainer.layout().setAlignItems(YogaAlign.CENTER);
        greenContainer.layout().setGap(YogaGutter.ALL, 8);
        greenValueLabel = MaterialLabel.create()
                .text(() -> "G:%s".formatted(formatValue(greenSlider.value())))
                .fontSize(14);
        greenValueLabel.layout().setMinWidth(50);

        greenSlider = MaterialSlider.create(MaterialSliderSize.ExtraSmall, SLIDER_WIDTH);
        greenSlider.style().valueIndicator(false);
        greenSlider.setMin(0);
        greenSlider.setMax(255);
        greenSlider.setValue((Number) (double) value.green());
        greenSlider.onInput(event -> {
            valueChangeListener.accept(value());
        });
        greenSlider.onChange(event -> {
            if (suppressSliderChangeEvent) {
                return;
            }
            Number oldChannelValue = (Number) event.getOldValue();
            Color oldColor = this.value.copy();
            this.value.green(((Number) event.getNewValue()).intValue());
            if (saveConsumer != null) {
                if (!saveConsumer.apply(this.value)) {
                    greenSlider.setValue(oldChannelValue);
                    this.value = oldColor;
                    return;
                }
            }
            if (saveRunnable != null) {
                saveRunnable.run();
            }
        });

        greenContainer.addChild(greenValueLabel);
        greenContainer.addChild(greenSlider);
        sliderContainer.addChild(greenContainer);

        ContainerWidget blueContainer = new ContainerWidget();
        blueContainer.layout().setFlexDirection(YogaFlexDirection.ROW);
        blueContainer.layout().setAlignItems(YogaAlign.CENTER);
        blueContainer.layout().setGap(YogaGutter.ALL, 8);

        blueValueLabel = MaterialLabel.create()
                .text(() -> "B:%s".formatted(formatValue(blueSlider.value())))
                .fontSize(14);
        blueValueLabel.layout().setMinWidth(50);

        blueSlider = MaterialSlider.create(MaterialSliderSize.ExtraSmall, SLIDER_WIDTH);
        blueSlider.style().valueIndicator(false);
        blueSlider.setMin(0);
        blueSlider.setMax(255);
        blueSlider.setValue((Number) (double) value.blue());
        blueSlider.onInput(event -> {
            valueChangeListener.accept(value());
        });
        blueSlider.onChange(event -> {
            if (suppressSliderChangeEvent) {
                return;
            }
            Number oldChannelValue = (Number) event.getOldValue();
            Color oldColor = this.value.copy();
            this.value.blue(((Number) event.getNewValue()).intValue());
            if (saveConsumer != null) {
                if (!saveConsumer.apply(this.value)) {
                    blueSlider.setValue(oldChannelValue);
                    this.value = oldColor;
                    return;
                }
            }
            if (saveRunnable != null) {
                saveRunnable.run();
            }
        });

        blueContainer.addChild(blueValueLabel);
        blueContainer.addChild(blueSlider);
        sliderContainer.addChild(blueContainer);

        colorPanelWidget = new ColorPanelWidget();
        colorPanelWidget.layout().setHeightPercent(100);
        colorPanelWidget.layout().setWidth(48);
        colorPanelWidget.layout().setMargin(YogaEdge.RIGHT, 12);
        contentContainer.addChild(colorPanelWidget);
        contentContainer.addChild(sliderContainer);
        contentContainer.setTooltipSupplier(this::resolveTooltip);
        this.container.addChild(contentContainer);
    }

    @Override
    public Color value() {
        return Color.rgba(
                redSlider.value().intValue(),
                greenSlider.value().intValue(),
                blueSlider.value().intValue(),
                255
        );
    }

    @Override
    public void tick(RenderContext ctx) {
        boolean enabled = updateRequirements();
        redSlider.setDisabled(!enabled);
        greenSlider.setDisabled(!enabled);
        blueSlider.setDisabled(!enabled);
    }

    private String formatValue(Number value) {
        return Integer.toString(value.intValue());
    }

    public ColorSelectOptionEntry setCurrentValue(Color value) {
        if (redSlider == null || greenSlider == null || blueSlider == null) {
            this.value = value;
            return this;
        }
        suppressSliderChangeEvent = true;
        try {
            redSlider.setValue((Number) (double) value.red());
            greenSlider.setValue((Number) (double) value.green());
            blueSlider.setValue((Number) (double) value.blue());
            this.value = value;
        } finally {
            suppressSliderChangeEvent = false;
        }
        return this;
    }

    public class ColorPanelWidget extends AbstractWidget<ColorPanelWidget> {

        @Override
        protected void init() {

        }

        @Override
        public void render(RenderContext ctx, UIInputState inputState) {
            Rectangle bounds = getBounds();
            ctx.roundedRect(
                    bounds.x,
                    bounds.y,
                    bounds.width,
                    bounds.height,
                    4,
                    value(),
                    true
            );
        }
    }
}