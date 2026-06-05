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
import io.homo.superresolution.core.gui.core.ContainerWidget;
import io.homo.superresolution.core.gui.core.backends.render.RenderContext;
import io.homo.superresolution.core.gui.widgets.label.MaterialLabel;
import io.homo.superresolution.core.gui.widgets.sliders.MaterialSlider;
import io.homo.superresolution.core.gui.widgets.sliders.MaterialSliderSize;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaAlign;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaFlexDirection;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaGutter;

import java.util.function.Consumer;
import java.util.function.Function;

public class NumberSliderOptionEntry extends AbstractOptionEntry<Number, NumberSliderOptionEntry> {
    private static final float SLIDER_WIDTH = 250f;
    protected MaterialSlider slider;
    protected MaterialLabel valueLabel;
    protected ContainerWidget sliderContainer;
    protected Number max;
    protected Number min;
    protected Number step;
    protected Function<Number, String> valueFormater;
    protected Consumer<Number> valueChangeListener = (value) -> {
    };
    private boolean suppressSliderChangeEvent = false;

    public NumberSliderOptionEntry(
            Text name,
            Number value,
            Number max,
            Number min
    ) {
        super(name, value);
        this.max = max;
        this.min = min;
        init();
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
        sliderContainer.layout().setFlexDirection(YogaFlexDirection.ROW);
        sliderContainer.layout().setAlignItems(YogaAlign.CENTER);
        sliderContainer.layout().setGap(YogaGutter.ALL, 12);

        valueLabel = MaterialLabel.create()
                .text(() -> formatValue(slider.value()))
                .fontSize(14);
        valueLabel.layout().setMinWidth(50);

        slider = MaterialSlider.create(MaterialSliderSize.Small, SLIDER_WIDTH);
        slider.style().valueIndicator(true);
        slider.setMin(min);
        slider.setMax(max);
        slider.setValue(value);
        slider.onInput(inputEvent -> {
            valueChangeListener.accept((Number) inputEvent.getNewValue());
        });
        slider.onChange(event -> {
            this.value = (Number) event.getNewValue();
            if (suppressSliderChangeEvent) {
                return;
            }
            if (saveConsumer != null) {
                if (!saveConsumer.apply(this.value)) {
                    slider.setValue((Number) event.getOldValue());
                    this.value = (Number) event.getOldValue();
                }
            }
            if (saveRunnable != null) {
                saveRunnable.run();
            }
        });

        sliderContainer.addChild(valueLabel);
        sliderContainer.addChild(slider);
        slider.setTooltipSupplier(this::resolveTooltip);

        container.addControl(sliderContainer);
    }

    @Override
    public Number value() {
        return slider != null ? slider.value() : value;
    }

    @Override
    public void tick(RenderContext ctx) {
        boolean enabled = updateRequirements();
        slider.setDisabled(!enabled);
        if (step.doubleValue() > 1e-6) {
            double range = max.doubleValue() - min.doubleValue();
            if (step.doubleValue() / range > 0.08) {
                slider.style().steps(true);
            } else {
                slider.style().steps(false);
            }
        }
    }

    private String formatValue(Number value) {
        if (valueFormater != null) {
            //TODO:在这里设置slider的value formatter似乎不合适
            slider.setValueIndicatorTextFormater(valueFormater);
            return valueFormater.apply(value);
        }

        return String.format("%.2f", value.doubleValue());
    }

    public NumberSliderOptionEntry setValueFormatter(Function<Number, String> formatter) {
        this.valueFormater = formatter;
        if (slider != null) {
            slider.setValueIndicatorTextFormater(formatter);
        }
        return this;
    }

    public NumberSliderOptionEntry setCurrentValue(Number value) {
        if (slider == null) {
            this.value = value;
            return this;
        }
        suppressSliderChangeEvent = true;
        try {
            slider.setValue(value);
            this.value = value;
        } finally {
            suppressSliderChangeEvent = false;
        }
        return this;
    }
}
