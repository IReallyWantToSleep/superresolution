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
import io.homo.superresolution.core.gui.MaterialScheme;
import io.homo.superresolution.core.gui.core.ContainerWidget;
import io.homo.superresolution.core.gui.core.UIInputState;
import io.homo.superresolution.core.gui.core.backends.render.RenderContext;
import io.homo.superresolution.core.gui.widgets.label.MaterialLabel;
import io.homo.superresolution.core.gui.widgets.sliders.MaterialSlider;
import io.homo.superresolution.core.gui.widgets.sliders.MaterialSliderSize;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.*;

import java.util.function.Function;

public class NumberSliderOptionEntry extends AbstractOptionEntry<Number, NumberSliderOptionEntry> {
    private static final float SLIDER_WIDTH = 200f;
    protected MaterialSlider slider;
    protected MaterialLabel valueLabel;
    protected ContainerWidget sliderContainer;
    protected Number max;
    protected Number min;
    protected Number step;
    protected Function<Number, String> valueFormater;

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
        if (step != null && step.doubleValue() != 0) {
            slider.style().steps(true);
            slider.setStep(step);
        }
        slider.setMin(min);
        slider.setMax(max);
        slider.setValue(value);
        slider.onChange(event -> {
            this.value = (Number) event.getNewValue();
            if (saveConsumer != null) {
                saveConsumer.accept(this.value);
            }
            if (saveRunnable != null) {
                saveRunnable.run();
            }
        });

        sliderContainer.addChild(valueLabel);
        sliderContainer.addChild(slider);

        container.addControl(sliderContainer);
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

    @Override
    public void render(RenderContext ctx, UIInputState inputState) {
        container.render(ctx, inputState);

    }

    @Override
    public Number value() {
        return slider != null ? slider.value() : value;
    }
}
